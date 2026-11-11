import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function NotFoundPage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="not-found-page">
      <div className="not-found-card">
        <h1 className="not-found-code">404</h1>
        <h2 className="not-found-title">Página no encontrada</h2>
        <p className="not-found-message">La página que buscas no existe o ha sido movida.</p>
        <div className="not-found-actions">
          <Link to={isAuthenticated ? '/dashboard' : '/login'} className="btn btn-primary">
            <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M19 12H5" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            <span>{isAuthenticated ? 'Volver al Panel' : 'Ir al Login'}</span>
          </Link>
        </div>
      </div>
    </div>
  )
}