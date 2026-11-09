import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { obtenerSnapshot } from './resultadosApi.js';
import { fusionarSnapshot, normalizarSnapshot } from './revision.js';

export function urlWebSocket() {
  const protocolo = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocolo}//${window.location.host}/ws`;
}

export function crearClienteStomp() {
  return new Client({
    brokerURL: urlWebSocket(),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000
  });
}

const ESTADO_INICIAL = {
  snapshot: null,
  cargando: false,
  error: null,
  conexion: 'inactivo'
};

export function useResultados(eleccionId, dependencias = {}) {
  const crearClienteRef = useRef(dependencias.crearCliente ?? crearClienteStomp);
  const consultarRef = useRef(dependencias.consultar ?? obtenerSnapshot);
  crearClienteRef.current = dependencias.crearCliente ?? crearClienteStomp;
  consultarRef.current = dependencias.consultar ?? obtenerSnapshot;
  const [estado, setEstado] = useState(ESTADO_INICIAL);

  useEffect(() => {
    if (!eleccionId) {
      setEstado(ESTADO_INICIAL);
      return undefined;
    }

    let cancelado = false;
    let suscripcion;
    const controlador = new AbortController();
    const cliente = crearClienteRef.current();

    setEstado({ snapshot: null, cargando: true, error: null, conexion: 'conectando' });

    const sincronizar = () => {
      consultarRef.current(eleccionId, { signal: controlador.signal })
        .then((respuesta) => {
          if (cancelado) return;
          const snapshot = normalizarSnapshot(respuesta);
          if (!snapshot) throw new Error('El servidor devolvió un snapshot inválido.');
          setEstado((anterior) => ({
            ...anterior,
            snapshot: fusionarSnapshot(anterior.snapshot, snapshot),
            cargando: false,
            error: null
          }));
        })
        .catch((error) => {
          if (cancelado || error.name === 'AbortError') return;
          setEstado((anterior) => ({ ...anterior, cargando: false, error: error.message }));
        });
    };

    cliente.onConnect = () => {
      if (cancelado) return;
      setEstado((anterior) => ({ ...anterior, conexion: 'conectado', error: null }));
      suscripcion?.unsubscribe();
      suscripcion = cliente.subscribe(
        `/topic/resultados/elecciones/${eleccionId}`,
        (mensaje) => {
          if (cancelado) return;
          try {
            const snapshot = normalizarSnapshot(JSON.parse(mensaje.body));
            if (!snapshot || snapshot.eleccionId !== eleccionId) return;
            setEstado((anterior) => ({
              ...anterior,
              snapshot: fusionarSnapshot(anterior.snapshot, snapshot),
              cargando: false,
              error: null
            }));
          } catch {
            setEstado((anterior) => ({ ...anterior, error: 'Se recibió una notificación ilegible.' }));
          }
        }
      );
      sincronizar();
    };

    cliente.onWebSocketClose = () => {
      if (cancelado) return;
      setEstado((anterior) => ({ ...anterior, conexion: 'reconectando' }));
    };

    cliente.onStompError = (frame) => {
      if (cancelado) return;
      setEstado((anterior) => ({
        ...anterior,
        conexion: 'error',
        error: frame.headers?.message ?? 'Error del canal STOMP.'
      }));
    };

    cliente.activate();

    return () => {
      cancelado = true;
      controlador.abort();
      suscripcion?.unsubscribe();
      cliente.deactivate();
    };
  }, [eleccionId]);

  return estado;
}
