import { Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function Layout() {
  const { usuario, logout, isAuthenticated, rol } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  const userName = usuario ? `${usuario.nombre} ${usuario.apellido}` : usuario?.nickname || 'Usuario'

  const handleLogout = () => {
    logout()
  }

  return (
    <div className="layout">
      <header className="layout-header">
        <div className="header-brand">
          <svg className="logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
          <span className="brand-text">Sistema de Votación</span>
        </div>

        <div className="header-user">
          <div className="user-info">
            <span className={`user-badge ${(rol || 'VOTANTE').toLowerCase()}`}>{rol || 'Votante'}</span>
            <span className="user-name">{userName}</span>
          </div>
          <button className="btn btn-logout" onClick={handleLogout} type="button">
            <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span>Cerrar sesión</span>
          </button>
        </div>
      </header>

      <main className="layout-main" role="main">
        <Outlet />
      </main>
    </div>
  )
}