import { Link } from 'react-router-dom';

export default function UnauthorizedPage() {
  return (
    <div style={{ paddingTop: '4rem' }}>
      <div className="container" style={{ textAlign: 'center', paddingTop: '4rem' }}>
        <h2>Accès refusé</h2>
        <p style={{ color: 'var(--color-text-muted)', marginTop: '1rem' }}>
          Vous n'avez pas les droits pour accéder à cette page.
        </p>
        <div style={{ marginTop: '2rem', display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          <Link to="/" className="btn secondary">Retour à l'accueil</Link>
          <a href="mailto:contact@caixadev.dev" className="btn outline">Contacter l'admin</a>
        </div>
      </div>
    </div>
  );
}

