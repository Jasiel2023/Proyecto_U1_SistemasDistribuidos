export async function obtenerSnapshot(eleccionId, { fetchImpl = fetch, signal } = {}) {
  const respuesta = await fetchImpl(`/api/resultados/elecciones/${encodeURIComponent(eleccionId)}`, {
    headers: { Accept: 'application/json' },
    signal
  });
  if (respuesta.status === 404) {
    throw new Error('La elección no existe en el módulo de Resultados.');
  }
  if (!respuesta.ok) {
    throw new Error(`No se pudo consultar el resultado (HTTP ${respuesta.status}).`);
  }
  return respuesta.json();
}
