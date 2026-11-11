export function decodeToken(token) {
  if (!token) return null
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (e) {
    console.error('Error decoding token:', e)
    return null
  }
}

export function isTokenExpired(token) {
  const decoded = decodeToken(token)
  if (!decoded?.exp) return true
  const now = Math.floor(Date.now() / 1000)
  return decoded.exp < now
}

export function getTokenExpiration(token) {
  const decoded = decodeToken(token)
  if (!decoded?.exp) return null
  return new Date(decoded.exp * 1000)
}