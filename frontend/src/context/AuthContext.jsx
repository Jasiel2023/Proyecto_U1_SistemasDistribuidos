import { createContext, useState, useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { login as apiLogin, validateSession, me, logout as apiLogout, getToken, clearToken } from '../api/authApi'
import { getByIdCuenta } from '../api/usuarioApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => getToken())
  const [user, setUser] = useState(null)
  const [usuario, setUsuario] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  const clearAll = useCallback(() => {
    setTokenState(null)
    setUser(null)
    setUsuario(null)
    setError(null)
    clearToken()
  }, [])

  const init = useCallback(async () => {
    const storedToken = getToken()
    if (!storedToken) {
      setLoading(false)
      return
    }

    try {
      const validation = await validateSession()
      if (!validation.valido) {
        clearAll()
        return
      }

      const userData = await me()
      setUser(userData)

      const usuarioData = await getByIdCuenta(userData.userId)
      setUsuario(usuarioData)
    } catch (err) {
      console.error('Auth init error:', err)
      clearAll()
    } finally {
      setLoading(false)
    }
  }, [clearAll])

  useEffect(() => {
    init()
  }, [init])

  const login = async (nickname, clave) => {
    setError(null)
    setLoading(true)
    try {
      const response = await apiLogin(nickname, clave)
      setTokenState(response.token)

      const validation = await validateSession()
      if (!validation.valido) {
        throw new Error(validation.mensaje || 'Sesión inválida')
      }

      const userData = await me()
      setUser(userData)

      const usuarioData = await getByIdCuenta(userData.userId)
      setUsuario(usuarioData)

      navigate('/dashboard')
      return { success: true }
    } catch (err) {
      clearAll()
      const message = err.response?.data?.mensaje || err.message || 'Error al iniciar sesión'
      setError(message)
      return { success: false, error: message }
    } finally {
      setLoading(false)
    }
  }

  const logout = async () => {
    try {
      await apiLogout()
    } catch (err) {
      console.error('Logout error:', err)
    } finally {
      clearAll()
      navigate('/login')
    }
  }

  const isAuthenticated = !!token && !!user

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        usuario,
        loading,
        error,
        isAuthenticated,
        login,
        logout,
        init,
        clearError: () => setError(null),
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export default AuthContext