import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useResultados } from './useResultados.js';

const ELECCION_A = '11111111-1111-4111-8111-111111111111';
const ELECCION_B = '22222222-2222-4222-8222-222222222222';

function snapshot(eleccionId, revision) {
  return {
    eleccionId,
    totalVotos: revision,
    conteos: [],
    revision,
    ultimaActualizacion: '2026-01-01T00:00:00Z',
    disponible: true
  };
}

function crearClienteFalso() {
  const cliente = {
    onConnect: null,
    onWebSocketClose: null,
    onStompError: null,
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn((destino, recibir) => {
      cliente.destino = destino;
      cliente.recibir = recibir;
      return { unsubscribe: vi.fn() };
    })
  };
  return cliente;
}

describe('useResultados', () => {
  it('se suscribe antes del GET y se resincroniza al reconectar', async () => {
    const cliente = crearClienteFalso();
    const consultar = vi.fn().mockResolvedValue(snapshot(ELECCION_A, 1));
    const { result } = renderHook(() => useResultados(ELECCION_A, {
      crearCliente: () => cliente,
      consultar
    }));

    expect(cliente.activate).toHaveBeenCalledTimes(1);
    act(() => cliente.onConnect());
    expect(cliente.subscribe).toHaveBeenCalledWith(
      `/topic/resultados/elecciones/${ELECCION_A}`,
      expect.any(Function)
    );
    await waitFor(() => expect(result.current.snapshot?.revision).toBe(1));

    act(() => cliente.onWebSocketClose());
    expect(result.current.conexion).toBe('reconectando');

    act(() => cliente.onConnect());
    await waitFor(() => expect(consultar).toHaveBeenCalledTimes(2));
    expect(cliente.subscribe).toHaveBeenCalledTimes(2);
  });

  it('no retrocede si el GET llega después de una notificación más nueva', async () => {
    const cliente = crearClienteFalso();
    let resolver;
    const consultar = vi.fn(() => new Promise((resolve) => { resolver = resolve; }));
    const { result } = renderHook(() => useResultados(ELECCION_A, {
      crearCliente: () => cliente,
      consultar
    }));

    act(() => cliente.onConnect());
    act(() => cliente.recibir({ body: JSON.stringify(snapshot(ELECCION_A, 3)) }));
    expect(result.current.snapshot?.revision).toBe(3);

    act(() => resolver(snapshot(ELECCION_A, 2)));
    await waitFor(() => expect(consultar).toHaveBeenCalledTimes(1));
    expect(result.current.snapshot?.revision).toBe(3);
  });

  it('cancela la suscripción y reinicia el estado al cambiar de elección', async () => {
    const clientes = [];
    const crearCliente = vi.fn(() => {
      const cliente = crearClienteFalso();
      clientes.push(cliente);
      return cliente;
    });
    const consultar = vi.fn((eleccionId) => Promise.resolve(snapshot(eleccionId, 1)));
    const { result, rerender, unmount } = renderHook(
      ({ eleccionId }) => useResultados(eleccionId, { crearCliente, consultar }),
      { initialProps: { eleccionId: ELECCION_A } }
    );

    act(() => clientes[0].onConnect());
    await waitFor(() => expect(result.current.snapshot?.eleccionId).toBe(ELECCION_A));

    rerender({ eleccionId: ELECCION_B });
    expect(clientes[0].deactivate).toHaveBeenCalledTimes(1);
    expect(clientes).toHaveLength(2);
    act(() => clientes[1].onConnect());
    await waitFor(() => expect(consultar).toHaveBeenCalledWith(ELECCION_B, expect.anything()));
    expect(clientes[1].destino).toBe(`/topic/resultados/elecciones/${ELECCION_B}`);

    unmount();
    expect(clientes[1].deactivate).toHaveBeenCalledTimes(1);
  });
});
