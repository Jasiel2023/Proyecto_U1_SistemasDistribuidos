import { api } from './client'

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

export async function login(nickname, clave) {
  const { data } = await api.post('/v1/auth/login', { nickname, clave })
  if (data.token) {
    setToken(data.token)
  }
  return data
}

export async function validateSession() {
  const token = getToken()
  if (!token) {
    return { valido: false, mensaje: 'Sin sesión activa' }
  }
  const { data } = await api.post('/v1/auth/validate')
  return data
}

export async function me() {
  const { data } = await api.get('/v1/auth/me')
  return data
}

export async function logout() {
  try {
    if (getToken()) {
      await api.post('/v1/auth/logout')
    }
  } finally {
    clearToken()
  }
}