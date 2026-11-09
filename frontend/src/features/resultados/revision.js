export function esRevisionMasNueva(actual, nuevo) {
  if (!nuevo) return false;
  if (!actual) return true;
  return Number(nuevo.revision) > Number(actual.revision);
}

export function fusionarSnapshot(actual, nuevo) {
  return esRevisionMasNueva(actual, nuevo) ? nuevo : actual;
}

export function normalizarSnapshot(valor) {
  if (!valor || typeof valor.eleccionId !== 'string') return null;
  if (!Number.isInteger(valor.revision) || valor.revision < 0) return null;
  if (!Number.isInteger(valor.totalVotos) || valor.totalVotos < 0) return null;
  if (!Array.isArray(valor.conteos)) return null;
  return valor;
}
