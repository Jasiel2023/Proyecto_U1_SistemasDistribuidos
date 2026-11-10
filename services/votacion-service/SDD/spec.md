# Especificaciones de votacion-service

## 1. Alcance

El servicio recibe un voto por REST, valida que la votacion y la opcion sean
validas, registra el sufragio en `votaciondb`, evita el doble voto y publica un
evento para el procesamiento de resultados.

Fuera del alcance de esta primera version quedan autenticacion, administracion
de usuarios, conteo final y WebSockets. La identidad del votante llega como
`usuarioId` y sera validada por el servicio de autenticacion cuando ese modulo
este disponible.

El trabajo se divide en dos partes coordinadas:

- **Backend:** `services/votacion-service`, responsable de validar, persistir y
  publicar el voto.
- **Frontend:** `frontend/src/features/votacion/`, responsable de la pantalla y
  del consumo del endpoint a traves del API Gateway.

La feature frontend no implementa persistencia, acceso a RabbitMQ ni reglas de
unicidad. Solo presenta el estado que devuelve el backend. La visualizacion de
conteos en vivo pertenece a `frontend/src/features/resultados/`.

## 2. Modelo de dominio minimo

### Votacion

- `id`: identificador unico.
- `nombre`: nombre visible de la votacion.
- `estado`: `ABIERTA` o `CERRADA`.
- `fecha_inicio` y `fecha_fin`: periodo opcional de validez.

### Opcion

- `id`: identificador unico.
- `votacion_id`: votacion propietaria.
- `nombre`: nombre de la opcion o candidato.
- `activa`: indica si puede recibir votos.

### Voto

- `id`: identificador unico.
- `votacion_id`: votacion a la que pertenece.
- `opcion_id`: opcion seleccionada.
- `usuario_id`: identificador del votante.
- `emitido_en`: fecha y hora UTC de registro.

## 3. Estructura MVC y capas

La implementacion seguira esta estructura de paquetes bajo
`ec.unl.votacion.votacion_service`:

```text
controller/
service/
repository/
entity/
dto/
messaging/
exception/
config/
```

Responsabilidades:

- `controller`: recibe requests HTTP, aplica validaciones de entrada y delega
  al servicio de aplicacion.
- `service`: implementa el caso de uso `registrarVoto`, las reglas de negocio
  y `@Transactional`.
- `repository`: encapsula consultas y operaciones de persistencia.
- `entity`: contiene el modelo JPA interno.
- `dto`: contiene los contratos de entrada y salida de la API.
- `messaging`: contiene eventos, configuracion RabbitMQ y productores.
- `exception`: contiene excepciones de dominio y el manejador global REST.
- `config`: contiene configuracion tecnica, como beans de RabbitMQ.

Reglas de dependencia:

- `controller` depende de `service` y `dto`.
- `service` depende de `repository`, `entity`, `dto` y `messaging`.
- `repository` depende de `entity`.
- `messaging` no depende de `controller`.
- Ninguna entidad JPA se devuelve directamente desde un controlador.

Restricciones obligatorias del dominio:

- una opcion debe pertenecer a la votacion indicada;
- solo se vota en una votacion `ABIERTA`;
- solo se vota por una opcion activa;
- no puede existir mas de un voto para `(votacion_id, usuario_id)`;
- `usuario_id`, `votacion_id` y `opcion_id` son obligatorios y positivos.

## 4. API REST

### Crear voto

`POST /api/votos`

El frontend consumira esta ruta como `/api/votos` mediante el cliente comun.
En desarrollo, Vite la reenviara al API Gateway y el Gateway la dirigira a
`votacion-service`.

Request:

```json
{
  "votacionId": 1,
  "opcionId": 2,
  "usuarioId": 15
}
```

Respuesta exitosa: `201 Created`

```json
{
  "id": 100,
  "votacionId": 1,
  "opcionId": 2,
  "usuarioId": 15,
  "emitidoEn": "2026-11-09T20:00:00Z",
  "mensaje": "Voto registrado correctamente"
}
```

Errores esperados:

- `400 Bad Request`: campos ausentes, cero, negativos o formato invalido.
- `404 Not Found`: votacion u opcion inexistente.
- `409 Conflict`: votacion cerrada, opcion inactiva, opcion de otra votacion
  o usuario que ya voto.
- `503 Service Unavailable`: no se puede completar una dependencia tecnica
  necesaria.

La respuesta de error debe seguir un formato uniforme con `status`, `code`,
`message` y `timestamp`. No debe incluir stack traces.

## 5. Evento RabbitMQ

El evento se publicara solo despues de guardar el voto exitosamente.

Nombre: `VotoEmitidoEvent`

```json
{
  "eventoId": "uuid",
  "votoId": 100,
  "votacionId": 1,
  "opcionId": 2,
  "usuarioId": 15,
  "emitidoEn": "2026-11-09T20:00:00Z"
}
```

Propiedades del mensaje:

- `contentType`: `application/json`;
- `eventId`: identificador unico para trazabilidad e idempotencia del
  consumidor;
- `deliveryMode`: persistente cuando la configuracion de RabbitMQ lo permita.

El productor no debe depender de la implementacion de `resultados-service`.
El consumidor sera responsable de procesar el evento de forma idempotente.

## 6. Persistencia y transacciones

Las tablas se crearan con Flyway. JPA debe validar el esquema. La operacion de
registro se ejecutara en una transaccion y usara la restriccion unica como
ultima garantia ante solicitudes concurrentes.

La primera migracion debe crear las tablas, claves foraneas, indices y la
restriccion unica del voto. No se usaran tablas compartidas con otros
microservicios.

## 7. Configuracion

Variables esperadas:

- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` y
  `POSTGRES_PASSWORD`;
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` y `RABBITMQ_PASS`.

Valores para ejecucion local pueden apuntar a `localhost`. El perfil de Docker
debe apuntar a los nombres de servicio de Compose.

## 8. Criterios de aceptacion

- Un voto valido se persiste una sola vez y devuelve `201`.
- Un segundo voto del mismo usuario en la misma votacion devuelve `409`.
- Dos solicitudes concurrentes para el mismo usuario producen como maximo un
  registro.
- No se aceptan opciones que pertenezcan a otra votacion.
- Un voto exitoso produce un `VotoEmitidoEvent` con los datos correctos.
- No se publica evento cuando la transaccion de persistencia falla.
- El servicio arranca con PostgreSQL y RabbitMQ configurados por Compose.
- Las pruebas automatizadas cubren los criterios anteriores.
- `features/votacion` puede enviar un voto valido mediante el API Gateway.
- `features/votacion` muestra correctamente respuestas `201`, `400`, `404` y
  `409` sin duplicar la logica del backend.
