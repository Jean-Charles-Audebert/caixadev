class ApiException extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message);
  }
}

/**
 * Renouvellement de session partagé : une seule requête /api/auth/refresh
 * est en vol à la fois. Les appelants concurrents attendent la même promesse
 * puis reprennent leur requête, au lieu d'échouer immédiatement.
 */
let refreshPromise: Promise<boolean> | null = null;

function refreshSession(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = fetch('/api/auth/refresh', {
    method: 'POST',
    credentials: 'include',
  })
    .then((res) => res.ok)
    .catch(() => false)
    .finally(() => {
      // La promesse est réinitialisée après règlement : un échec ultérieur
      // pourra déclencher une nouvelle tentative, sans boucle immédiate.
      refreshPromise = null;
    });

  return refreshPromise;
}

function isAuthEndpoint(path: string): boolean {
  // Comparaison sur le chemin seul : une query string ne doit pas changer la
  // décision (l'ancienne version `includes('/auth/')` pouvait classifier à tort
  // une URL quelconque contenant ce fragment).
  const cleanPath = path.split('?')[0];
  return cleanPath === '/api/auth' || cleanPath.startsWith('/api/auth/');
}

function doFetch(path: string, options: RequestInit): Promise<Response> {
  return fetch(path, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
  });
}

export async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  let res = await doFetch(path, options);

  // 401 = non authentifié : une seule tentative de renouvellement, puis
  // rejeu de la requête. 403 = authentifié sans droit : aucun renouvellement.
  // Les endpoints d'authentification sont exclus pour éviter toute boucle.
  if (res.status === 401 && !isAuthEndpoint(path)) {
    const refreshed = await refreshSession();
    if (!refreshed) {
      throw new ApiException(401, 'Session expirée');
    }
    res = await doFetch(path, options);
  }

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiException(res.status, body?.message ?? 'Erreur serveur');
  }

  if (res.status === 204) return null as T;
  return res.json();
}
