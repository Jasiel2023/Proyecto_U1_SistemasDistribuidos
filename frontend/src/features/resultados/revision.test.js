import { describe, expect, it } from 'vitest';
import { esRevisionMasNueva, fusionarSnapshot, normalizarSnapshot } from './revision.js';

const base = {
  eleccionId: '11111111-1111-4111-8111-111111111111',
  totalVotos: 2,
  conteos: [],
  revision: 2,
  ultimaActualizacion: '2026-01-01T00:00:00Z',
  disponible: true
};

describe('revisiones de snapshot', () => {
  it('aplica una revisión mayor', () => {
    expect(esRevisionMasNueva(base, { ...base, revision: 3 })).toBe(true);
    expect(fusionarSnapshot(base, { ...base, revision: 3 }).revision).toBe(3);
  });

  it('descarta revisiones antiguas o repetidas', () => {
    expect(esRevisionMasNueva(base, { ...base, revision: 1 })).toBe(false);
    expect(esRevisionMasNueva(base, { ...base, revision: 2 })).toBe(false);
    expect(fusionarSnapshot(base, { ...base, revision: 1 })).toBe(base);
  });

  it('rechaza snapshots incompletos', () => {
    expect(normalizarSnapshot(null)).toBeNull();
    expect(normalizarSnapshot({ ...base, revision: -1 })).toBeNull();
    expect(normalizarSnapshot({ ...base, totalVotos: 1.5 })).toBeNull();
    expect(normalizarSnapshot({ ...base, conteos: null })).toBeNull();
    expect(normalizarSnapshot(base)).toEqual(base);
  });
});
