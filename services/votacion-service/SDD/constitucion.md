# Constitucion de votacion-service

## Proposito

Este documento establece las reglas obligatorias para disenar, implementar,
probar y desplegar `votacion-service`. El servicio registra votos validos y
publica eventos para que otros componentes procesen los resultados sin acceder
directamente a su base de datos.

El alcance de trabajo incluye tambien la integracion funcional con
`frontend/src/features/votacion/`. El backend y esta feature representan el
mismo dominio, pero mantienen responsabilidades separadas.

## Principios

### 1. Responsabilidad unica del dominio

`votacion-service` es propietario de los votos, las elecciones y los candidatos
de votacion que defina su modelo. Ningun otro microservicio puede leer o
modificar directamente `votaciondb`.

### 2. REST como entrada sincrona

La entrada publica del servicio sera HTTP REST. El endpoint principal sera
`POST /api/votos`, consumido normalmente a traves de `api-gateway`.

No se implementaran llamadas REST directas desde `votacion-service` hacia otros
microservicios para validar o registrar un voto.

El frontend no llamara directamente a `votacion-service`. Consumira el API
Gateway usando el cliente compartido `frontend/src/api/client.js`.

### 3. Arquitectura MVC por capas

El servicio usara una variante de Modelo-Vista-Controlador adaptada a una API
REST:

- **Modelo:** entidades JPA, value objects y reglas propias del dominio.
- **Vista:** representaciones JSON mediante DTOs de respuesta y errores REST.
- **Controlador:** endpoints HTTP, validacion de entrada y mapeo de respuestas.
- **Service:** casos de uso, reglas de negocio y limites transaccionales.
- **Repository:** persistencia y consultas mediante Spring Data JPA.
- **Messaging:** publicacion de eventos RabbitMQ fuera del controlador.

Los controladores no contendran logica de negocio ni accederan directamente a
los repositorios. Los servicios de aplicacion coordinaran repositorios,
validaciones y publicacion de eventos.

### 4. RabbitMQ como salida asincrona

Despues de registrar correctamente un voto, el servicio publicara el evento
`VotoEmitidoEvent` mediante RabbitMQ.

La configuracion inicial usara:

- Exchange: `votacion.exchange`
- Routing key: `voto.emitido`
- Cola de consumo: `votos.procesados`

Los nombres deben centralizarse en configuracion y no repetirse como literales
en varias clases.

### 5. Un votante solo puede votar una vez

La regla de voto unico debe estar protegida por la base de datos, no solamente
por una consulta previa en Java. La combinacion `(eleccion_id, id_usuario)`
debe tener una restriccion unica.

La logica de aplicacion debe traducir un intento duplicado a una respuesta
controlada, sin exponer el error interno de PostgreSQL.

### 6. Consistencia transaccional

El registro del voto y sus validaciones se ejecutaran dentro de una transaccion.
Solo se emitira `VotoEmitidoEvent` cuando el registro haya sido confirmado
correctamente.

Las operaciones deben ser idempotentes frente a reintentos del cliente. Un
reintento del mismo usuario para la misma votacion no puede crear otro voto.

### 7. Migraciones como fuente del esquema

Todas las tablas, indices, restricciones y datos iniciales necesarios se
definiran mediante Flyway en `src/main/resources/db/migration`.

Hibernate no podra crear ni alterar el esquema en ejecucion; se mantendra
`spring.jpa.hibernate.ddl-auto=validate`.

### 8. Contratos explicitos

Los DTO REST y el evento RabbitMQ deben tener nombres, tipos y reglas de
validacion documentados. No se expondran directamente entidades JPA como
contratos de la API.

### 9. Calidad verificable

Cada regla critica tendra pruebas automatizadas. Como minimo se verificaran:

- voto valido;
- datos invalidos;
- voto duplicado;
- concurrencia de dos solicitudes para el mismo votante;
- publicacion del evento;
- migracion y arranque del contexto con PostgreSQL.

### 10. Configuracion por ambiente

Credenciales, hosts y puertos se recibiran por variables de entorno o valores
de configuracion. En Docker se usaran nombres de servicio, por ejemplo
`postgres-votacion` y `rabbitmq`; `localhost` se reservara para ejecucion
local fuera de contenedores.

### 11. Observabilidad y seguridad basicas

Se conservara Actuator para salud del servicio. Los logs no deben incluir
credenciales ni datos innecesarios del votante. Los errores REST tendran
respuestas estables y no mostraran stack traces al cliente.

### 12. Responsabilidad del frontend de votacion

`frontend/src/features/votacion/` sera responsable de:

- capturar la seleccion del votante;
- validar formularios basicos antes de enviar;
- consumir `POST /api/votos` mediante el cliente compartido;
- mostrar estados de carga, exito y error;
- no duplicar reglas de negocio del backend;
- no acceder directamente a PostgreSQL, RabbitMQ u otros microservicios.

Los resultados en vivo y la conexion WebSocket pertenecen a
`features/resultados/`, no a `features/votacion/`.

## Regla de cambios

Un cambio que modifique el endpoint, el esquema, el evento o una regla de voto
debe actualizar primero `spec.md`, despues `plan.md` y `taks.md` antes de
implementarse. Las excepciones deben quedar justificadas en el mismo cambio.
