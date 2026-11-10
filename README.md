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
| `frontend` (React) | 5173 / 80 | — | — |
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

## Tecnologías
- **Backend:** Java 21 + Spring Boot 4 (Web, Data JPA, AMQP, WebSocket, Flyway, Actuator)
- **Mensajería:** RabbitMQ
- **Base de datos:** PostgreSQL 16 (una instancia por servicio)
- **Frontend:** React (Vite)
- **Infraestructura:** Docker + Docker Compose

## Estructura del repositorio
```
Proyecto_U1_SistemasDistribuidos/
├── docker-compose.yml          # infra compartida (RabbitMQ)
├── compose.full.yml            # junta todo el sistema (entrega final)
├── .env                        # variables/secretos (NO se sube a Git)
├── .env.example                # plantilla de variables
├── services/
│   ├── api-gateway/            # punto de entrada (Spring Cloud Gateway)
│   │   ├── application.yml
│   │   └── ...
│   └── votacion-service/
│       ├── docker-compose.yml  # SOLO su BD + su contenedor
│       ├── Dockerfile
│       └── src/
└── frontend/                   # React + Vite (una sola app)
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/client.js       # cliente axios (compartido)
        ├── components/         # UI común
        └── features/
            ├── autenticacion/
            ├── votacion/
            ├── resultados/
            └── usuarios/
```

## Requisitos previos
- **Docker** y **Docker Compose** (obligatorio)
- **Java 21** (solo si desarrollas un microservicio desde el IDE)
- **Node.js 20+** (solo si desarrollas el frontend)

> Los proyectos Spring Boot traen el wrapper `mvnw`, así que **no necesitas instalar Maven**.

## Cómo levantar el proyecto

### 1. Configura tus variables
Copia la plantilla y ajusta si quieres:
```bash
cp .env.example .env
```

### 2. Levantar TODO el sistema (entrega)
```bash
docker compose up
```
> Mientras el proyecto esté en construcción, algunos servicios/frontend aún no existen.
> Usa la opción 3 para trabajar solo en tu parte.

### 3. Levantar SOLO tu microservicio (desarrollo diario)
Cada quien trabaja con **su** fragmento, sin tocar el de los demás:
```bash
docker compose -f docker-compose.yml -f services/votacion-service/docker-compose.yml up -d
```

### 4. Ejecutar un microservicio desde el IDE (sin Docker para el servicio)
```bash
cd services/votacion-service
./mvnw spring-boot:run
```
Igual para cualquier otro servicio, p. ej. el gateway:
```bash
cd services/api-gateway
./mvnw spring-boot:run
```

## Frontend (React + Vite)

Es **una sola aplicación** en `frontend/`, organizada por **features** (una carpeta por
dominio/microservicio) para que cada integrante trabaje sin chocar con los demás.

### Instalar y ejecutar
```bash
cd frontend
npm install        # instala dependencias
npm run dev        # servidor de desarrollo: http://localhost:5173
```
Dependencias principales: `axios` (REST), `react-router-dom` (rutas),
`@stomp/stompjs` + `sockjs-client` (WebSocket de resultados en vivo).

> Importante: instala **dentro de `frontend/`**, no en la raíz del repo.

### Estructura y "dueños"
```
frontend/src/
├── api/client.js        # ⚠️ COMPARTIDO: cliente axios base (baseURL '/api')
├── components/          # UI común
└── features/
    ├── autenticacion/   # dueño: quien haga autenticación
    ├── votacion/        # dueño: quien haga votación
    ├── resultados/      # dueño: quien haga resultados en vivo
    └── usuarios/        # dueño: quien haga usuarios
```
Cada integrante edita **solo su carpeta** `features/<x>/`.

### Proxy de desarrollo (`vite.config.js`)
Evita problemas de CORS reenviando las llamadas:
- `/api` → `http://localhost:8080` (`api-gateway`)
- `/ws`  → `http://localhost:8083` (`resultados-service`, WebSocket)

### Reglas para no generar conflictos
- Archivos **compartidos** (`package.json`, `vite.config.js`, `router.jsx`,
  `api/client.js`): editarlos **una persona a la vez**.
- Instalar todas las dependencias comunes **de una vez** (un commit compartido).
- No importar código de la carpeta de otro; usar `api/client.js` y `components/`.

### Producción (Docker)
El frontend se compila (`npm run build`) y se sirve con **nginx** en un contenedor.
Su `Dockerfile` y su fragmento `frontend/docker-compose.yml` se agregan en la fase de
dockerización.

## Convenciones del equipo

### Nombres
- Microservicio: `<dominio>-service` en **español** (ej. `votacion-service`).
- Base de datos: `<dominio>db` (ej. `votaciondb`).
- Paquete base: `ec.unl.votacion.<dominio>`.

### Reglas de arquitectura
- **Cada microservicio** tiene su carpeta, su `pom.xml`, su **Dockerfile**, su
  **fragmento `docker-compose.yml`** y su **propia base de datos**.
- **Nunca** un servicio accede directamente a la BD de otro.
- Cada servicio define sus tablas con **migraciones Flyway** (`src/main/resources/db/migration`).
- Comunicación entre servicios **solo por RabbitMQ** (procesos asíncronos).

### Cómo agregar un microservicio nuevo
1. Crea `services/<nombre>/` con su proyecto Spring Boot.
2. Añade su `docker-compose.yml` (su Postgres + su contenedor).
3. Agrega su línea al `include:` de `compose.full.yml`.
4. Mueve su migración `V1__...sql` y su `Dockerfile`.

## Estado actual
- [x] Infraestructura (RabbitMQ) y estructura por fragmentos
- [x] Esqueleto de `votacion-service` (conectado a `votaciondb`)
- [x] `api-gateway` (Spring Cloud Gateway, enrutando `/api/votos` → `votacion-service`)
- [x] Frontend React base (Vite + estructura por features)
- [ ] Flujo REST del voto + productor RabbitMQ (`votacion-service`)
- [ ] `resultados-service` (consumidor + WebSocket)
- [ ] `autenticacion-service` y `usuarios-service`
- [ ] Dockerfiles + `compose.full.yml` completo (`docker compose up`)

## Flujo de trabajo con Git
- Cada integrante trabaja en **su propia rama** (ej. `jasiel-dev`).
- No subir **nunca** el archivo `.env` (ya está en `.gitignore`).
- Antes de commitear: `git status` y verificar que no aparezcan `.env` ni `target/`.
