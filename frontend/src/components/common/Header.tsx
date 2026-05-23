import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function Header() {
  const { user, logout } = useAuth();

  return (
    <nav style={{
      position: 'fixed', top: 0, width: '100%', zIndex: 100,
      background: 'rgba(255,255,255,0.95)', borderBottom: '1px solid var(--color-border)',
      backdropFilter: 'blur(4px)',
    }}>
      <div className="container" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem' }}>
        <Link to="/" style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--color-text)' }}>
          Caixadev
        </Link>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          {user ? (
            <>
              <Link to="/dashboard" className="btn outline" style={{ padding: '0.3rem 0.8rem', fontSize: '0.9rem' }}>
                Tableau de bord
              </Link>
              <button onClick={logout} className="btn secondary" style={{ padding: '0.3rem 0.8rem', fontSize: '0.9rem' }}>
                Déconnexion
              </button>
            </>
          ) : (
            <Link to="/login" className="btn" style={{ padding: '0.3rem 0.8rem', fontSize: '0.9rem' }}>
              Connexion
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}

