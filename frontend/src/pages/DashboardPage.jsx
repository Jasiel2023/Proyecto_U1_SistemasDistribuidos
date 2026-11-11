import { useAuth } from '../hooks/useAuth'

export function DashboardPage() {
  const { usuario, user } = useAuth()

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    try {
      return new Date(dateString).toLocaleString('es-EC', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch {
      return dateString
    }
  }

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">Panel de Votante</h1>
          <p className="dashboard-subtitle">Bienvenido, {usuario ? `${usuario.nombre} ${usuario.apellido}` : user?.nickname || 'Usuario'}</p>
        </div>
      </div>

      <div className="dashboard-grid">
        <section className="card profile-card" aria-labelledby="profile-title">
          <h2 id="profile-title" className="card-title">Mi Perfil</h2>
          <div className="profile-grid">
            {usuario && (
              <>
                <div className="profile-field">
                  <label>Cédula</label>
                  <span>{usuario.cedula}</span>
                </div>
                <div className="profile-field">
                  <label>Correo</label>
                  <span>{usuario.correo}</span>
                </div>
                <div className="profile-field">
                  <label>Nickname</label>
                  <span>{usuario.nickname}</span>
                </div>
                <div className="profile-field">
                  <label>Nombre completo</label>
                  <span>{usuario.nombre} {usuario.apellido}</span>
                </div>
                <div className="profile-field">
                  <label>ID Cuenta</label>
                  <span>{usuario.idCuenta}</span>
                </div>
                <div className="profile-field">
                  <label>ID Usuario</label>
                  <span>{usuario.id}</span>
                </div>
              </>
            )}
            {!usuario && user && (
              <>
                <div className="profile-field">
                  <label>ID Cuenta (userId)</label>
                  <span>{user.userId}</span>
                </div>
                <div className="profile-field">
                  <label>Nickname</label>
                  <span>{user.nickname}</span>
                </div>
                <div className="profile-field">
                  <label>Token expira</label>
                  <span>{formatDate(user.expiraEn)}</span>
                </div>
              </>
            )}
            {!usuario && !user && (
              <div className="profile-field">
                <span>Cargando datos de usuario...</span>
              </div>
            )}
          </div>
        </section>

        <section className="card votaciones-card" aria-labelledby="votaciones-title">
          <h2 id="votaciones-title" className="card-title">Votaciones Disponibles</h2>
          <div className="votaciones-placeholder">
            <svg className="placeholder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
              <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h3>Próximamente</h3>
            <p>Integración con <strong>votacion-service</strong> en desarrollo</p>
            <p className="placeholder-hint">Aquí aparecerán las votaciones activas cuando estén disponibles</p>
          </div>
        </section>
      </div>
    </div>
  )
}