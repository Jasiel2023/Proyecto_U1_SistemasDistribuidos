import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api/client'
import { getVotosRegistrados } from '../features/votacion/votoStorage'

export function DashboardPage() {
  const { usuario, user } = useAuth()
  const [elecciones, setElecciones] = useState([])
  const [eleccionesState, setEleccionesState] = useState('loading')
  const [votosRegistrados, setVotosRegistrados] = useState([])

  useEffect(() => {
    setVotosRegistrados(getVotosRegistrados(user?.userId))
  }, [user?.userId])

  useEffect(() => {
    let mounted = true

    api.get('/elecciones')
      .then(({ data }) => {
        if (!mounted) return
        setElecciones(data)
        setEleccionesState('ready')
      })
      .catch(() => {
        if (mounted) setEleccionesState('error')
      })

    return () => {
      mounted = false
    }
  }, [])

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">Panel de Votante</h1>
          <p className="dashboard-subtitle">Bienvenido, {usuario ? `${usuario.nombre} ${usuario.apellido}` : user?.nickname || 'Usuario'}</p>
        </div>
        <Link className="btn btn-outline" to="/resultados">Ver resultados</Link>
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

        <section className="card votaciones-card elections-card" aria-labelledby="votaciones-title">
          <h2 id="votaciones-title" className="card-title">Votaciones Disponibles</h2>
          {eleccionesState === 'loading' && <p className="empty-state">Cargando elecciones...</p>}
          {eleccionesState === 'error' && <p className="error-state">No fue posible cargar las elecciones.</p>}
          {eleccionesState === 'ready' && elecciones.length === 0 && (
            <div className="empty-state">
              <h3>No hay votaciones disponibles</h3>
              <p>Cuando una elección sea habilitada, aparecerá en esta sección.</p>
            </div>
          )}
          {eleccionesState === 'ready' && elecciones.length > 0 && (
            <div className="available-elections">
              {elecciones.map((eleccion) => (
                <ElectionCard
                  eleccion={eleccion}
                  key={eleccion.id}
                  yaVoto={votosRegistrados.includes(String(eleccion.id))}
                />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

function ElectionCard({ eleccion, yaVoto }) {
  return (
    <article className="available-election">
      <div>
        <span className="election-status">Activa</span>
        <h3>{eleccion.titulo}</h3>
        <p>{formatDate(eleccion.fecha)} · {eleccion.horaInicio?.slice(0, 5)} - {eleccion.horaFin?.slice(0, 5)}</p>
        <small>{eleccion.candidatos?.length ?? 0} candidatos disponibles</small>
      </div>
      <div className="election-actions">
        {yaVoto ? (
          <button className="btn btn-secondary btn-sm" disabled type="button">Ya votaste</button>
        ) : (
          <Link className="btn btn-primary btn-sm" to={`/votacion?eleccionId=${encodeURIComponent(eleccion.id)}`}>
            Emitir voto
          </Link>
        )}
        <Link className="btn btn-outline btn-sm" to={`/resultados?eleccion=${encodeURIComponent(eleccion.id)}`}>
          Resultados
        </Link>
      </div>
    </article>
  )
}

function formatDate(dateString) {
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
