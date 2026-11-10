import { api } from '../../api/client'

const TOKEN_KEY = 'votacion.auth.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function login(nickname, clave) {
  const { data } = await api.post('/v1/auth/login', { nickname, clave })
  setToken(data.token)
  return data
}

export async function register(nickname, clave) {
  const { data } = await api.post('/v1/auth/register', { nickname, clave })
  return data
}

export async function validateSession() {
  if (!getToken()) {
    return { valido: false, mensaje: 'Sin sesion activa' }
  }
  const { data } = await api.post('/v1/auth/validate', null, { headers: authHeaders() })
  return data
}

export async function me() {
  const { data } = await api.get('/v1/auth/me', { headers: authHeaders() })
  return data
}

export async function logout() {
  try {
    if (getToken()) {
      await api.post('/v1/auth/logout', null, { headers: authHeaders() })
    }
  } finally {
    clearToken()
  }
}
