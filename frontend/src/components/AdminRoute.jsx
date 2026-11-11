import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function AdminRoute({ children }) {
  const { isAuthenticated, loading, isAdmin } = useAuth()

  if (loading) {
    return (
      <div className="loading-screen" role="status" aria-label="Cargando...">
        <div className="spinner"></div>
        <p>Cargando sesión...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!isAdmin) {
    return <Navigate to="/dashboard" replace />
  }

  return children
}
