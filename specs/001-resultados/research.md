# Research y evidencia — Resultados (T001, T002, T004, T034, T038)

Fecha de inspección: 2026-11-08. Este documento reemplaza la fotografía inicial del informe; todo lo siguiente fue revalidado en este repositorio y en fuentes oficiales.

## 1. Inventario del repositorio (T001)

Estado inicial real de `git status --short`:

```text
?? constitution.md
?? task.md
```

`git log --oneline -10` mostró un único commit: `508f6c0 Initial commit`.

Inspección de archivos:

- `README.md`: solo el título del proyecto; no documenta módulos ni comandos.
- `constitution.md` y `task.md`: documentos de trabajo entregados.
- No existen `pom.xml`, `services/`, `frontend/`, `docker-compose*.yml`, `compose*.yml`, `.env`, `.env.example`, migraciones, entidades ni pruebas.
- El informe previo describía gateway y votacion-service como esqueletos, frontend Vite y RabbitMQ/PostgreSQL configurados. **Eso no se corresponde con el estado actual del repositorio**: no hay código de esos módulos. Se preserva lo existente (README y documentos) y se crea únicamente lo necesario para Resultados y su integración mínima.

Rutas de evidencia creadas por este trabajo:

- `services/resultados-service/**`
- `services/api-gateway/**`
- `frontend/**`
- `compose.full.yml`
- `scripts/resultados/**`
- `specs/001-resultados/**`

No se eliminaron ni revirtieron archivos ajenos. Los cambios compartidos se limitan a gateway mínimo, frontend de Resultados, configuración y contratos.

## 2. Verificación de herramientas (T002)

| Herramienta | Hallazgo inicial | Verificación realizada | Resultado |
|---|---|---|---|
| JDK | PATH apuntaba a Java 8 (`1.8.0_491`) | Se descargó Temurin 21.0.13+9 portátil en el directorio temporal de trabajo | `openjdk 21.0.13` funcional para compilar |
| Maven | No instalado | Se descargó Apache Maven 3.9.11 portátil | `Apache Maven 3.9.11` funcional; se añadirá wrapper al proyecto |
| Node | Node 24.16.0 / npm 11.13.0 | `node -v`, `npm -v` | Compatible con Vite 8 y React 19 |
| Docker | Daemon inicialmente detenido | Se inició Docker Desktop; `docker version` | Docker Server 29.6.2 disponible |
| Red / Maven Central | No verificada en el informe | Consultas HTTPS a `repo.maven.apache.org` | Disponible (HTTP 200) |
| Compilación baseline | No se pudo ejecutar | No existía ningún `pom.xml` ni build frontend | Baseline N/A; no había código que compilar |

Los binarios portátiles usados para validar no se versionan en el repositorio. El proyecto incluye Maven Wrapper para que un clon con JDK 21 no dependa de un Maven global.

## 3. Verificación de versiones reportadas (T002)

Fuente: metadatos y BOM de Maven Central.

- `spring-boot-starter-parent` 4.1.2 existe.
- `spring-cloud-dependencies` 2025.1.4 existe.
- El POM de `spring-cloud-dependencies:2025.1.4` declara `spring-boot.version = 4.0.9` y `spring-cloud-gateway.version = 5.0.4`.
- Conclusión: **Boot 4.1.2 + Cloud 2025.1.4 no es la combinación validada por el BOM**. Se adopta la alternativa compatible **Spring Boot 4.0.9 + Spring Cloud 2025.1.4** para gateway; Resultados usa Boot 4.0.9 sin Cloud. Esto cumple el principio de no copiar versiones a ciegas y registrar una alternativa.
- Jackson gestionado por Boot 4.0.9: **3.1.7** (paquete `tools.jackson`). Spring AMQP 4.0.6 incluye `JacksonJsonMessageConverter` para Jackson 3; se usa ese converter.
- Flyway gestionado: **11.14.1**; se añade `flyway-database-postgresql`.
- Testcontainers gestionado: **2.0.5**, con nuevos artefactos `testcontainers-postgresql`, `testcontainers-rabbitmq`, `testcontainers-junit-jupiter`.
- Gateway MVC 5.0.4 soporta proxy WebSocket si `spring-boot-starter-websocket` está en el classpath (documentación oficial de Spring Cloud Gateway, sección WebSockets). Se usa ese soporte en lugar de cambiar silenciosamente a gateway reactivo.

## 4. Decisiones de dominio (T004)

| Tema | Decisión | Justificación / estado |
|---|---|---|
| Tipos de ID | `UUID` para `eventId`, `votoId`, `eleccionId`, `candidatoId` | Evita colisiones entre servicios y no exige secuencias compartidas. **Provisional** hasta confirmar productor. |
| `schemaVersion` | Entero `1` | Contrato v1 cerrado; una versión distinta se rechaza y va a DLQ. |
| Semántica de voto | Solo voto emitido; cuenta exactamente un voto por voto lógico | Anulaciones, cambios y recálculos quedan fuera de alcance. |
| Idempotencia | `eventId` único y voto lógico `(eleccionId, votoId)` único | Cubre reentrega del mismo mensaje y republicación con otro `eventId`. |
| Duplicado conflictivo | Se registra en `evento_conflicto`, no altera conteos, se confirma el mensaje | Evita bucles por un mensaje que no puede reconciliarse. |
| Elección sin proyección | HTTP 200 con snapshot vacío (`disponible=false`, revisión 0, sin timestamp) | No inventa existencia en Votación; el frontend muestra estado vacío. |
| Elección sin votos | Igual que sin proyección hasta que llegue el primer evento | No se crean proyecciones vacías por consultas. |
| Publicación/acceso | Resultados públicos en perfil local; sin JWT propio | No existe contrato de Autenticación verificado. La integración protegida queda bloqueada, no simulada. |
| Catálogo/nombres | No hay catálogo propio; el cliente introduce UUID de elección y muestra UUID de candidato | No se inventan nombres ni existencia. **Dependencia externa**. |
| `ultimaActualizacion` | `OffsetDateTime`/`TIMESTAMPTZ` en UTC, instante de procesamiento confirmado | El diagrama usaba `Time`; se documenta la mejora. `occurredAt` del evento no sustituye el instante de actualización. |
| Revisión | `BIGINT` monotónica por elección, incrementada en la misma transacción que los conteos | Permite descartar snapshots antiguos en el cliente. |
| Retención | `voto_procesado` se conserva en el MVP para mantener deduplicación | No hay purga automática; se documenta como límite operativo. |
| Nombres RabbitMQ | `votacion.exchange` (topic durable), `voto.emitido.v1`, `resultados.voto-emitido.v1`, `votacion.dlx`, `resultados.voto-emitido.v1.dlq` | **Propuestos**; no hay evidencia del productor real. |

## 5. Contrato del productor y bloqueo G4 (T034)

No existe en el repositorio un productor de Votación, ni esquema, ni evidencia de exchange/cola reales. Por tanto:

- `contracts/voto-emitido.v1.schema.json` queda marcado **PROPUESTO**.
- El consumidor se implementa contra ese contrato y se prueba con fixtures que usan exactamente el mismo esquema.
- La tarea T034 queda **bloqueada** hasta disponer del productor real. Cualquier discrepancia deberá registrarse aquí y en `contracts/`, actualizando consumidor y pruebas.
- No se implementa Votación completo ni se inventan nombres definitivos.
- **Revalidación 2026-11-08:** `services/` solo contiene `api-gateway` y `resultados-service`; no hay `votacion-service`, controladores de voto, entidad `Voto`, `RabbitTemplate` ni productor. T034 se cierra como bloqueo registrado en B001; T035 permanece abierta.

## 6. Límites del MVP (T038)

Revalidado al cierre 2026-11-08: estos límites siguen vigentes y no se presentan como integración terminada.

- Una sola instancia de Resultados y broker STOMP simple; no hay difusión entre instancias.
- Sin autenticación real; acceso público local explícito. La política protegida depende de un proveedor externo.
- Sin catálogo de elecciones/candidatos; se muestran UUIDs y no nombres inventados.
- Sin anulaciones ni cambios de voto.
- Sin purga de deduplicación; el crecimiento de `voto_procesado` es un límite conocido.
- La recuperación tras desconexión depende de REST; un WebSocket perdido no se reenvía.
- El gateway MVC requiere `spring-boot-starter-websocket` para el upgrade; se verificará con smoke real.
- PostgreSQL y RabbitMQ se usan reales en integración; los mocks no acreditan G2/G3/G4.

## 7. Alternativas descartadas

- Gateway reactivo: no hay gateway previo que preservar y MVC 5.0.4 ya soporta WebSocket; cambiar de variante no está justificado.
- Redis: no aporta a los criterios de Resultados y la constitución lo reserva a Votación.
- `ddl-auto=update`: prohibido; Flyway + validate.
- SockJS: se prefiere STOMP sobre WebSocket nativo porque el gateway MVC verificado soporta upgrade y se evita la complejidad de transportes HTTP de respaldo. El contrato documenta esta elección.
