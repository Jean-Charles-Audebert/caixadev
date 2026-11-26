import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { API_KEY_SCOPES, createApiKey, listApiKeys, revokeApiKey } from '../services/apiKeyService';
import type { ApiKeyInfo, CreatedApiKey } from '../types';

const STATUS_LABELS: Record<ApiKeyInfo['status'], string> = {
  ACTIVE: 'Active',
  EXPIRED: 'Expirée',
  REVOKED: 'Révoquée',
};

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleString('fr-FR') : '—';
}

function errorMessage(err: unknown, fallback: string): string {
  return err instanceof Error && err.message ? err.message : fallback;
}

const cellStyle: React.CSSProperties = {
  padding: '0.5rem',
  borderBottom: '1px solid var(--color-border)',
  textAlign: 'left',
  verticalAlign: 'top',
};

/** Administration des clés API machine (superadministrateur). */
export default function ApiKeysPage() {
  const { user } = useAuth();
  const [keys, setKeys] = useState<ApiKeyInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [name, setName] = useState('');
  const [scopes, setScopes] = useState<string[]>([API_KEY_SCOPES[0]]);
  const [expiresAt, setExpiresAt] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const [created, setCreated] = useState<CreatedApiKey | null>(null);
  const [copied, setCopied] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    listApiKeys()
      .then((list) => {
        setKeys(list);
        setError('');
      })
      .catch((err) => setError(errorMessage(err, 'Impossible de charger les clés.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (user?.superAdmin) refresh();
  }, [user, refresh]);

  if (user && !user.superAdmin) return <Navigate to="/unauthorized" replace />;

  function toggleScope(scope: string) {
    setScopes((current) =>
      current.includes(scope) ? current.filter((s) => s !== scope) : [...current, scope]
    );
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError('');
    if (!name.trim()) {
      setFormError('Le nom est obligatoire.');
      return;
    }
    if (scopes.length === 0) {
      setFormError('Choisissez au moins une portée.');
      return;
    }
    setSubmitting(true);
    try {
      const result = await createApiKey({
        name: name.trim(),
        scopes,
        // datetime-local est en heure locale : conversion explicite en UTC.
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      });
      setCreated(result);
      setCopied(false);
      setName('');
      setExpiresAt('');
      refresh();
    } catch (err) {
      setFormError(errorMessage(err, 'Création impossible.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCopy() {
    if (!created) return;
    try {
      await navigator.clipboard.writeText(created.key);
      setCopied(true);
    } catch {
      setCopied(false);
      setFormError('Copie impossible : sélectionnez la clé et copiez-la manuellement.');
    }
  }

  async function handleRevoke(key: ApiKeyInfo) {
    if (!window.confirm(`Révoquer la clé « ${key.name} » (${key.keyPrefix}) ? Cette action est définitive.`)) {
      return;
    }
    try {
      await revokeApiKey(key.id);
      refresh();
    } catch (err) {
      setError(errorMessage(err, 'Révocation impossible.'));
    }
  }

  return (
    <div style={{ paddingTop: '4rem' }}>
      <div className="container">
        <p style={{ marginBottom: '0.5rem' }}>
          <Link to="/dashboard">← Tableau de bord</Link>
        </p>
        <h2>Clés API</h2>
        <p style={{ color: 'var(--color-text-muted)', marginTop: '0.3rem' }}>
          Accès machine à <code>api.caixadev.dev</code> (en-tête <code>Authorization: Bearer cxd_…</code>).
        </p>

        {created && (
          <section
            role="alert"
            style={{
              marginTop: '1.5rem',
              padding: '1rem',
              border: '2px solid var(--color-error)',
              borderRadius: '8px',
              background: 'var(--color-bg-alt)',
            }}
          >
            <h3 style={{ marginTop: 0 }}>Clé « {created.apiKey.name} » créée</h3>
            <p>
              <strong>Copiez cette clé maintenant : elle ne sera plus jamais affichée.</strong> Seule son
              empreinte est conservée ; en cas de perte, révoquez-la et créez-en une nouvelle.
            </p>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <input
                readOnly
                value={created.key}
                aria-label="Clé API"
                onFocus={(e) => e.currentTarget.select()}
                style={{
                  flex: '1 1 24rem',
                  fontFamily: 'monospace',
                  padding: '0.5rem',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius)',
                }}
              />
              <button type="button" className="btn" onClick={handleCopy}>
                {copied ? 'Copiée ✓' : 'Copier'}
              </button>
              <button type="button" className="btn secondary" onClick={() => setCreated(null)}>
                J'ai copié la clé
              </button>
            </div>
          </section>
        )}

        <section style={{ marginTop: '2rem' }}>
          <h3>Nouvelle clé</h3>
          <form onSubmit={handleCreate} noValidate style={{ maxWidth: '32rem' }}>
            <div className="form-group">
              <label htmlFor="api-key-name">Nom</label>
              <input
                id="api-key-name"
                value={name}
                maxLength={100}
                onChange={(e) => setName(e.target.value)}
                placeholder="ex. Claude – poste bureau"
                required
              />
            </div>
            <fieldset className="form-group" style={{ border: 'none', padding: 0 }}>
              <legend style={{ fontWeight: 600, fontSize: '0.9rem' }}>Portées</legend>
              {API_KEY_SCOPES.map((scope) => (
                <label key={scope} style={{ fontWeight: 400 }}>
                  <input
                    type="checkbox"
                    checked={scopes.includes(scope)}
                    onChange={() => toggleScope(scope)}
                  />{' '}
                  {scope}
                </label>
              ))}
            </fieldset>
            <div className="form-group">
              <label htmlFor="api-key-expires">Expiration (facultative)</label>
              <input
                id="api-key-expires"
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
              />
            </div>
            {formError && <p className="error-message">{formError}</p>}
            <button type="submit" className="btn" disabled={submitting}>
              {submitting ? 'Création…' : 'Créer la clé'}
            </button>
          </form>
        </section>

        <section style={{ marginTop: '2rem' }}>
          <h3>Clés existantes</h3>
          {loading && <p>Chargement…</p>}
          {error && <p className="error-message">{error}</p>}
          {!loading && !error && keys.length === 0 && <p>Aucune clé pour l'instant.</p>}
          {!loading && keys.length > 0 && (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
                <thead>
                  <tr>
                    <th style={cellStyle}>Nom</th>
                    <th style={cellStyle}>Préfixe</th>
                    <th style={cellStyle}>Portées</th>
                    <th style={cellStyle}>Statut</th>
                    <th style={cellStyle}>Créée</th>
                    <th style={cellStyle}>Expire</th>
                    <th style={cellStyle}>Dernier usage</th>
                    <th style={cellStyle} />
                  </tr>
                </thead>
                <tbody>
                  {keys.map((key) => (
                    <tr key={key.id} style={{ opacity: key.status === 'ACTIVE' ? 1 : 0.6 }}>
                      <td style={cellStyle}>{key.name}</td>
                      <td style={cellStyle}><code>cxd_{key.keyPrefix}_…</code></td>
                      <td style={cellStyle}>
                        {key.scopes.map((scope) => (
                          <span key={scope} className="role-badge">{scope}</span>
                        ))}
                      </td>
                      <td style={cellStyle}>{STATUS_LABELS[key.status]}</td>
                      <td style={cellStyle}>
                        {formatDate(key.createdAt)}
                        <br />
                        <span style={{ color: 'var(--color-text-muted)' }}>{key.createdBy}</span>
                      </td>
                      <td style={cellStyle}>{formatDate(key.expiresAt)}</td>
                      <td style={cellStyle}>{formatDate(key.lastUsedAt)}</td>
                      <td style={cellStyle}>
                        {key.status !== 'REVOKED' && (
                          <button
                            type="button"
                            className="btn outline"
                            style={{ padding: '0.2rem 0.6rem', fontSize: '0.85rem' }}
                            onClick={() => handleRevoke(key)}
                          >
                            Révoquer
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
