import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function LoginPage() {
  const { isAuthenticated, isAdmin, isVotante, login, loading: authLoading } = useAuth()
  const navigate = useNavigate()

  const [nickname, setNickname] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [formErrors, setFormErrors] = useState({ nickname: '', clave: '' })

  if (isAuthenticated) {
    navigate(isAdmin ? '/admin/dashboard' : '/dashboard', { replace: true })
    return null
  }

  const validateForm = () => {
    const errors = { nickname: '', clave: '' }
    let isValid = true

    if (!nickname.trim()) {
      errors.nickname = 'El nickname es requerido'
      isValid = false
    }

    if (!clave) {
      errors.clave = 'La clave es requerida'
      isValid = false
    }

    setFormErrors(errors)
    return isValid
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!validateForm()) return

    setLoading(true)
    const result = await login(nickname, clave)

    if (!result.success) {
      setError(result.error)
    }
    setLoading(false)
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <svg className="login-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
          <h1>Iniciar Sesión</h1>
          <p className="login-subtitle">Sistema de Votación en Tiempo Real</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          {error && <div className="error-message global-error" role="alert">{error}</div>}

          <div className="form-group">
            <label htmlFor="nickname">Nickname</label>
            <input
              type="text"
              id="nickname"
              name="nickname"
              value={nickname}
              onChange={(e) => {
                setNickname(e.target.value)
                if (formErrors.nickname) setFormErrors(prev => ({ ...prev, nickname: '' }))
              }}
              placeholder="Ingrese su nickname"
              disabled={loading || authLoading}
              autoComplete="username"
              aria-invalid={!!formErrors.nickname}
              aria-describedby={formErrors.nickname ? 'nickname-error' : undefined}
            />
            {formErrors.nickname && (
              <span id="nickname-error" className="error-message field-error" role="alert">{formErrors.nickname}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="clave">Clave</label>
            <input
              type="password"
              id="clave"
              name="clave"
              value={clave}
              onChange={(e) => {
                setClave(e.target.value)
                if (formErrors.clave) setFormErrors(prev => ({ ...prev, clave: '' }))
              }}
              placeholder="Ingrese su clave"
              disabled={loading || authLoading}
              autoComplete="current-password"
              aria-invalid={!!formErrors.clave}
              aria-describedby={formErrors.clave ? 'clave-error' : undefined}
            />
            {formErrors.clave && (
              <span id="clave-error" className="error-message field-error" role="alert">{formErrors.clave}</span>
            )}
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-login"
            disabled={loading || authLoading}
          >
            {loading || authLoading ? (
              <>
                <span className="btn-spinner" aria-hidden="true"></span>
                Iniciando sesión...
              </>
            ) : (
              'Iniciar Sesión'
            )}
          </button>
        </form>

        <div className="login-footer">
          <p>¿No tienes cuenta? Contacta al administrador</p>
        </div>
      </div>
    </div>
  )
}