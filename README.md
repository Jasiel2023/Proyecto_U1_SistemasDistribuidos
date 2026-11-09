# Proyecto_U1_SistemasDistribuidos

Sistema de votaciones en tiempo real. Este repositorio contiene actualmente el **módulo de Resultados** y la integración mínima necesaria con el gateway y el frontend.

## Estado

- **Resultados aislado completo; integración pendiente (G4).**
- G0–G3 cumplen con evidencia en `specs/001-resultados/validation.md`.
- G4 está bloqueado: no existe productor real de Votación ni proveedor de Autenticación en el repositorio.
- Autenticación, Usuarios y Votación no están implementados aquí; sus contratos son dependencias externas.

## Arquitectura implementada

```text
Frontend React (5173 / nginx 3000)
        |  /api, /ws
        v
api-gateway MVC (8080)
        |  REST /api/resultados/**  +  puente WebSocket /ws
        v
resultados-service (8083) ---> PostgreSQL resultadosdb (5436)
        ^
        |  VotoEmitidoEvent v1 (RabbitMQ)
Votación (productor externo, pendiente)
```

- `resultados-service`: consumo RabbitMQ idempotente, proyección persistente, REST de solo lectura y notificaciones STOMP.
- `api-gateway`: enruta REST y actúa como puente WebSocket explícito para MVC.
- `frontend`: feature React de resultados con recuperación REST y actualizaciones en vivo.
- PostgreSQL propio de Resultados; sin acceso a bases de otros servicios.
- Sin Redis en Resultados; la prevención de doble voto corresponde a Votación.

## Servicios y puertos

| Componente | Puerto local | Evidencia |
|---|---|---|
| Frontend (Vite / nginx) | 5173 / 3000 | `frontend/` |
| API Gateway | 8080 | `services/api-gateway/` |
| Resultados | 8083 | `services/resultados-service/` |
| PostgreSQL Resultados | 5436 | `compose.full.yml` |
| RabbitMQ | 5672 / 15672 | `compose.full.yml` |

## Inicio rápido

Requisitos: Docker Desktop, JDK 21, Node.js 20+.

```powershell
Copy-Item .env.example .env
docker compose -f compose.full.yml up -d --build
```

- Frontend: `http://localhost:3000`
- Gateway: `http://localhost:8080/api/resultados/elecciones/{uuid}`
- WebSocket: `ws://localhost:8080/ws`
- RabbitMQ Management: `http://localhost:15672`

Publicar fixtures y consultar:

```powershell
$eleccion = [guid]::NewGuid().ToString()
node scripts\resultados\publicar-fixtures.mjs --eleccion $eleccion --votos 6 --duplicados 2
Invoke-RestMethod "http://localhost:3000/api/resultados/elecciones/$eleccion"
```

Instrucciones completas, pruebas, DLQ y diagnóstico: `specs/001-resultados/quickstart.md`.

## Contratos

- Evento: `specs/001-resultados/contracts/voto-emitido.v1.schema.json` (**propuesto**).
- REST: `specs/001-resultados/contracts/resultados.openapi.yaml`.
- WebSocket: `specs/001-resultados/contracts/websocket.md`.
- Modelo de datos: `specs/001-resultados/data-model.md`.

## Pruebas

```powershell
$env:JAVA_HOME = "C:\Users\Are\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
cd services\resultados-service
.\mvnw.cmd -B test          # 28 pruebas, PostgreSQL y RabbitMQ reales
cd ..\..\frontend
npm test                    # 6 pruebas
npm run build
cd ..
docker compose -f compose.full.yml config --quiet
```

## Documentación

- Especificación: `specs/001-resultados/spec.md`
- Plan: `specs/001-resultados/plan.md`
- Tareas: `specs/001-resultados/tasks.md`
- Validación y puertas: `specs/001-resultados/validation.md`
- Investigación y límites: `specs/001-resultados/research.md`
