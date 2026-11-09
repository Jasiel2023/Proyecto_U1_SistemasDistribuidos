# Validación y puertas de calidad — Resultados

Fecha: 2026-11-08. Solo se marca una puerta/prueba cuando existe evidencia ejecutada. Los bloqueos externos se registran, no se simulan.

## G0 — Inventario y versiones comprobados

Estado: **CUMPLE** (T001–T002).

- `git status --short` inicial: solo `constitution.md` y `task.md` sin seguimiento; un commit inicial. No existen los esqueletos descritos por el informe previo.
- JDK 21.0.13 Temurin verificado (portátil para la validación), Maven 3.9.11 verificado, Node 24.16.0/npm 11.13.0, Docker Server 29.6.2.
- Maven Central consultado: Boot 4.1.2 existe; Cloud 2025.1.4 existe; el BOM de Cloud 2025.1.4 declara Boot 4.0.9. Se adopta Boot 4.0.9 + Cloud 2025.1.4 como combinación verificada.
- Evidencia detallada: `research.md` §1–§3.

## G1 — Spec, plan, modelo y contratos coherentes

Estado: **CUMPLE** (T003–T009).

- `spec.md`: US1–US4, FR-001–FR-025 y criterios Given/When/Then; exclusiones y dependencias externas explícitas.
- `plan.md`: estructura de archivos, stack verificado, upsert atómico, ACK/retry/DLQ, after-commit, una instancia, perfiles y despliegue.
- `data-model.md`: tablas, PK/UNIQUE, BIGINT, TIMESTAMPTZ, invariantes y flujo transaccional.
- `contracts/`: esquema de evento v1 con ejemplos válidos/inválidos, OpenAPI REST y contrato WebSocket.
- Coherencia revisada: REST usa `disponible` y revisión 0 para ausencia de proyección; WebSocket reutiliza el mismo snapshot; la deduplicación se apoya en restricciones de base, no en lecturas previas; no hay dependencia interna bloqueante. Las dependencias externas (productor Votación, Autenticación, catálogo) quedan aisladas.
- `tasks.md` mantiene T001–T040 con dependencias y correspondencia US/FR; no hay una segunda lista de progreso.

## G2 — Módulo aislado completo y probado con eventos reales por RabbitMQ

Estado: **CUMPLE** (T010–T024 completas, incluida T021).

- `.\mvnw.cmd -B -DskipTests package` → BUILD SUCCESS con JDK 21.
- `.\mvnw.cmd -B test` → **28 pruebas, 0 fallos, 0 errores** (PostgreSQL 17.11 y RabbitMQ 4 reales vía Testcontainers).
- US1: snapshot vacío, conteos, aislamiento y contrato OpenAPI (`Us1ResultadoQueryIntegrationTest`, `VotoEmitidoContractTest`).
- US2: deduplicación por `eventId` y `(eleccionId, votoId)`, conflicto auditado, concurrencia, rollback, reentrega, reintentos limitados y DLQ (`EventoAggregationIntegrationTest`, `RabbitMessagingIntegrationTest`).
- US3: notificación solo después del commit, ausencia tras rollback, aislamiento por elección y recuperación REST de la revisión notificada (`WebSocketIntegrationTest`).
- Verificación manual adicional: evento publicado por la API de gestión de RabbitMQ y snapshot recibido por cliente STOMP real en `/topic/resultados/elecciones/{id}`.
- Evidencia de comandos y salida: `research.md` §3 y logs de Maven de la sesión de validación.

## G3 — Gateway, frontend y contenedores verificados

Estado: **CUMPLE** (T025–T033; G4 sigue bloqueado).

- Frontend: `npm test` → **6 pruebas, 0 fallos**; `npm run build` → BUILD SUCCESS (Vite 8, React 19).
- Gateway MVC: `GET /api/resultados/**` por 8080 responde 200 y conserva la ruta; handshake `/ws` por 8080 responde 101.
- Spring Cloud Gateway Server MVC 5.0.4 no incluye `HandlerFunctions.websocket()`; el upgrade directo por la ruta MVC falla. Se implementó un puente WebSocket MVC explícito (`WebSocketProxyConfig` + `ProxyWebSocketHandler`) sin cambiar a gateway reactivo. Decisión y evidencia en `research.md` §7.
- `docker compose -f compose.full.yml config --quiet` → válido; build de las tres imágenes → OK.
- `docker compose -f compose.full.yml up -d` → PostgreSQL, RabbitMQ, Resultados, gateway y frontend saludables.
- Smoke real por `http://localhost:3000` (nginx → gateway → Resultados): 6 votos, 2 duplicados, **dos clientes WebSocket** con total 6 y REST con total 6/revisión 6. Script: `scripts/resultados/smoke-gateway.mjs`.
- Reinicio de `resultados-service` conservó el total 6/revisión 6 en el volumen PostgreSQL.
- Orígenes: CORS `http://localhost:5173` → 200, origen no permitido → 403; WebSocket permitido → 101, no permitido → 403.
- Frontend servido por nginx: `/` → 200 y assets → 200. Sin nombres de catálogo inventados; se muestran UUIDs.

## Consolidación T036–T040 (2026-11-08)

Re-ejecución de cierre con Docker Desktop 29.6.2 activo, JDK 21.0.9 y Node 24.16.0:

| Comando | Resultado |
|---|---|
| `services/resultados-service/.\mvnw.cmd -B -DskipTests package` | BUILD SUCCESS |
| `services/api-gateway/.\mvnw.cmd -B -DskipTests package` | BUILD SUCCESS |
| `services/resultados-service/.\mvnw.cmd -B test` | 28 pruebas, 0 fallos, 0 errores |
| `frontend/npm test` | 6 pruebas, 0 fallos |
| `frontend/npm run build` | BUILD SUCCESS (Vite 8.3.6) |
| `docker compose -f compose.full.yml config --quiet` | OK |

La salida de las pruebas incluye trazas esperadas de escenarios negativos (rollback, DLQ y JSON inválido); no son fallos: el resumen de Surefire es 28/0/0.

**T034:** no existe `services/votacion-service` ni productor real en el repositorio. Se cierra como bloqueo registrado (B001); el contrato permanece PROPUESTO. **T035:** permanece abierta y bloqueada; no se declara G4.

**T039 — revisión de cumplimiento:**
- Sin secretos reales: solo valores de desarrollo por defecto en perfiles y `.env.example`; no hay credenciales de producción.
- Sin acceso a `votaciondb`, `usuariosdb` o `autenticaciondb`; sin Redis; `ddl-auto=validate` en main y test.
- Sin mocks activos por defecto: Testcontainers solo en pruebas; el runtime usa PostgreSQL/RabbitMQ reales.
- Se corrigieron contradicciones de `contracts/websocket.md`: reconexión 3000 ms (código real), demo por puerto 3000 y descarte de revisiones iguales.
- Trazabilidad: US1 → `Us1ResultadoQueryIntegrationTest` + `VotoEmitidoContractTest`; US2 → `EventoAggregationIntegrationTest` + `RabbitMessagingIntegrationTest`; US3 → `WebSocketIntegrationTest` + pruebas frontend; US4 → `compose config`, builds y smoke previo de G3.
- Adaptación registrada: `task.md` refleja las casillas completas por solicitud explícita del usuario, pero la fuente canónica sigue siendo este `tasks.md`.

**T040:** declaración final **“Resultados aislado completo; integración pendiente”**. G0–G3 cumplen con evidencia; G4 sigue bloqueado por B001/B002/B003.

## G4 — Integración con productor real y política de acceso validada

Estado: **BLOQUEADO**.

- No existe productor de Votación ni contrato confirmado en el repositorio; T034 se cerró como bloqueo registrado y T035 permanece abierta.
- No existe proveedor de Autenticación verificado; la integración protegida de T031 queda bloqueada. El MVP usa lectura pública local explícita.
- No se declara “Resultados integrado completo”.

## Registro de bloqueos

| ID | Bloqueo | Responsable/dependencia | Impacto | Estado |
|---|---|---|---|---|
| B001 | Contrato/exchange/colas reales de Votación | Equipo Votación | T035, G4 (T034 cerrada como bloqueo registrado) | Abierto |
| B002 | Proveedor de Autenticación y contrato de acceso | Equipo Autenticación | T031 protegido, G4 | Abierto |
| B003 | Catálogo/nombres de elecciones y candidatos | Equipo Votación/Usuarios | Nombres en frontend; se muestran UUID | Abierto |
