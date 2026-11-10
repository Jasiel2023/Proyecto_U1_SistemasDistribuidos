# Plan de implementacion de votacion-service

## Fase 0: Alineacion tecnica

Confirmar que el servicio usa Java 21, Spring Boot 4, Spring MVC, Spring Data
JPA, Flyway, Spring AMQP, PostgreSQL y Actuator. Mantener REST + RabbitMQ como
decisiones oficiales y eliminar ambiguedades de Kafka, gRPC o ZeroMQ.

Salida: especificaciones aprobadas y configuracion de ambientes definida.

## Fase 1: Persistencia

Disenar el modelo `votaciones`, `opciones` y `votos`. Crear la migracion Flyway
con claves foraneas, indices y la restriccion unica para impedir el doble voto.
Agregar datos de prueba solo si son necesarios para desarrollo.

Salida: esquema reproducible y validado por Hibernate.

## Fase 2: Dominio y aplicacion

Implementar la estructura MVC por capas: entidades, repositorios, servicios de
aplicacion, DTOs, excepciones y mapeadores necesarios. El servicio de
aplicacion sera el unico componente que coordine reglas de negocio,
persistencia y publicacion del evento. Implementar el caso de uso transaccional
de registro.
Validar estado de la votacion, existencia y pertenencia de la opcion, estado de
la opcion y duplicidad del votante.

Salida: caso de uso de registro funcionando sin dependencia HTTP externa.

## Fase 3: API REST

Crear DTOs de entrada y salida, validaciones Bean Validation, controlador
`POST /api/votos` y manejo uniforme de errores. Verificar que el gateway pueda
reenviar la ruta sin modificar el contrato.

Salida: endpoint documentado y probado con respuestas `201`, `400`, `404` y
`409`.

## Fase 3.1: Integracion con frontend/votacion

Consumir el endpoint desde `frontend/src/features/votacion/` usando
`frontend/src/api/client.js`. Implementar la captura de la opcion, envio del
voto, estados de carga y mensajes para exito o errores del backend.

Esta feature no debe conectarse directamente al microservicio, a la base de
datos ni a RabbitMQ. La comunicacion debe pasar por el API Gateway.

Salida: el usuario puede enviar un voto desde la interfaz y recibe una
respuesta coherente con el contrato REST.

## Fase 4: Integracion con RabbitMQ

Declarar exchange, cola, binding y productor JSON. Publicar
`VotoEmitidoEvent` despues de una persistencia exitosa y agregar metadatos de
trazabilidad.

Salida: un voto valido genera exactamente un evento observable por la cola.

## Fase 5: Contenedorizacion

Crear el `Dockerfile` del servicio y completar el Compose local. Separar los
valores de ejecucion local de los valores internos de Docker. Agregar health
checks y dependencias de arranque para PostgreSQL y RabbitMQ.

Salida: el servicio puede iniciar con la infraestructura compartida usando
Docker Compose.

## Fase 6: Pruebas y endurecimiento

Agregar pruebas unitarias, de repositorio, de controlador, de integracion con
PostgreSQL/RabbitMQ y de concurrencia. Revisar logs, errores, validaciones,
idempotencia y configuracion de Actuator.

Salida: criterios de aceptacion cumplidos y evidencia de ejecucion de pruebas.

## Orden de entrega

1. Migracion y modelo de persistencia.
2. Caso de uso transaccional.
3. API REST.
4. Integracion con `features/votacion`.
5. Productor RabbitMQ.
6. Pruebas de concurrencia e integracion.
7. Dockerfile y Compose.

No se implementara WebSocket en este servicio. Esa responsabilidad pertenece a
`resultados-service`.
