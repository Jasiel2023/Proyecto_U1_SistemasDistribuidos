import { useState } from 'react'
import { api } from '../../api/client'
import './VotacionPage.css'

const initialForm = {
  votacionId: '1',
  opcionId: '',
  usuarioId: '',
}

function VotacionPage() {
  const [form, setForm] = useState(initialForm)
  const [requestState, setRequestState] = useState({
    status: 'idle',
    message: '',
  })

  const isSubmitting = requestState.status === 'submitting'

  function handleChange(event) {
    const { name, value } = event.target
    setForm((currentForm) => ({ ...currentForm, [name]: value }))
    if (requestState.status !== 'idle') {
      setRequestState({ status: 'idle', message: '' })
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (isSubmitting) return

    setRequestState({ status: 'submitting', message: '' })

    try {
      const response = await api.post('/votos', {
        votacionId: Number(form.votacionId),
        opcionId: Number(form.opcionId),
        usuarioId: Number(form.usuarioId),
      })

      setRequestState({
        status: 'success',
        message: response.data.mensaje || 'Voto registrado correctamente.',
      })
    } catch (error) {
      const responseData = error.response?.data
      const status = error.response?.status
      const message = getErrorMessage(status, responseData)

      setRequestState({ status: 'error', message })
    }
  }

  return (
    <main className="votacion-page">
      <section className="votacion-shell" aria-labelledby="votacion-title">
        <div className="votacion-intro">
          <p className="eyebrow">Votaciones en tiempo real</p>
          <h1 id="votacion-title">Emite tu voto</h1>
          <p className="intro-copy">
            Selecciona la votación y registra tu opción de forma segura. El
            sistema valida el voto único en el backend.
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
              <p className="form-kicker">Datos del sufragio</p>
              <h2>Completa los identificadores</h2>
            </div>
          </div>

          <div className="field-grid">
            <label className="field">
              <span>Votación</span>
              <small>ID de la votación abierta</small>
              <input
                min="1"
                name="votacionId"
                onChange={handleChange}
                required
                type="number"
                value={form.votacionId}
              />
            </label>

            <label className="field">
              <span>Opción</span>
              <small>ID de tu opción o candidato</small>
              <input
                min="1"
                name="opcionId"
                onChange={handleChange}
                required
                type="number"
                value={form.opcionId}
              />
            </label>

            <label className="field field-wide">
              <span>Usuario</span>
              <small>Identificador del votante autenticado</small>
              <input
                min="1"
                name="usuarioId"
                onChange={handleChange}
                required
                type="number"
                value={form.usuarioId}
              />
            </label>
          </div>

          <button className="submit-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Registrando voto...' : 'Confirmar voto'}
            <span aria-hidden="true">-&gt;</span>
          </button>

          <div
            aria-live="polite"
            className={`form-feedback ${requestState.status}`}
            role={requestState.status === 'error' ? 'alert' : 'status'}
          >
            {requestState.message}
          </div>
        </form>
      </section>
    </main>
  )
}

function getErrorMessage(status, responseData) {
  if (status === 400) {
    return responseData?.message || 'Revisa los datos ingresados.'
  }

  if (status === 404) {
    return responseData?.message || 'La votación o la opción no existe.'
  }

  if (status === 409) {
    return responseData?.message || 'No puedes emitir este voto.'
  }

  return responseData?.message || 'No fue posible registrar el voto. Intenta nuevamente.'
}

export default VotacionPage
