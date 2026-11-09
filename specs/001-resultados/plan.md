# Plan técnico — Resultados (001)

Fecha: 2026-11-08 | Stack verificado: Java 21, Spring Boot 4.0.9, Spring Cloud 2025.1.4, PostgreSQL 17, RabbitMQ 4, React 19 + Vite 8.

## 1. Arquitectura

```text
Votación (externo, bloqueado) --VotoEmitidoEvent v1--> RabbitMQ (votacion.exchange)
                                                        |
                                                        v
                                              resultados-service (8083)
                                                 |            |
                                        PostgreSQL resultadosdb  STOMP /ws
                                                 |            |
                                              REST /api/resultados/**  /topic/resultados/elecciones/{id}
                                                        |            |
                                                     api-gateway (8080, MVC)
                                                        |            |
                                                     frontend React (Vite/nginx)
```

- Resultados es propietario exclusivo de `resultadosdb`.
- Sin Redis, sin acceso a tablas ajenas, sin entidades JPA que representen elecciones/candidatos de Votación.
- REST es la fuente de recuperación; WebSocket solo notifica snapshots confirmados.
- Una instancia de Resultados y broker STOMP simple para el MVP.

## 2. Estructura de archivos

```text
services/resultados-service/
  pom.xml
  mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
  Dockerfile
  docker-compose.yml                      # PostgreSQL propio (5436 host)
  src/main/java/ec/unl/votacion/resultados/
    ResultadosServiceApplication.java
    api/ResultadoController.java
    api/ApiExceptionHandler.java
    application/ResultadoSnapshot.java
    application/ResultadoQueryService.java
    application/ResultadoCommandService.java
    application/SnapshotNotificationService.java
    application/ResultadoActualizadoEvent.java
    application/RevisionActualizada.java
    application/ProcesamientoResultado.java
    domain/Resumen.java, ConteoCandidato.java, ConteoCandidatoId.java,
           VotoProcesado.java, EventoConflicto.java
    infrastructure/persistence/ResumenRepository.java
    infrastructure/persistence/ConteoCandidatoRepository.java
    infrastructure/persistence/VotoProcesadoRepository.java
    infrastructure/persistence/EventoConflictoRepository.java
    infrastructure/persistence/ProyeccionRepository.java   # upserts atómicos + lectura
    messaging/VotoEmitidoEvent.java
    messaging/RabbitTopologyConfig.java
    messaging/VotoEmitidoListener.java
    websocket/WebSocketConfig.java
    config/ResultadosProperties.java
    config/CorsConfig.java
  src/main/resources/application.yml
  src/main/resources/application-local.yml
  src/main/resources/application-container.yml
  src/main/resources/db/migration/V1__resultados.sql
  src/test/java/ec/unl/votacion/resultados/...
  src/test/resources/application-test.yml

services/api-gateway/
  pom.xml, Dockerfile
  src/main/java/ec/unl/votacion/gateway/
    ApiGatewayApplication.java
    WebSocketProxyConfig.java
    ProxyWebSocketHandler.java
  src/main/resources/application.yml

frontend/
  package.json, vite.config.js, index.html, Dockerfile, nginx.conf
  src/main.jsx, src/App.jsx, src/styles.css
  src/features/resultados/ResultadosPage.jsx
  src/features/resultados/useResultados.js
  src/features/resultados/resultadosApi.js
  src/features/resultados/revision.js
  src/features/resultados/revision.test.js
  src/features/resultados/useResultados.test.jsx
  src/test/setup.js

compose.full.yml
.env.example
scripts/resultados/publicar-fixtures.mjs
scripts/resultados/smoke-gateway.mjs
```

## 3. Backend

### 3.1 Dependencias

- `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`
- `spring-boot-starter-amqp`, `spring-boot-starter-websocket`, `spring-boot-starter-actuator`
- `flyway-core`, `flyway-database-postgresql`, `postgresql`
- Test: `spring-boot-starter-test`, `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-rabbitmq`, `testcontainers-junit-jupiter`, `awaitility`

### 3.2 Persistencia y concurrencia

- Flyway crea las tablas; JPA `validate` comprueba mapeos. Entidades JPA solo para el modelo propio; los upserts críticos usan `JdbcTemplate` con SQL nativo y `RETURNING`.
- Unicidad: `voto_procesado.event_id` (PK) y `(eleccion_id, voto_id)` (UNIQUE). `INSERT ... ON CONFLICT DO NOTHING` decide si es nuevo o duplicado.
- Incremento: `INSERT ... ON CONFLICT (...) DO UPDATE ... + 1` para resumen y conteo. La fila de resumen se bloquea primero, serializando la elección y evitando deadlocks; no hay read-modify-write desprotegido.
- Toda la operación (evento + resumen + conteo + auditoría de conflicto) ocurre en una transacción.
- `totalVotos` y `SUM(conteos)` se actualizan juntos; un test verifica la invariante.

### 3.3 Mensajería

- Exchange topic durable `votacion.exchange`; routing key `voto.emitido.v1`; cola durable `resultados.voto-emitido.v1` con DLX `votacion.dlx` y routing key de muerte `voto.emitido.v1.dlq`; DLQ `resultados.voto-emitido.v1.dlq`.
- Converter `JacksonJsonMessageConverter` (Jackson 3) con tipo objetivo `VotoEmitidoEvent`; `content-type=application/json`.
- `@RabbitListener` con `acknowledge-mode=auto`: el contenedor confirma después de que el proxy transaccional hace commit. Una excepción revierte y no confirma.
- Reintentos limitados: `spring.rabbitmq.listener.simple.retry` (3 intentos, backoff exponencial), `default-requeue-rejected=false`.
- Errores permanentes o reintentos agotados: rechazo sin requeue → DLX/DLQ. Sin bucles de requeue.
- Validación de contrato antes de persistir: `eventId` no nulo, `schemaVersion=1`, UUIDs y `occurredAt` no nulo. Un mensaje malformado no se convierte en voto.
- Nombres marcados **propuestos** hasta T034.

### 3.4 Consulta REST

- `GET /api/resultados/elecciones/{eleccionId}`.
- Sin proyección: 200 con snapshot vacío y `disponible=false`.
- Lectura `REPEATABLE_READ` de resumen y conteos; `ultimaActualizacion` en UTC.
- UUID inválido: 400 Problem Details. Error interno: 500 sin filtrar detalles.
- No se exponen entidades ni identidad de votante.

### 3.5 WebSocket

- `@EnableWebSocketMessageBroker`; endpoint `/ws` (WebSocket nativo); broker simple `/topic`; sin prefijo de aplicación.
- `allowed-origins` configurable y restringido por entorno.
- Al confirmar el commit, `ApplicationEventPublisher` publica `ResultadoActualizadoEvent`; un `@TransactionalEventListener(phase=AFTER_COMMIT)` recarga el snapshot y lo envía a `/topic/resultados/elecciones/{eleccionId}`.
- Fallo de envío: se captura y registra; no se propaga ni revierte el commit.
- Revisión monotónica por elección; el cliente descarta revisiones menores.

### 3.6 Seguridad y perfiles

- `local`: acceso público de lectura, CORS limitado a `localhost:5173`/`8080`.
- `container`: DNS de Compose (`resultados-db`, `rabbitmq`), orígenes configurables.
- `test`: Testcontainers.
- Sin secretos reales; `.env.example` con valores de desarrollo.
- Si el contrato externo exige auth, T031 se bloquea y se registra; no se inventa JWT.

## 4. Gateway MVC

- `spring-cloud-starter-gateway-server-webmvc` 5.0.4 + `spring-boot-starter-websocket`.
- Ruta REST en `spring.cloud.gateway.server.webmvc.routes`: `/api/resultados/**` → `${RESULTADOS_URL:http://localhost:8083}`.
- Se conserva `/api/votos/**` si el módulo existiera; en este repositorio no hay ruta previa que romper.
- **Hallazgo verificado:** Gateway MVC 5.0.4 no incluye `HandlerFunctions.websocket()` ni un handler para el esquema `ws://`; una ruta WebSocket por propiedades falla al iniciar y el intento de upgrade por `http()` devuelve 400. El soporte nativo aparece en la línea 5.1, aún no estable en esta combinación de BOM.
- **Alternativa mínima aplicada:** puente MVC explícito `/ws` → `ws://resultados-service:8083/ws` con `WebSocketProxyConfig` y `ProxyWebSocketHandler` (cliente y servidor `spring-websocket`, relay de texto/binario, cierre y errores). No se cambió a gateway reactivo.
- Evidencia: handshake por `localhost:8080/ws` → 101; tráfico STOMP de dos clientes a través de `localhost:3000` → gateway → Resultados → OK (`smoke-gateway.mjs`).
- Orígenes del puente restringidos por `GATEWAY_WS_ALLOWED_ORIGINS`; origen no permitido → 403.

## 5. Frontend

- React 19 + Vite 8; proxy `/api` a `http://localhost:8080` y `/ws` a `ws://localhost:8080` con `ws: true`.
- Feature con entrada de UUID de elección, total, tabla de conteos, timestamp, revisión, estado de conexión y estados vacío/cargando/error.
- `@stomp/stompjs` con WebSocket nativo. Secuencia: `onConnect` → suscribirse → GET; descartar revisiones menores; al reconectar volver a suscribirse y GET; limpiar al cambiar de elección/desmontar.
- Sin inventar catálogo ni nombres; se muestran UUIDs.
- Build de producción servido por nginx con proxy `/api` y `/ws` al gateway.

## 6. Pruebas y puertas

| Tipo | Herramienta | Evidencia |
|---|---|---|
| Contrato de evento | JUnit + Jackson estricto sobre ejemplos | válidos aceptados; inválidos rechazados |
| US1 REST | `@SpringBootTest` + Testcontainers PostgreSQL | snapshot, vacío, aislamiento, 400 |
| Deduplicación/concurrencia | Servicio real + PostgreSQL | mismo eventId, mismo voto, conflicto, N hilos, suma |
| Rollback | Trigger temporal que falla en conteo | sin evento/resumen/conteo parcial |
| RabbitMQ real | Testcontainers RabbitMQ + RabbitTemplate | evento persistido, DLQ, consumidores concurrentes |
| WebSocket | STOMP client real contra servicio | after-commit, rollback, aislamiento, REST recovery |
| Frontend | Vitest + Testing Library | revisiones fuera de orden, reconexión, cambio de elección |
| Compose | `docker compose config` + smoke | G3 |

G0: inventario/versiones. G1: spec/plan/contratos/modelo. G2: servicio aislado con RabbitMQ real. G3: gateway/frontend/contenedores. G4: productor real; bloqueado mientras no exista.

## 7. Renumeraciones y trazabilidad

No se renumeran tareas. `tasks.md` mantiene T001–T040 y cada tarea referencia US/FR. El progreso vive solo en `specs/001-resultados/tasks.md`; `task.md` es la guía de entrada.
