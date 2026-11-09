# Tasks — Resultados (001)

Fuente única de progreso. `task.md` es la guía de entrada y no duplica estados. Formato: `- [ ] Tnnn [USn] acción + ruta + dependencia + criterio de aceptación`. `[P]` solo para tareas realmente independientes.

## Fase 0 — Descubrimiento y especificación

- [x] T001 Inspeccionar git status, instrucciones, POM, compose, gateway y frontend; registrar estado y evidencia en `specs/001-resultados/research.md`. Dep.: ninguna. Aceptación: cambios ajenos preservados e inventario actualizado.
- [x] T002 Verificar JDK 21, Maven, Node, Docker y versiones declaradas; intentar compilación baseline y registrar comandos/fallos en `research.md`. Dep.: T001. Aceptación: stack viable o alternativa mínima justificada; ninguna versión inventada. FR-025.
- [x] T003 Crear `specs/001-resultados/spec.md` con US1–US4, requisitos FR y criterios Given/When/Then. Dep.: T001. Aceptación: alcance y exclusiones explícitos.
- [x] T004 Resolver en `research.md` tipos de ID, semántica de voto, publicación/acceso, ausencia de catálogo y nombres RabbitMQ. Dep.: T003. Aceptación: decisiones provisionales y bloqueos externos listados.
- [x] T005 Crear `contracts/voto-emitido.v1.schema.json` y ejemplos válidos/inválidos; fijar exchange/cola/DLQ propuestos. Dep.: T004. Aceptación: contrato marcado propuesto o confirmado. FR-005–FR-009.
- [x] T006 Crear `contracts/resultados.openapi.yaml` y `contracts/websocket.md`. Dep.: T004. Aceptación: contratos consumibles con ejemplos y transporte documentado. FR-002, FR-016–FR-018.
- [x] T007 Crear `data-model.md` con tablas, unicidad, BIGINT, timestamps, revisión, transacciones y retención. Dep.: T005–T006. Aceptación: invariantes y flujo transaccional definidos. FR-001, FR-007–FR-013.
- [x] T008 Crear `plan.md` y este `tasks.md` con T001–T040, dependencias y correspondencia US/FR. Dep.: T002, T005–T007. Aceptación: plan concreto y trazabilidad completa.
- [x] T009 Analizar coherencia constitución/spec/plan/contratos/tasks; registrar G0/G1 en `validation.md`. Dep.: T008. Aceptación: sin bloqueo interno de diseño; dependencias externas aisladas. FR-025.

## Fase 1 — Fundaciones

- [x] T010 Crear `services/resultados-service/pom.xml`, wrapper Maven y `ResultadosServiceApplication.java`. Dep.: T009. Aceptación: compila con JDK 21. FR-001.
- [x] T011 Configurar `application.yml` y perfiles local/test/contenedor; crear `.env.example` sin secretos. Dep.: T010. Aceptación: propiedades y puertos documentados; sin credenciales de producción. FR-023.
- [x] T012 Crear `services/resultados-service/docker-compose.yml` para PostgreSQL propio con volumen y healthcheck; no duplicar RabbitMQ en `compose.full.yml`. Dep.: T011. Aceptación: `docker compose config` válido y base accesible. FR-023.
- [x] T013 Crear `V1__resultados.sql`, entidades y repositorios. Dep.: T012. Aceptación: Flyway arranca desde base vacía y Hibernate validate pasa tras reinicio. FR-001, FR-007.

## Fase 2 — US1: consultas persistentes

- [x] T014 [US1] Crear pruebas de contrato/repositorio para snapshot, conteos, ausencia de proyección, validación y aislamiento. Dep.: T013. Aceptación: pruebas detectan la funcionalidad pendiente. FR-002–FR-004.
- [x] T015 [US1] Implementar DTO y servicio de consulta con lectura consistente. Dep.: T014. Aceptación: cumple OpenAPI y conserva revisión/timestamp. FR-002–FR-004.
- [x] T016 [US1] Implementar controlador GET y tratamiento uniforme de errores. Dep.: T015. Aceptación: T014 pasa; no se exponen entidades ni identidad de votantes. FR-002, FR-003, FR-022.

## Fase 3 — US2: eventos y conteo fiable

- [x] T017 [US2] Crear pruebas de agregación PostgreSQL: nuevo, mismo eventId, mismo voto, payload incompatible, dos elecciones y concurrencia. Dep.: T016, T005. Aceptación: escenarios trazables a `spec.md`. FR-005–FR-013.
- [x] T018 [US2] Implementar validación y servicio transaccional de deduplicación/incremento con upsert atómico. Dep.: T017. Aceptación: total igual a suma, duplicados sin incremento, conflictos sin corrupción. FR-005–FR-013.
- [x] T019 [US2] Declarar topología durable, converter JSON y consumidor; ACK tras commit, retry limitado y DLQ. Dep.: T018. Aceptación: evento válido persistido antes del ACK. FR-005, FR-014.
- [x] T020 [US2] Probar con RabbitMQ/PostgreSQL reales: reentrega, rollback, DLQ, consumidores concurrentes y aislamiento. Dep.: T019. Aceptación: conteos exactos, sin descarte silencioso, pruebas reproducibles. FR-010–FR-014.
- [x] T021 [US2] Crear herramienta de fixtures en `scripts/resultados/` aislada de producción y con el esquema real. Dep.: T020. Aceptación: demo repetible sin endpoint de escritura. FR-024.

## Fase 4 — US3: WebSocket e interfaz

- [x] T022 [US3] Crear pruebas de notificación tras commit, ausencia tras rollback y aislamiento de destinos. Dep.: T020, T006. Aceptación: pruebas fallan sin la implementación. FR-015–FR-017.
- [x] T023 [US3] Implementar configuración STOMP y publicación de snapshot tras commit; aislar fallos de difusión. Dep.: T022. Aceptación: T022 pasa; no se emiten estados sin confirmar. FR-015, FR-016.
- [x] T024 [US3] Probar handshake, suscripción, reconexión y recuperación REST con cliente real. Dep.: T023. Aceptación: evidencia G2 sin afirmar integración con Votación. FR-015–FR-018.
- [x] T025 [US3] Implementar `frontend/src/features/resultados/` con entrada de elección, totales, conteos y estados. Dep.: T016, T006. Aceptación: no inventa catálogo ni nombres. FR-019, FR-025.
- [x] T026 [US3] Crear cliente STOMP y sincronización subscribe + GET; descartar snapshots antiguos, reconectar y limpiar. Dep.: T024–T025. Aceptación: sin carrera ni suscripciones duplicadas. FR-017–FR-019.
- [x] T027 [US3] Añadir pruebas frontend de reconexión, revisiones desordenadas y cambio de elección; ejecutar build. Dep.: T026. Aceptación: visualización converge con REST tras desconexión. FR-018, FR-019.

## Fase 5 — US4: integración y operación

- [x] T028 [US4] Agregar ruta `/api/resultados/**` en `services/api-gateway/src/main/resources/application.yml`, conservando rutas previas si existen. Dep.: T016. Aceptación: consultas atraviesan gateway sin reescritura incorrecta. FR-020.
- [x] T029 [US4] Verificar soporte WebSocket del gateway MVC; implementar `/ws` y probar handshake/tráfico por 8080. Dep.: T024, T028. Aceptación: transporte verificado o alternativa mínima documentada; sin cambio silencioso a reactivo. FR-020.
- [x] T030 [US4] Corregir `frontend/vite.config.js` para `/api` y `/ws` hacia gateway. Dep.: T029. Aceptación: el frontend deja de evitar el gateway. FR-021.
- [x] T031 [US4] Aplicar política de acceso: perfil local explícito, orígenes restringidos y bloqueo registrado de integración protegida. Dep.: T030. Aceptación: sin simular autenticación terminada. FR-022.
- [x] T032 [US4] Crear Dockerfile de Resultados y entradas en `compose.full.yml`; configurar gateway/frontend mínimos. Dep.: T030. Aceptación: build reproducible, DNS, healthchecks y arranque. FR-023.
- [x] T033 [US4] Ejecutar smoke de contenedores y verificar consulta + dos clientes. Dep.: T032, T027. Aceptación: instrucciones limpias, reinicio conserva conteos, G3 con evidencia. FR-020–FR-025.
- [x] T034 [US4] Comparar contrato provisional con productor real y registrar discrepancias. Dep.: T005, T020. Aceptación: contrato actualizado o bloqueo registrado; no se implementa Votación. FR-005. **Cierre 2026-11-08:** no existe `votacion-service` ni productor en el repositorio; B001 registrado. Contrato v1 permanece PROPUESTO.
- [ ] T035 [US4] Validar voto real → evento → conteo → REST → WebSocket → interfaz con productor disponible. Dep.: T031, T033–T034. Aceptación: evidencia G4 o tarea pendiente por dependencia. FR-025. **BLOQUEADA:** sin productor real (B001) ni proveedor de Autenticación (B002); no se declara G4.

## Fase 6 — Cierre y entrega

- [x] T036 Consolidar pruebas de aceptación, compilación backend, build frontend y Compose. Dep.: T033 y tareas integradas disponibles. Aceptación: resultados y bloqueos distinguidos en `validation.md`. **Evidencia 2026-11-08:** `mvnw -B test` 28/0, `mvnw -B -DskipTests package` OK en Resultados y gateway, `npm test` 6/0, `npm run build` OK, `docker compose config --quiet` OK.
- [x] T037 Crear `quickstart.md` con comandos PowerShell y Docker, variables, puertos, consultas, fixtures, DLQ y troubleshooting; actualizar README sin exagerar alcance. Dep.: T033. **Evidencia:** `specs/001-resultados/quickstart.md` y `README.md` actualizados; alcance aislado y G4 bloqueado declarados.
- [x] T038 Documentar límites del MVP en `research.md`. Dep.: T036. Aceptación: una instancia, productor/catálogo externos, seguridad pendiente y recuperación REST. **Evidencia:** `research.md` §6, revalidado en el cierre 2026-11-08.
- [x] T039 Revisar diff y cumplimiento constitucional; comprobar trazabilidad FR→US→T→prueba. Dep.: T036–T038. Aceptación: sin secretos, sin acceso a bases ajenas, sin mocks activos por defecto, sin tareas marcadas sin evidencia. **Evidencia:** revisión en `validation.md`; corregidas inconsistencias de `contracts/websocket.md`; sin secretos reales, sin acceso a bases ajenas ni Redis, `ddl-auto=validate`.
- [x] T040 Entregar resumen de archivos/cambios, comandos, resultados, puertas y bloqueos. Dep.: T039. Aceptación: declarar “Resultados integrado completo” solo con T035; si no, “Resultados aislado completo; integración pendiente” solo con G3 probado. **Cierre:** “Resultados aislado completo; integración pendiente”; G4 bloqueado por B001/B002/B003.

## Correspondencia US/FR

| US | FR principales | Tareas |
|---|---|---|
| US1 | FR-001–FR-004, FR-022 | T014–T016, T010–T013 |
| US2 | FR-005–FR-014, FR-024 | T017–T021 |
| US3 | FR-015–FR-019 | T022–T027 |
| US4 | FR-020–FR-025 | T028–T035 |
| Transversal | FR-022, FR-025 | T001–T009, T036–T040 |
