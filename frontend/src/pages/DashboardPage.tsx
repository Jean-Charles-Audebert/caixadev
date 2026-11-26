import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyProjects } from '../services/projectService';
import type { ProjectInfo } from '../types';

export default function DashboardPage() {
  const { user } = useAuth();
  const [projects, setProjects] = useState<ProjectInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyProjects()
      .then(setProjects)
      .catch(() => setError('Impossible de charger vos projets.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ paddingTop: '4rem' }}>
      <div className="container">
        <h2>Tableau de bord</h2>
        <p style={{ color: 'var(--color-text-muted)', marginTop: '0.3rem' }}>
          Bienvenue{user?.displayName ? `, ${user.displayName}` : ''} !
        </p>

        {user?.superAdmin && (
          <section style={{ marginTop: '1.5rem' }}>
            <h3>Administration</h3>
            <Link to="/admin/api-keys" className="btn outline">
              Clés API
            </Link>
          </section>
        )}

        <section style={{ marginTop: '2rem' }}>
          {loading && <p>Chargement des projets…</p>}
          {error && <p className="error-message">{error}</p>}

          {!loading && !error && projects.length === 0 && (
            <div style={{
              background: 'var(--color-bg-alt)',
              border: '1px solid var(--color-border)',
              borderRadius: '8px',
              padding: '2rem',
              textAlign: 'center',
            }}>
              <p>Vous n'avez accès à aucun projet pour l'instant.</p>
              <p style={{ color: 'var(--color-text-muted)', marginTop: '0.5rem' }}>
                Contactez l'administrateur pour demander un accès :{' '}
                <a href="mailto:contact@caixadev.dev">contact@caixadev.dev</a>
              </p>
            </div>
          )}

          {!loading && projects.map((project) => (
            <article key={project.id} className="project-card">
              <h3>{project.name}</h3>
              {project.description && (
                <p style={{ color: 'var(--color-text-muted)', margin: '0.3rem 0' }}>{project.description}</p>
              )}
              <div className="roles">
                {project.roles.map((role) => (
                  <span key={role} className="role-badge">{role}</span>
                ))}
              </div>
            </article>
          ))}
        </section>
      </div>
    </div>
  );
}

