# Contrato WebSocket — Resultados

Estado: implementado y verificado en G3 mediante el puente WebSocket MVC del gateway.

## Transporte y justificación

- **STOMP sobre WebSocket nativo** en `/ws` (sin SockJS).
- Justificación: Spring Cloud Gateway Server MVC 5.0.4 no soporta rutas WebSocket nativas; se verificó un puente MVC explícito (`WebSocketProxyConfig` + `ProxyWebSocketHandler`) que reenvía el upgrade y el tráfico hacia Resultados. Vite y nginx reenvían el upgrade al gateway. Evita la complejidad de transportes HTTP de SockJS sin aportar un requisito adicional.
- Broker simple en memoria (`/topic`). MVP de una sola instancia de Resultados; no se declara difusión horizontal.
- Heartbeats STOMP: 10000 ms cliente y servidor. Reconexión del cliente: 3000 ms (`reconnectDelay` en `useResultados.js`).

## Endpoint

```text
ws://<host>:8080/ws
```

Por Vite en desarrollo: `ws://localhost:5173/ws` (proxy a 8080).
Por nginx en demo: `ws://localhost:3000/ws` (proxy a gateway).

## Suscripción

```text
SUBSCRIBE
destination:/topic/resultados/elecciones/{eleccionId}
id:resultados-{eleccionId}
```

Un cliente debe suscribirse a una sola elección. El servidor publica únicamente en el destino de la elección afectada.

## Mensaje del servidor

`MESSAGE` con `content-type: application/json` y el cuerpo:

```json
{
  "eleccionId": "4f1c1a1e-6f31-4f7e-9f0a-2f4d0f6a1b12",
  "totalVotos": 3,
  "conteos": [
    { "candidatoId": "4f1c1a1e-6f31-4f7e-9f0a-2f4d0f6a1b13", "votos": 2 },
    { "candidatoId": "4f1c1a1e-6f31-4f7e-9f0a-2f4d0f6a1b14", "votos": 1 }
  ],
  "revision": 3,
  "ultimaActualizacion": "2026-11-08T15:04:06Z",
  "disponible": true
}
```

Reglas:

- El snapshot se emite **después del commit** de la transacción del evento.
- `revision` es monotónica por elección. El cliente descarta cualquier snapshot con `revision` menor que la ya aplicada.
- Un fallo de difusión no revierte el voto contabilizado; se registra el error.
- No incluye identidad del votante ni datos de otras elecciones.

## Estrategia de suscripción y recuperación

1. Abrir STOMP y, en `onConnect`, suscribirse primero al destino de la elección.
2. Inmediatamente después, ejecutar `GET /api/resultados/elecciones/{eleccionId}`.
3. Aplicar snapshots solo si su `revision` es estrictamente mayor que la última aplicada; descartar los antiguos. Una revisión igual es idempotente.
4. Ante desconexión, STOMP reconecta; al reconectar se cancela la suscripción previa si existe, se vuelve a suscribir y se repite GET.
5. Al cambiar de elección o desmontar, cancelar la suscripción, desactivar el cliente e ignorar respuestas tardías.

## Errores

- Conexión rechazada o cerrada: el cliente muestra `desconectado` y sigue reintentando; REST sigue disponible.
- Snapshot con forma inválida: se ignora y se registra en consola.
- Origen no permitido: el handshake se rechaza según `resultados.websocket.allowed-origins`.
- No se usa un endpoint STOMP de aplicación; no hay operaciones de escritura por WebSocket.
