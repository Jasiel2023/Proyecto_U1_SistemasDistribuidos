# Especificación — Módulo de Resultados (001)

Versión: 1.0.0 | Fecha: 2026-11-08 | Estado: aprobada para implementación con dependencias externas aisladas
Constitución canónica: `.specify/memory/constitution.md` (copia íntegra de `constitution.md`; sin divergencias).

## 1. Objetivo y alcance

Entregar el módulo de Resultados del sistema de votaciones: un microservicio propietario de `resultadosdb`, que consume eventos `VotoEmitidoEvent` v1 desde RabbitMQ, mantiene conteos persistentes idempotentes, expone consultas REST, notifica snapshots por WebSocket después del commit e incluye la interfaz React y la integración mínima con gateway y despliegue.

Incluido:

- Servicio `services/resultados-service`, paquete `ec.unl.votacion.resultados`, puerto local 8083.
- PostgreSQL propio `resultadosdb` (host local publicado en 5436), migraciones Flyway e Hibernate `validate`.
- Consumo RabbitMQ con deduplicación, reintentos limitados y DLQ.
- REST de solo lectura `GET /api/resultados/elecciones/{eleccionId}`.
- WebSocket STOMP en `/ws`, destino `/topic/resultados/elecciones/{eleccionId}`.
- Feature `frontend/src/features/resultados`.
- Rutas mínimas en gateway para REST y WebSocket y proxy de Vite.
- Compose de infraestructura, Dockerfiles, fixtures reproducibles y documentación operativa.

Fuera de alcance:

- Implementar Autenticación, Usuarios o Votación; sus contratos y productores son dependencias externas.
- Crear endpoints públicos o privados para alterar conteos.
- Anulaciones, cambios de voto, recálculos, votos presenciales o multi-instancia WebSocket.
- Catálogo de elecciones/candidatos propio; no se inventan nombres ni existencia en Votación.
- Declarar seguridad integrada terminada sin proveedor real de autenticación.

## 2. Actores y dependencias externas

- **Operador/demostrador**: levanta infraestructura y publica fixtures.
- **Cliente de consulta**: navegador o consumidor REST que lee resultados.
- **Productor Votación** (externo, bloqueado): publica `VotoEmitidoEvent` v1 en RabbitMQ.
- **Proveedor de Autenticación** (externo, bloqueado): no existe contrato verificado; el MVP usa lectura pública local explícita.
- **Catálogo de elecciones/candidatos** (externo, bloqueado): el frontend recibe el ID de elección y muestra UUIDs de candidatos sin inventar nombres.

## 3. Historias de usuario priorizadas

### US1 (P1) — Consultar resultados persistentes

Como cliente de resultados quiero consultar por elección el total y los conteos persistidos para conocer el estado confirmado aunque no haya WebSocket disponible.

Prueba independiente: con fixtures insertados directamente en `resultadosdb`, `GET /api/resultados/elecciones/{id}` devuelve un snapshot correcto sin Votación ni RabbitMQ.

Criterios de aceptación:

1. **Given** una elección con resumen y conteos persistidos, **When** se consulta su ID, **Then** la respuesta 200 incluye `eleccionId`, `totalVotos`, `conteos`, `revision`, `ultimaActualizacion` y `disponible=true`.
2. **Given** una elección sin proyección, **When** se consulta un UUID válido, **Then** la respuesta 200 devuelve `totalVotos=0`, `conteos=[]`, `revision=0`, `ultimaActualizacion=null`, `disponible=false`, sin afirmar que la elección existe en Votación.
3. **Given** dos elecciones con conteos, **When** se consulta una, **Then** no aparecen datos de la otra.
4. **Given** un ID con formato inválido, **When** se consulta, **Then** la respuesta es 400 con Problem Details uniforme.
5. **Given** cualquier respuesta, **Then** no se expone identidad de votantes ni entidades JPA.
6. **Given** una lectura concurrente con una escritura confirmada, **Then** resumen y conteos corresponden a una misma revisión consistente.

### US2 (P1) — Contabilizar eventos sin duplicar

Como sistema de resultados quiero procesar eventos de voto de RabbitMQ de forma idempotente y transaccional para que reentregas y concurrencia no corrompan los totales.

Prueba independiente: publicar eventos conformes al contrato en RabbitMQ real y consultar por REST; Votación puede no existir.

Criterios de aceptación:

1. **Given** un evento v1 válido, **When** se consume, **Then** se persiste el voto procesado, se incrementa el resumen y su conteo, y solo entonces se confirma el ACK.
2. **Given** el mismo `eventId` reentregado, **When** se consume de nuevo, **Then** no se incrementa ningún total y el mensaje se confirma sin bucle.
3. **Given** el mismo voto lógico `(eleccionId, votoId)` con otro `eventId`, **Then** no se incrementa y se registra la duplicidad.
4. **Given** un `eventId` repetido con payload incompatible, **Then** se detecta y registra el conflicto, sin sobrescribir ni sumar.
5. **Given** dos elecciones concurrentes, **Then** cada una mantiene sus conteos aislados.
6. **Given** cualquier instante confirmado, **Then** `totalVotos` es igual a la suma de `votos` de sus conteos.
7. **Given** un fallo de base de datos antes del commit, **Then** no queda evento, resumen ni conteo parcial.
8. **Given** un mensaje inválido o un error permanente, **Then** tras reintentos limitados termina en DLQ, sin descarte silencioso ni requeue infinito.
9. **Given** N eventos concurrentes con duplicados, **Then** los totales equivalen a votos lógicos únicos.

### US3 (P2) — Visualizar cambios y recuperarse de desconexiones

Como usuario quiero ver actualizaciones en tiempo real y recuperar el estado vigente al reconectar para no depender de una notificación frágil.

Prueba independiente: dos clientes suscritos a elecciones distintas reciben solo su snapshot; al desconectar y volver, REST entrega la revisión vigente.

Criterios de aceptación:

1. **Given** un evento confirmado, **When** el commit termina, **Then** se publica un snapshot por WebSocket en el destino de su elección.
2. **Given** una transacción revertida, **Then** no se publica snapshot.
3. **Given** dos clientes en elecciones distintas, **Then** no reciben mensajes cruzados.
4. **Given** un fallo al publicar WebSocket después del commit, **Then** los conteos permanecen y el error se registra.
5. **Given** una suscripción y una consulta REST simultáneas, **Then** el cliente descarta revisiones menores y converge a la mayor.
6. **Given** una desconexión y reconexión, **Then** el cliente vuelve a suscribirse y recupera por REST la revisión vigente sin retroceder.
7. **Given** el frontend, **Then** muestra selección/entrada de elección por UUID, total, conteos, estados vacío/cargando/error y estado de conexión.
8. **Given** un cambio de elección o desmontaje, **Then** se cancelan suscripciones y respuestas tardías.

### US4 (P2) — Ejecutar e integrar reproduciblemente

Como equipo quiero levantar el módulo con PostgreSQL, RabbitMQ, gateway y frontend para demostrar el flujo completo y distinguir lo verificado de lo bloqueado.

Prueba independiente: contenedores levantan, fixtures se publican, REST responde y dos clientes ven actualizaciones; reiniciar conserva conteos.

Criterios de aceptación:

1. **Given** `compose.full.yml`, **When** se levanta, **Then** PostgreSQL, RabbitMQ, Resultados, gateway y frontend arrancan con healthchecks y DNS de Compose.
2. **Given** el frontend, **Then** usa `/api` y `/ws` del gateway, no el 8083 directo.
3. **Given** un gateway MVC, **When** se prueba REST y handshake WebSocket por 8080, **Then** el transporte elegido funciona o queda documentada una alternativa mínima compatible.
4. **Given** los fixtures, **Then** son repetibles y no crean endpoints para alterar conteos.
5. **Given** un reinicio de Resultados, **Then** la deduplicación y los conteos persisten.
6. **Given** ausencia del productor real o de Autenticación, **Then** se registra el bloqueo de G4 sin simular integración terminada.

## 4. Requisitos funcionales

| ID | Requisito | US |
|---|---|---|
| FR-001 | Mantener una proyección por elección en `resultado_resumen` y `resultado_conteo` en `resultadosdb`. | US1 |
| FR-002 | Exponer `GET /api/resultados/elecciones/{eleccionId}` de solo lectura. | US1 |
| FR-003 | Responder elección sin proyección con snapshot vacío y `disponible=false`, sin inventar existencia. | US1 |
| FR-004 | Leer resumen y conteos en una lectura consistente. | US1 |
| FR-005 | Consumir `VotoEmitidoEvent` v1 desde `votacion.exchange`/`voto.emitido.v1`. | US2 |
| FR-006 | Validar `eventId`, `schemaVersion=1`, UUIDs y `occurredAt` con zona. | US2 |
| FR-007 | Deduplicar por `eventId` persistido. | US2 |
| FR-008 | Deduplicar por voto lógico `(eleccionId, votoId)` con otro `eventId`. | US2 |
| FR-009 | Detectar y registrar duplicados con payload incompatible sin sumar ni sobrescribir. | US2 |
| FR-010 | Insertar evento procesado, actualizar resumen y conteo en una única transacción. | US2 |
| FR-011 | Usar incremento atómico/bloqueo; soportar creación simultánea del primer resumen/conteo. | US2 |
| FR-012 | Mantener `totalVotos` igual a la suma de conteos por elección. | US2 |
| FR-013 | Incrementar una revisión monotónica por elección en cada evento nuevo. | US2/US3 |
| FR-014 | Confirmar ACK solo después del commit; reintentos limitados y DLQ para errores permanentes. | US2 |
| FR-015 | Publicar snapshot STOMP después del commit, sin revertir conteos por fallo de difusión. | US3 |
| FR-016 | Incluir en el snapshot `eleccionId`, `totalVotos`, `conteos`, `ultimaActualizacion` con zona y `revision`. | US3 |
| FR-017 | Permitir recuperación REST tras desconexión o reinicio. | US3 |
| FR-018 | Implementar cliente subscribe-first + GET y descartar revisiones antiguas. | US3 |
| FR-019 | Mostrar estados vacío/cargando/error/conexión en la feature React. | US3 |
| FR-020 | Enrutar `/api/resultados/**` y `/ws` por gateway MVC sin romper `/api/votos/**`. | US4 |
| FR-021 | Usar `/api` y `/ws` del gateway desde frontend y Vite. | US4 |
| FR-022 | Definir acceso público local explícito; no exponer identidad de votante; no simular JWT. | Todas |
| FR-023 | Configurar perfiles local/contenedor/test, variables y `.env.example` sin secretos reales. | US4 |
| FR-024 | Proveer fixtures repetibles sin endpoints que alteren conteos. | US2/US4 |
| FR-025 | Documentar límites de MVP, decisiones externas y bloqueos con evidencia de puertas. | US4 |

## 5. Requisitos no funcionales y restricciones

- Java 21; Spring Boot 4.0.9 y Spring Cloud 2025.1.4 verificados como combinación compatible (el BOM 2025.1.4 declara Boot 4.0.9; Boot 4.1.2 no fue la combinación validada).
- PostgreSQL real y RabbitMQ real en integración (Testcontainers en pruebas; contenedores en demo).
- Flyway como fuente del esquema; `ddl-auto=validate`; timestamps en UTC y tipos `TIMESTAMPTZ`.
- Sin Redis en Resultados; sin acceso a tablas de otros servicios.
- Un solo broker STOMP simple y una sola instancia de Resultados para el MVP; no se promete difusión horizontal.
- Sin secretos reales ni credenciales de producción por defecto.
- Orígenes CORS/WebSocket restringidos por configuración de entorno.
- No prometer entrega exactamente una vez; la semántica es al-menos-una-vez con deduplicación.

## 6. Contratos

- Evento: `contracts/voto-emitido.v1.schema.json` (propuesto, pendiente de productor real).
- REST: `contracts/resultados.openapi.yaml`.
- WebSocket: `contracts/websocket.md`.
- Modelo de datos: `data-model.md`.
- Plan técnico: `plan.md`.

## 7. Trazabilidad y decisiones abiertas

- Toda tarea en `tasks.md` referencia US/FR.
- Decisiones externas bloqueantes: tipos y semántica final del productor Votación, nombres de exchange/colas, política definitiva de acceso y catálogo de elecciones/candidatos.
- Mientras no exista productor real, el contrato se marca **propuesto** y la integración G4 queda **bloqueada**; el módulo aislado puede cerrar G3.
