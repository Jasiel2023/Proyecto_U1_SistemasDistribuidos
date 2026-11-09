# Quickstart — módulo de Resultados

Estado: **Resultados aislado completo; integración pendiente (G4)**.
La integración con un productor real de Votación y con Autenticación sigue bloqueada (B001/B002/B003).

## 1. Requisitos

- Docker Desktop con Compose v2.
- JDK 21 para ejecutar los servicios desde consola.
- Node.js 20+ y npm para el frontend y los scripts.
- PowerShell 5.1 o superior.

En este equipo el `java` del PATH puede apuntar a Java 8. Para las pruebas locales:

```powershell
$env:JAVA_HOME = "C:\Users\Are\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
```

## 2. Variables de entorno

```powershell
Copy-Item .env.example .env
```

Ajuste solo valores de desarrollo. No versione `.env` ni credenciales reales.
Variables principales: `RESULTADOS_DB_*`, `RABBITMQ_*`, `GATEWAY_PORT`, `FRONTEND_PORT`,
`RESULTADOS_URL`, `RESULTADOS_WS_URL`, `RESULTADOS_CORS_ALLOWED_ORIGINS`,
`RESULTADOS_WS_ALLOWED_ORIGINS` y `GATEWAY_WS_ALLOWED_ORIGINS`.

## 3. Opción A — todo en contenedores

```powershell
docker compose -f compose.full.yml up -d --build
docker compose -f compose.full.yml ps
```

Servicios esperados: `resultados-db`, `rabbitmq`, `resultados-service`, `api-gateway` y `frontend`.

| Servicio | URL local |
|---|---|
| Frontend (nginx) | `http://localhost:3000` |
| Gateway REST | `http://localhost:8080/api/resultados/...` |
| Gateway WebSocket | `ws://localhost:8080/ws` |
| Resultados directo | `http://localhost:8083` |
| RabbitMQ Management | `http://localhost:15672` |
| PostgreSQL Resultados | `localhost:5436` |

Para detener sin borrar datos:

```powershell
docker compose -f compose.full.yml down
```

Para borrar también los volúmenes:

```powershell
docker compose -f compose.full.yml down -v
```

## 4. Opción B — infraestructura en Docker y servicios en local

```powershell
docker compose -f compose.full.yml up -d resultados-db rabbitmq

# Terminal 1: Resultados (perfil local por defecto)
cd services\resultados-service
.\mvnw.cmd spring-boot:run

# Terminal 2: gateway
cd services\api-gateway
.\mvnw.cmd spring-boot:run

# Terminal 3: frontend
cd frontend
npm install
npm run dev
```

Frontend de desarrollo: `http://localhost:5173`; Vite reenvía `/api` y `/ws` al gateway `8080`.

## 5. Publicar fixtures

Genera UUIDs reproducibles a partir de una elección y publica eventos v1 por RabbitMQ.
No existe ningún endpoint REST para alterar conteos.

```powershell
$eleccion = [guid]::NewGuid().ToString()
node scripts\resultados\publicar-fixtures.mjs --eleccion $eleccion --votos 6 --duplicados 2
```

Si cambió las credenciales de RabbitMQ en `.env`, pase `--user` y `--password`.
El script bloquea hosts remotos salvo que se use `--allow-remote` de forma explícita.

## 6. Consultar resultados

```powershell
Invoke-RestMethod "http://localhost:3000/api/resultados/elecciones/$eleccion"
Invoke-RestMethod "http://localhost:8080/api/resultados/elecciones/$eleccion"
Invoke-RestMethod "http://localhost:8083/api/resultados/elecciones/$eleccion"
```

Respuesta sin proyección: HTTP 200 con `disponible=false`, `totalVotos=0`, `revision=0`
y `ultimaActualizacion=null`. No afirma que la elección exista en Votación.

En el frontend abra `http://localhost:3000` o `http://localhost:5173` e ingrese el UUID.
También puede usar `?eleccion=<uuid>` en la URL.

## 7. WebSocket

- Transporte: STOMP sobre WebSocket nativo (sin SockJS).
- Endpoint: `/ws` en el gateway (`8080`) o directo a Resultados (`8083`).
- Destino: `/topic/resultados/elecciones/{eleccionId}`.
- El snapshot se envía después del commit; REST es la fuente de recuperación.
- El cliente descarta revisiones no mayores que la aplicada.

Smoke con dos clientes, REST y duplicados:

```powershell
node scripts\resultados\smoke-gateway.mjs --base http://localhost:3000 --votos 6 --duplicados 2
```

## 8. Pruebas y validación

```powershell
# Backend: 28 pruebas con PostgreSQL y RabbitMQ reales (Testcontainers)
cd services\resultados-service
.\mvnw.cmd -B test

# Empaquetado
.\mvnw.cmd -B -DskipTests package
cd ..\api-gateway
.\mvnw.cmd -B -DskipTests package

# Frontend: 6 pruebas y build
cd ..\..\frontend
npm test
npm run build

# Compose
cd ..
docker compose -f compose.full.yml config --quiet
```

Las pruebas de integración requieren Docker activo. Las trazas de error de rollback,
DLQ o JSON inválido son escenarios negativos esperados; el resumen debe ser
`Tests run: 28, Failures: 0, Errors: 0`.

## 9. DLQ y diagnóstico

- Exchange: `votacion.exchange` (topic durable, **propuesto**).
- Routing key: `voto.emitido.v1`.
- Cola: `resultados.voto-emitido.v1`.
- DLX: `votacion.dlx`; DLQ: `resultados.voto-emitido.v1.dlq`.
- Revise colas y mensajes en `http://localhost:15672`.
- Un mensaje inválido o con `schemaVersion != 1` termina en DLQ tras reintentos limitados.
- Si el evento no se enruta, verifique exchange, routing key y binding.
- Si la elección no muestra datos, publique fixtures y consulte de nuevo.
- Si el handshake devuelve 403, revise los orígenes permitidos en `.env`.
- Si el puerto está ocupado, cambie `GATEWAY_PORT`, `FRONTEND_PORT` o
  `RESULTADOS_DB_HOST_PORT`.
- Un reinicio de Resultados conserva conteos en el volumen de PostgreSQL.

## 10. Límites conocidos

- Una sola instancia de Resultados y broker STOMP simple; sin difusión horizontal.
- Sin catálogo de elecciones/candidatos; se muestran UUIDs.
- Sin anulaciones ni cambios de voto.
- Sin purga de `voto_procesado`; la deduplicación se conserva en el MVP.
- Acceso público local explícito; no hay JWT ni integración con Autenticación.
- No se declara integración completa con Votación: falta productor real y contrato confirmado.
