# autenticacion-service

Microservicio de registro, login y gestion de sesiones con JWT del sistema de votacion.

- **Java 21 / Spring Boot 4.1.2**
- **PostgreSQL 16** (`autenticaciondb`, puerto host `5433`)
- **Flyway** para migraciones (`src/main/resources/db/migration`)
- **Puerto:** `8081`
- **Ruta externa via gateway:** `/api/v1/auth/**`

## Ejecutar solo este servicio

Desde la raiz del repositorio:

```bash
docker compose -f docker-compose.yml \
               -f services/autenticacion-service/docker-compose.yml up -d --build
```

O desde el IDE:

```bash
cd services/autenticacion-service
./mvnw spring-boot:run
```

Requiere un PostgreSQL local en `localhost:5433` con base `autenticaciondb`, o bien definir:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/autenticaciondb
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin
```

## Variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/autenticaciondb` | URL JDBC |
| `POSTGRES_USER` | `admin` | Usuario de la base |
| `POSTGRES_PASSWORD` | `admin` | Clave de la base |
| `JWT_SECRET` | secreto de desarrollo | Secreto HMAC (minimo 32 caracteres) |
| `JWT_EXPIRATION_MS` | `3600000` | Duracion del token |
| `APP_ADMIN_ENABLED` | `true` | Crea la cuenta inicial si no existe |
| `APP_ADMIN_USERNAME` | `admin` | Usuario inicial |
| `APP_ADMIN_PASSWORD` | `admin123` | Clave inicial |

## Endpoints

| Metodo | Ruta | Auth | Descripcion |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | No | Registra una cuenta |
| `POST` | `/api/v1/auth/login` | No | Autentica y devuelve el JWT |
| `POST` | `/api/v1/auth/validate` | Bearer | Valida el token |
| `GET` | `/api/v1/auth/me` | Bearer | Usuario de la sesion actual |
| `POST` | `/api/v1/auth/logout` | Bearer | Invalida el token |

### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"admin","clave":"admin123"}'
```

### Validar sesion

```bash
curl -X POST http://localhost:8081/api/v1/auth/validate \
  -H "Authorization: Bearer <TOKEN>"
```

## Migraciones

`V1__init_autenticacion.sql` crea las tablas `cuenta` y `sesion`. Hibernate corre con
`ddl-auto: validate`, por lo que el esquema debe coincidir con las entidades.

## CORS

CORS se configura centralizadamente en `api-gateway`. El frontend debe llamar al gateway:

```text
http://localhost:8080/api/v1/auth/**
```

y no directamente al puerto `8081`.
