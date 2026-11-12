const STORAGE_PREFIX = 'votacion.votos.registrados'

function storageKey(userId) {
  return `${STORAGE_PREFIX}.${userId}`
}

export function getVotosRegistrados(userId) {
  if (!userId) return []

  try {
    const stored = JSON.parse(localStorage.getItem(storageKey(userId)) || '[]')
    return Array.isArray(stored) ? stored.map(String) : []
  } catch {
    return []
  }
}

export function registrarVotoLocal(userId, eleccionId) {
  if (!userId || !eleccionId) return

  const votos = new Set(getVotosRegistrados(userId))
  votos.add(String(eleccionId))
  localStorage.setItem(storageKey(userId), JSON.stringify([...votos]))
}
