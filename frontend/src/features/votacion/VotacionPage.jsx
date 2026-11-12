import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import { useAuth } from '../../hooks/useAuth'
import { registrarVotoLocal } from './votoStorage'
import './VotacionPage.css'

function VotacionPage() {
  const { user } = useAuth()
  const [elecciones, setElecciones] = useState([])
  const [eleccionId, setEleccionId] = useState('')
  const [eleccionBloqueada, setEleccionBloqueada] = useState(false)
  const [candidatoId, setCandidatoId] = useState('')
  const [loadState, setLoadState] = useState('loading')
  const [requestState, setRequestState] = useState({ status: 'idle', message: '' })
  const [votoRegistrado, setVotoRegistrado] = useState(false)

  useEffect(() => {
    let mounted = true

    api.get('/elecciones')
      .then((response) => {
        if (!mounted) return
        const available = response.data
        const requestedId = new URLSearchParams(window.location.search).get('eleccionId')
        const requestedElection = available.find((eleccion) => String(eleccion.id) === requestedId)
        setElecciones(available)
        setEleccionId(requestedElection ? String(requestedElection.id) : available[0] ? String(available[0].id) : '')
        setEleccionBloqueada(Boolean(requestedElection))
        setLoadState('ready')
      })
      .catch(() => {
        if (mounted) setLoadState('error')
      })

    return () => {
      mounted = false
    }
  }, [])

  const eleccionSeleccionada = elecciones.find(
    (eleccion) => String(eleccion.id) === eleccionId,
  )

  function seleccionarEleccion(event) {
    setEleccionId(event.target.value)
    setCandidatoId('')
    setVotoRegistrado(false)
    setRequestState({ status: 'idle', message: '' })
  }

  function seleccionarCandidato(event) {
    setCandidatoId(event.target.value)
    if (requestState.status !== 'idle') {
      setRequestState({ status: 'idle', message: '' })
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (requestState.status === 'submitting' || !eleccionSeleccionada) return

    setRequestState({ status: 'submitting', message: '' })

    try {
      const response = await api.post('/votos', {
        eleccionId,
        candidatoId,
      })

      setRequestState({
        status: 'success',
        message: response.data.mensaje || 'Voto registrado correctamente.',
      })
      registrarVotoLocal(user?.userId, eleccionId)
      setVotoRegistrado(true)
    } catch (error) {
      const responseData = error.response?.data
      if (error.response?.status === 409) {
        registrarVotoLocal(user?.userId, eleccionId)
        setVotoRegistrado(true)
      }
      setRequestState({
        status: 'error',
        message: getErrorMessage(error.response?.status, responseData),
      })
    }
  }

  const isSubmitting = requestState.status === 'submitting'
  const isReady = loadState === 'ready' && elecciones.length > 0

  return (
    <main className="votacion-page">
      <section className="votacion-shell" aria-labelledby="votacion-title">
           <div className="votacion-intro">
          <Link className="back-dashboard" to="/dashboard">Volver al dashboard</Link>
          <p className="eyebrow">Elecciones en tiempo real</p>
          <h1 id="votacion-title">Emite tu voto</h1>
          <p className="intro-copy">
            Consulta la elección activa, conoce a sus candidatos y registra tu
            decisión de forma segura.
          </p>
          <div className="architecture-note">
            <span className="status-dot" aria-hidden="true" />
            <span>Solicitud REST a través del API Gateway</span>
          </div>
        </div>

        <form className="voto-form" onSubmit={handleSubmit}>
          <div className="form-heading">
            <span className="step-number">01</span>
            <div>
              <p className="form-kicker">Boleta electoral</p>
              <h2>Selecciona tu elección</h2>
            </div>
          </div>

          {loadState === 'loading' && <p className="empty-state">Cargando elecciones...</p>}
          {loadState === 'error' && (
            <p className="empty-state error-text">No fue posible cargar las elecciones.</p>
          )}
          {loadState === 'ready' && elecciones.length === 0 && (
            <p className="empty-state">No hay elecciones activas disponibles.</p>
          )}

          {isReady && (
            <>
              <label className="field">
                <span>Elección</span>
                 <select disabled={eleccionBloqueada} value={eleccionId} onChange={seleccionarEleccion}>
                  {elecciones.map((eleccion) => (
                    <option key={eleccion.id} value={eleccion.id}>
                      {eleccion.titulo}
                    </option>
                  ))}
                </select>
              </label>

              {eleccionSeleccionada && (
                <div className="election-details">
                  <div>
                    <span>Fecha</span>
                    <strong>{formatDate(eleccionSeleccionada.fecha)}</strong>
                  </div>
                  <div>
                    <span>Horario para votar</span>
                    <strong>
                      {formatTime(eleccionSeleccionada.horaInicio)} - {formatTime(eleccionSeleccionada.horaFin)}
                    </strong>
                  </div>
                </div>
              )}

              <fieldset className="candidate-list">
                <legend>Candidatos</legend>
                {eleccionSeleccionada?.candidatos.map((candidato) => (
                  <label
                    className={`candidate-card ${String(candidato.id) === candidatoId ? 'selected' : ''}`}
                    key={candidato.id}
                  >
                    <input
                      checked={String(candidato.id) === candidatoId}
                      name="candidatoId"
                      onChange={seleccionarCandidato}
                      type="radio"
                      value={candidato.id}
                    />
                    <span className="candidate-mark" aria-hidden="true" />
                    <span className="candidate-info">
                      <strong>{candidato.nombre} {candidato.apellido}</strong>
                      <small>{candidato.partidoPolitico}</small>
                    </span>
                  </label>
                ))}
              </fieldset>

              <button
                className="submit-button"
                disabled={isSubmitting || votoRegistrado || !candidatoId}
                type="submit"
              >
                {isSubmitting ? 'Registrando voto...' : votoRegistrado ? 'Voto registrado' : 'Confirmar voto'}
                <span aria-hidden="true">-&gt;</span>
              </button>

              <div
                aria-live="polite"
                className={`form-feedback ${requestState.status}`}
                role={requestState.status === 'error' ? 'alert' : 'status'}
              >
                {requestState.message}
              </div>
            </>
          )}
        </form>
      </section>
    </main>
  )
}

function formatDate(value) {
  if (!value) return 'Sin fecha'
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

function formatTime(value) {
  return value ? value.slice(0, 5) : '--:--'
}

function getErrorMessage(status, responseData) {
  if (status === 400) return responseData?.message || 'Revisa los datos ingresados.'
  if (status === 404) return responseData?.message || 'La elección o el candidato no existe.'
  if (status === 409) return 'Ya votaste, no es posible volver a votar en esta eleccion.'
  return responseData?.message || 'No fue posible registrar el voto. Intenta nuevamente.'
}

export default VotacionPage
