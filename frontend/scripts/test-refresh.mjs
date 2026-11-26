// Test de non-régression du renouvellement de session côté frontend.
//
// Le projet n'a pas de runner de tests installé (ni vitest, ni jest) et aucune
// dépendance ne doit être ajoutée hors ligne. Ce script compile donc le seul
// module concerné (src/services/api.ts, sans dépendance) avec le `tsc` déjà
// présent dans node_modules, puis l'exécute avec un `fetch` simulé.
//
// Couverture :
//   1. requêtes concurrentes en 401 -> un seul POST /api/auth/refresh, rejeu de chacune ;
//   2. 403 (authentifié sans droit) -> aucun renouvellement ;
//   3. un seul rejeu : un 401 après renouvellement lève l'erreur, sans boucle ;
//   4. endpoints d'authentification exclus (y compris avec query string) ;
//   5. un chemin qui *contient* « auth » sans être /api/auth n'est pas exclu ;
//   6. renouvellement refusé -> ApiException 401 « Session expirée », pas de rejeu.

import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const outDir = fs.mkdtempSync(path.join(os.tmpdir(), 'caixadev-refresh-'));

function compile() {
  const tsc = path.join(projectRoot, 'node_modules', 'typescript', 'bin', 'tsc');
  if (!fs.existsSync(tsc)) {
    throw new Error('TypeScript est absent de node_modules : exécuter `npm ci` hors ligne est requis.');
  }
  execFileSync(
    process.execPath,
    [
      tsc,
      path.join(projectRoot, 'src', 'services', 'api.ts'),
      '--outDir', outDir,
      '--target', 'ES2020',
      '--module', 'ES2020',
      '--moduleResolution', 'node',
      '--lib', 'ES2020,DOM',
      '--skipLibCheck',
    ],
    { stdio: 'inherit' }
  );
  fs.writeFileSync(path.join(outDir, 'package.json'), '{"type":"module"}');
}

function jsonResponse(status, body) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

let calls = [];
let handler = () => jsonResponse(500, { message: 'aucun handler' });

globalThis.fetch = async (url, options = {}) => {
  calls.push({ url, options });
  return handler(url, options, calls);
};

const callsTo = (url) => calls.filter((call) => call.url === url);

async function run(name, fn) {
  calls = [];
  handler = () => jsonResponse(500, { message: 'aucun handler' });
  try {
    await fn();
    console.log(`  ok  ${name}`);
  } catch (error) {
    process.exitCode = 1;
    console.error(`  FAIL ${name}`);
    console.error(error);
  }
}

async function main() {
  compile();
  const { request } = await import(pathToFileURL(path.join(outDir, 'api.js')).href);

  await run('concurrence : un seul refresh pour plusieurs 401 simultanés', async () => {
    let refreshCalls = 0;
    let projectCalls = 0;
    let releaseRefresh;
    const refreshGate = new Promise((resolve) => { releaseRefresh = resolve; });

    handler = async (url) => {
      if (url === '/api/auth/refresh') {
        refreshCalls += 1;
        await refreshGate;
        return jsonResponse(200, {});
      }
      if (url === '/api/projects') {
        projectCalls += 1;
        return projectCalls <= 2
          ? jsonResponse(401, { message: 'expiré' })
          : jsonResponse(200, { projects: [] });
      }
      return jsonResponse(500, { message: 'inattendu' });
    };

    const first = request('/api/projects');
    const second = request('/api/projects');

    // Laisse les deux requêtes atteindre leur 401 avant de libérer le refresh.
    await new Promise((resolve) => setTimeout(resolve, 0));
    assert.equal(refreshCalls, 1, 'un seul refresh doit être en vol');
    releaseRefresh();

    const [a, b] = await Promise.all([first, second]);
    assert.deepEqual(a, { projects: [] });
    assert.deepEqual(b, { projects: [] });
    assert.equal(refreshCalls, 1, 'aucun refresh supplémentaire');
    assert.equal(projectCalls, 4, 'chaque appelant initial + un rejeu');
  });

  await run('403 : aucun renouvellement, erreur propagée', async () => {
    let refreshCalls = 0;
    handler = (url) => {
      if (url === '/api/auth/refresh') refreshCalls += 1;
      if (url === '/api/admin/users') return jsonResponse(403, { message: 'Accès refusé' });
      return jsonResponse(500, { message: 'inattendu' });
    };

    await assert.rejects(
      request('/api/admin/users'),
      (error) => error.status === 403 && error.message === 'Accès refusé'
    );
    assert.equal(refreshCalls, 0);
    assert.equal(callsTo('/api/admin/users').length, 1);
  });

  await run('un seul rejeu : 401 après refresh -> erreur, pas de boucle', async () => {
    let refreshCalls = 0;
    let pathCalls = 0;
    handler = (url) => {
      if (url === '/api/auth/refresh') {
        refreshCalls += 1;
        return jsonResponse(200, {});
      }
      pathCalls += 1;
      return jsonResponse(401, { message: 'toujours expiré' });
    };

    await assert.rejects(request('/api/users/me'), (error) => error.status === 401);
    assert.equal(refreshCalls, 1, 'un seul refresh');
    assert.equal(pathCalls, 2, 'appel initial + un seul rejeu');
  });

  await run('endpoint /api/auth exclu, même avec query string', async () => {
    let refreshCalls = 0;
    handler = (url) => {
      if (url === '/api/auth/refresh') refreshCalls += 1;
      return jsonResponse(401, { message: 'identifiants invalides' });
    };

    await assert.rejects(
      request('/api/auth/login?redirect=/dashboard'),
      (error) => error.status === 401 && error.message === 'identifiants invalides'
    );
    assert.equal(refreshCalls, 0);
  });

  await run('chemin contenant « auth » sans être /api/auth : refresh autorisé', async () => {
    let refreshCalls = 0;
    let pathCalls = 0;
    handler = (url) => {
      if (url === '/api/auth/refresh') {
        refreshCalls += 1;
        return jsonResponse(200, {});
      }
      pathCalls += 1;
      return pathCalls === 1
        ? jsonResponse(401, { message: 'expiré' })
        : jsonResponse(200, { value: 42 });
    };

    assert.deepEqual(await request('/api/notauth/thing'), { value: 42 });
    assert.equal(refreshCalls, 1, 'l’ancien `includes(\'/auth/\')` aurait exclu ce chemin à tort');
    assert.equal(pathCalls, 2);
  });

  await run('refresh refusé : ApiException 401 sans rejeu', async () => {
    let refreshCalls = 0;
    let pathCalls = 0;
    handler = (url) => {
      if (url === '/api/auth/refresh') {
        refreshCalls += 1;
        return jsonResponse(401, { message: 'non' });
      }
      pathCalls += 1;
      return jsonResponse(401, { message: 'expiré' });
    };

    await assert.rejects(
      request('/api/projects'),
      (error) => error.status === 401 && error.message === 'Session expirée'
    );
    assert.equal(refreshCalls, 1);
    assert.equal(pathCalls, 1, 'aucun rejeu quand le renouvellement échoue');
  });

  await run('204 : réponse sans corps', async () => {
    handler = () => new Response(null, { status: 204 });
    assert.equal(await request('/api/auth/logout', { method: 'POST' }), null);
  });

  if (process.exitCode) {
    console.error('\nÉchec du test de renouvellement de session.');
  } else {
    console.log('\nTous les scénarios de renouvellement de session sont passés.');
  }
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(() => {
    fs.rmSync(outDir, { recursive: true, force: true });
  });
