# Tareas de votacion-service

Las tareas deben ejecutarse en orden. No se inicia una fase mientras su criterio
de salida no este verificado. Si una decision modifica el contrato, el esquema,
el evento o una regla de negocio, primero se actualizan `spec.md` y `plan.md`.

## Fase 0: Alineacion tecnica

### Objetivo

Dejar cerradas las decisiones tecnicas antes de crear codigo de negocio.

### Tareas

- [x] Confirmar Java 21 y Spring Boot 4.1.2.
- [x] Confirmar Spring MVC como tecnologia REST.
- [x] Confirmar Spring Data JPA para persistencia.
- [x] Confirmar Flyway como unica herramienta de migraciones.
- [x] Confirmar Spring AMQP y RabbitMQ para eventos.
- [x] Confirmar PostgreSQL 16 como base de datos.
- [x] Confirmar que WebSockets pertenecen a `resultados-service`, no a este
  servicio.
- [x] Confirmar el contrato `POST /api/votos`.
- [x] Confirmar los estados `ABIERTA` y `CERRADA`.
- [x] Confirmar los nombres `votacion.exchange`, `votos.procesados` y
  `voto.emitido`.
- [x] Definir variables de entorno para ejecucion local y Docker.
- [x] Eliminar del diseno cualquier dependencia de Kafka, gRPC o ZeroMQ.

### Criterio de salida

- [x] `constitucion.md`, `spec.md` y `plan.md` no contienen decisiones
  contradictorias.
- [x] El proyecto compila con `./mvnw -DskipTests package`.

## Fase 1: Persistencia y migraciones

### Objetivo

Crear un esquema reproducible que proteja el voto unico desde PostgreSQL.

### Tareas

- [x] Crear `src/main/resources/db/migration/V1__create_votacion_tables.sql`.
- [x] Crear tabla `elecciones` con titulo, fecha, horario y activacion.
- [x] Crear tabla `candidatos` relacionada con `elecciones`.
- [x] Crear tabla `votos` relacionada con `elecciones` y `candidatos`.
- [x] Definir tipos, longitudes, valores no nulos y claves primarias.
- [x] Agregar claves foraneas entre las tablas.
- [x] Agregar restriccion unica `(eleccion_id, id_usuario)`.
- [x] Agregar indices para consultas por votacion, opcion y usuario.
- [x] Definir fechas almacenadas en UTC.
- [x] Verificar que no se requieren datos iniciales para desarrollo.
- [x] Configurar `ddl-auto=validate`.
- [x] Verificar que no existan tablas creadas manualmente fuera de Flyway.

### Criterio de salida

- [x] Flyway ejecuta `V1` desde una base vacia sin errores.
- [x] La migracion rechaza un voto duplicado mediante la restriccion unica.
- [x] Hibernate valida el esquema sin errores.
- [x] El modelo de base de datos coincide con `spec.md`.

## Fase 2: Dominio y aplicacion

### Objetivo

Implementar la logica de negocio sin mezclarla con HTTP ni con detalles de
infraestructura.

### Tareas

- [x] Crear paquetes `controller`, `service`, `repository`, `entity`, `dto`,
  `messaging`, `exception` y `config`.
- [x] Crear entidades JPA para `Eleccion`, `Candidato` y `Voto`.
- [x] Mapear correctamente tablas, relaciones, indices y restricciones.
- [x] Crear repositorios para votaciones, opciones y votos.
- [x] Crear `CrearVotoRequest`.
- [x] Crear `VotoResponse`.
- [x] Crear excepciones de dominio para votacion cerrada, opcion invalida y
  voto duplicado.
- [x] Crear el servicio de aplicacion para `registrarVoto`.
- [x] Aplicar `@Transactional` en el servicio de aplicacion.
- [x] Validar que los identificadores sean obligatorios y positivos.
- [x] Validar que la votacion exista y este `ABIERTA`.
- [x] Validar que la opcion exista, este activa y pertenezca a la votacion.
- [x] Persistir el voto usando la restriccion unica como garantia final.
- [x] Traducir una violacion de unicidad a la excepcion de voto duplicado.
- [x] Mantener toda la logica de negocio fuera de los controladores.

### Criterio de salida

- [x] El caso de uso puede probarse sin levantar un servidor HTTP.
- [x] El servicio no realiza llamadas REST a otros microservicios.
- [x] Las reglas del dominio estan cubiertas por pruebas unitarias.
- [x] El servicio de aplicacion no expone entidades JPA como contrato externo.

## Fase 3: API REST

### Objetivo

Exponer el caso de uso con un contrato HTTP estable y errores controlados.

### Tareas

- [x] Crear el controlador para `POST /api/votos`.
- [x] Recibir `CrearVotoRequest` con `@Valid`.
- [x] Delegar toda la operacion al servicio de aplicacion.
- [x] Mapear el resultado a `VotoResponse`.
- [x] Responder `201 Created` para un voto registrado.
- [x] Crear manejador global de excepciones.
- [x] Responder `400` para datos invalidos.
- [x] Responder `404` para votacion u opcion inexistente.
- [x] Responder `409` para conflictos de negocio o voto duplicado.
- [x] Responder `503` para indisponibilidad tecnica cuando corresponda.
- [x] Usar respuesta de error uniforme con `status`, `code`, `message` y
  `timestamp`.
- [x] Evitar stack traces y detalles internos en las respuestas.
- [x] Verificar compatibilidad con la ruta configurada en `api-gateway`.

### Criterio de salida

- [x] El endpoint cumple exactamente el contrato de `spec.md`.
- [x] Las pruebas MVC cubren respuestas exitosas y errores.
- [x] El controlador no contiene consultas ni reglas de negocio.

## Fase 3.1: Integracion con frontend/votacion

### Objetivo

Conectar la interfaz de votacion con el contrato REST sin acoplarla a la
implementacion interna del microservicio.

### Tareas

- [x] Trabajar dentro de `frontend/src/features/votacion/`.
- [x] Consumir el cliente compartido `frontend/src/api/client.js`.
- [x] Implementar la captura de `eleccionId`, `candidatoId` e `idUsuario`.
- [x] Enviar `POST /api/votos` mediante el API Gateway.
- [x] Mostrar estado de envio y evitar doble envio accidental en la interfaz.
- [x] Mostrar confirmacion para respuesta `201`.
- [x] Mostrar mensajes para respuestas `400`, `404` y `409`.
- [x] No acceder directamente a `votacion-service` desde el navegador.
- [x] No implementar en esta feature WebSockets ni conteo de resultados.
- [x] No duplicar en React la validacion de voto unico del backend.

### Criterio de salida

- [x] La feature envia un voto valido usando solo el contrato REST.
- [x] La interfaz representa correctamente exito, carga y errores.
- [x] La prueba manual funciona pasando por el API Gateway.
- [x] No se modifican carpetas de otras features.

## Fase 4: Integracion con RabbitMQ

### Objetivo

Publicar un evento confiable despues de registrar un voto valido.

### Tareas

- [x] Crear la configuracion de `votacion.exchange`.
- [x] Declarar la cola `votos.procesados`.
- [x] Declarar el binding con `voto.emitido`.
- [x] Configurar mensajes JSON.
- [x] Configurar mensajes persistentes cuando corresponda.
- [x] Crear `VotoEmitidoEvent`.
- [x] Incluir `eventoId` unico en cada evento.
- [x] Incluir `votoId`, `eleccionId`, `candidatoId`, `idUsuario` y `emitidoEn`.
- [x] Crear el productor RabbitMQ dentro de `messaging`.
- [x] Publicar el evento solo despues de confirmar el registro del voto.
- [x] Evitar que el productor dependa de `resultados-service`.
- [x] Registrar errores de publicacion sin filtrar credenciales.

### Criterio de salida

- [x] Un voto valido produce un unico evento con el formato especificado.
- [x] Un voto rechazado no produce evento.
- [x] El evento puede ser consumido como JSON por otro microservicio.
- [x] Las pruebas verifican exchange, routing key y contenido del evento.

## Fase 5: Contenedorizacion e infraestructura

### Objetivo

Permitir que el servicio funcione de forma reproducible junto a PostgreSQL y
RabbitMQ mediante Docker Compose.

### Tareas

- [x] Crear `services/votacion-service/Dockerfile`.
- [x] Configurar la construccion del JAR mediante Maven Wrapper o una imagen
  multi-stage.
- [x] Configurar `POSTGRES_HOST=postgres-votacion` dentro de Docker.
- [x] Configurar `RABBITMQ_HOST=rabbitmq` dentro de Docker.
- [x] Separar configuracion local de configuracion de contenedor.
- [x] Agregar el servicio `votacion-service` al Compose correspondiente.
- [x] Configurar dependencias de arranque mediante health checks.
- [x] Exponer el puerto `8082` solo donde sea necesario.
- [x] Verificar que las credenciales provengan del entorno y no del codigo.
- [x] Agregar el servicio a `compose.full.yml` cuando el Dockerfile este listo.

### Criterio de salida

- [ ] PostgreSQL, RabbitMQ y `votacion-service` levantan sin configuracion
  manual adicional.
- [x] Flyway ejecuta las migraciones al iniciar el contenedor.
- [x] El servicio puede conectarse a PostgreSQL y RabbitMQ usando nombres de
  servicio, no `localhost`.
- [ ] El endpoint responde desde el API Gateway.

## Fase 6: Pruebas y endurecimiento

### Objetivo

Validar las reglas funcionales, la concurrencia y la operacion completa.

### Tareas

- [x] Probar registro de voto valido.
- [x] Probar request invalido.
- [x] Probar votacion inexistente.
- [x] Probar opcion inexistente.
- [x] Probar opcion perteneciente a otra votacion.
- [x] Probar opcion inactiva.
- [x] Probar votacion cerrada.
- [x] Probar voto duplicado secuencial.
- [x] Probar dos votos concurrentes para el mismo usuario.
- [x] Confirmar que la concurrencia produzca como maximo un registro.
- [x] Confirmar que no se publique evento si falla la persistencia.
- [x] Confirmar que el evento contenga datos correctos.
- [x] Probar salud mediante Actuator.
- [x] Revisar logs y eliminar credenciales o datos sensibles.
- [x] Ejecutar `./mvnw test` con PostgreSQL y RabbitMQ disponibles.
- [x] Ejecutar pruebas de integracion mediante Docker Compose.
- [x] Registrar evidencia de cada criterio de aceptacion.

### Criterio de salida

- [x] Todos los criterios de aceptacion de `spec.md` estan verificados.
- [x] `./mvnw test` termina correctamente.
- [x] El flujo REST + PostgreSQL + RabbitMQ funciona de extremo a extremo.
- [x] No quedan tareas bloqueantes de fases anteriores.
