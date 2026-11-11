# Sistema de Votación en Tiempo Real

Proyecto de la asignatura **Sistemas Distribuidos (Unidad 1)**.

Sistema de votación en tiempo real basado en **microservicios contenerizados**, que usa
**colas de mensajes (RabbitMQ)** para desacoplar el procesamiento de votos y
**WebSockets** para publicar los resultados en vivo, garantizando la **prevención del
doble voto** ante una ingesta concurrente.

## Integrantes
- Arelys Ajila
- Isael Rodríguez
- Tayron Morales
- Jasiel Quezada
- Docente: Ing. Jose Guaman

## Arquitectura

| Componente | Puerto | Base de datos | Contenedor Postgres |
|---|---|---|---|
| `api-gateway` | 8080 | *(sin BD)* | — |
| `autenticacion-service` | 8081 | `autenticaciondb` | `postgres-autenticacion` (5433) |
| `votacion-service` | 8082 | `votaciondb` | `postgres-votacion` (5435) |
| `resultados-service` | 8083 | `resultadosdb` | `postgres-resultados` (5436) |
| `usuarios-service` | 8084 | `usuariosdb` | `postgres-usuarios` (5434) |
| `frontend` (React) | 5173 / 3000 | — | — |
| `rabbitmq` | 5672 / 15672 | — | `votacion-rabbitmq` |

### Tipos de comunicación
- **Síncrona (REST):** el frontend llama al `api-gateway`, y este enruta a cada servicio.
- **Asíncrona (RabbitMQ):** eventos entre servicios (nunca llamadas directas).
- **Tiempo real (WebSocket):** `resultados-service` difunde el conteo en vivo.

### Flujo del voto
```
Frontend ──REST──► api-gateway ──► votacion-service ──► votaciondb
                                          │
                                          └─ evento `voto.emitido` ─► RabbitMQ
                                                                        │
                                          resultados-service ◄──────────┘
                                                    │
                                                    └─ WebSocket ─► Frontend (en vivo)
```

### Mensajería (RabbitMQ)
| Recurso | Nombre |
|---|---|
| Exchange | `votacion.exchange` |
| Cola | `votos.procesados` |
| Routing key | `voto.emitido` |
| Evento | `VotoEmitidoEvent` |
| DLQ | `votos.procesados.dlq` |

## Tecnologías
- **Backend:** Java 21 + Spring Boot 4 (Web, Data JPA, AMQP, WebSocket, Flyway, Actuator)
- **Mensajería:** RabbitMQ
- **Base de datos:** PostgreSQL (una instancia por servicio)
- **Frontend:** React + Vite
- **Infraestructura:** Docker + Docker Compose

## Estructura del repositorio
```
Proyecto_U1_SistemasDistribuidos/
├── docker-compose.yml          # infraestructura compartida (RabbitMQ)
├── compose.full.yml            # sistema integrado (infra + Resultados + gateway + frontend)
├── .env.example                # plantilla de variables
├── services/
│   ├── api-gateway/            # punto de entrada (Spring Cloud Gateway)
│   │   ├── application.yml
│   │   └── ...
│   ├── votacion-service/
│   │   ├── docker-compose.yml  # SOLO su BD + su contenedor
│   │   ├── Dockerfile
│   │   └── src/
│   └── autenticacion-service/
│       ├── docker-compose.yml  # BD + contenedor del servicio
│       ├── Dockerfile
│       └── src/
└── frontend/                   # React + Vite (una sola app)
    └── src/features/
        ├── autenticacion/
        ├── votacion/
        ├── resultados/
        └── usuarios/
```

## Requisitos previos
- **Docker** y **Docker Compose**
- **Java 21** para ejecutar microservicios desde el IDE
- **Node.js 20+** para el frontend
- **Maven Wrapper** incluido en los servicios; no hace falta Maven global

## Cómo levantar el proyecto

### 1. Configurar variables
```bash
cp .env.example .env
```

### 2. Levantar el sistema integrado
```bash
docker compose -f compose.full.yml up
```
Servicios incluidos: RabbitMQ, PostgreSQL de Votación, PostgreSQL y Resultados,
gateway y frontend. La aplicación de Votación aún no tiene Dockerfile ni productor
implementado.

### 3. Levantar solo la infraestructura
```bash
docker compose -f docker-compose.yml -f services/votacion-service/docker-compose.yml up -d
docker compose -f docker-compose.yml -f services/autenticacion-service/docker-compose.yml up -d
```
> El microservicio de autenticacion crea la cuenta inicial `admin` / `admin123`
> (configurable con `APP_ADMIN_USERNAME` y `APP_ADMIN_PASSWORD`).

### 4. Ejecutar microservicios desde el IDE
```bash
cd services/resultados-service
./mvnw spring-boot:run

cd services/api-gateway
./mvnw spring-boot:run

cd services/votacion-service
./mvnw spring-boot:run
```

## Resultados

- REST: `GET http://localhost:8080/api/resultados/elecciones/{eleccionId}`
- WebSocket/STOMP: `ws://localhost:8080/ws`
- Tópico: `/topic/resultados/elecciones/{eleccionId}`
- Consumidor: exchange `votacion.exchange`, routing key `voto.emitido`, cola `votos.procesados`
- Idempotencia por `eventId` y por `(eleccionId, votoId)`
- Persistencia propia en `resultadosdb`; lectura pública en el perfil local

El frontend permite consultar una elección por UUID y recibe actualizaciones por STOMP.
Cuando no existen votos, muestra el estado vacío sin inventar catálogo ni nombres.

### Datos de prueba simulados
Los fixtures publican eventos directamente en RabbitMQ; no representan una emisión real
de Votación:
```bash
node scripts/resultados/publicar-fixtures.mjs --eleccion <uuid> --votos 6
node scripts/resultados/smoke-gateway.mjs --base http://localhost:3000
```

## Frontend (React + Vite)

```bash
cd frontend
npm ci
npm run dev       # http://localhost:5173
npm test
npm run build
```

El proxy de desarrollo envía `/api` y `/ws` al gateway (`http://localhost:8080`).
En producción nginx sirve el build y reenvía `/api` y `/ws` al gateway.

## Pruebas

```bash
cd services/resultados-service && ./mvnw test
cd services/api-gateway && ./mvnw test
cd services/votacion-service && ./mvnw test
cd frontend && npm test && npm run build
docker compose -f compose.full.yml config --quiet
```

## Estado actual
- [x] Infraestructura (RabbitMQ) y estructura por fragmentos
- [x] Esqueleto de `votacion-service` (conectado a `votaciondb`)
- [x] `api-gateway` (Spring Cloud Gateway, enrutando `/api/votos` y `/api/v1/auth`)
- [x] Frontend React base (Vite + estructura por features)
- [x] `autenticacion-service` (JWT, sesiones, Flyway y Docker)
- [ ] Flujo REST del voto + productor RabbitMQ (`votacion-service`)
- [ ] `resultados-service` (consumidor + WebSocket)
- [ ] `usuarios-service`
- [ ] Dockerfiles + `compose.full.yml` completo (`docker compose -f compose.full.yml up`)

## Flujo de trabajo con Git
- Cada integrante trabaja en **su propia rama**.
- No subir **nunca** el archivo `.env` (ya está en `.gitignore`).
- Antes de commitear: `git status` y verificar que no aparezcan `.env` ni `target/`.
