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
| `usuario-service` | 8084 | `db_usuarios` | `postgres-usuario` (5434) |
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
├── docker-compose.yml          # entrada unica para levantar todo el sistema
├── compose.full.yml            # definicion integrada incluida por docker-compose.yml
├── .env.example                # plantilla de variables
├── services/
│   ├── api-gateway/            # punto de entrada (Spring Cloud Gateway)
│   │   ├── application.yml
│   │   └── ...
│   ├── autenticacion-service/
│   │   ├── docker-compose.yml  # BD + contenedor del servicio
│   │   ├── Dockerfile
│   │   └── src/
│   ├── usuario-service/
│   │   ├── docker-compose.yml  # BD + contenedor del servicio
│   │   ├── Dockerfile
│   │   └── src/
│   ├── votacion-service/
│   │   ├── docker-compose.yml  # SOLO su BD + su contenedor
│   │   ├── Dockerfile
│   │   └── src/
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

Hay dos formas:

- **Opción A:** todo en una sola PC (modo local, ideal para empezar y entregar)
- **Opción B:** distribuido en 4 PCs (un microservicio por PC, el objetivo de la materia)

---

### Opción A — Todo en una sola PC

#### A.1 Configurar variables

```bash
cp .env.example .env
```
Verifica que `.env` contenga un `JWT_SECRET` de al menos 32 caracteres.

> `JWT_SECRET` debe tener al menos 32 caracteres. Si usas el valor de ejemplo del
> archivo, el login funcionará, pero en producción cámbialo.

#### A.2 Crear la red compartida (una sola vez)

```bash
docker compose up -d --build
```
> Si dice "already exists", ignóralo.

#### A.3 Levantar todo el sistema

```bash
docker compose -f compose.full.yml up -d --build
docker compose -f compose.full.yml ps
```

> Usa `--build` la primera vez para compilar las imágenes de los microservicios.
> Espera a que todos los contenedores estén en estado `Up` o `healthy`.

#### A.4 Crear el perfil del administrador (obligatorio la primera vez)

`autenticacion-service` crea la cuenta `admin` en su propia base de datos, pero
`usuario-service` **no crea ningún perfil**. Sin ese perfil el login del frontend
te devuelve al formulario (detrás hace una llamada a
`/api/v1/usuarios/cuenta/<id>` y recibe `404`).

Comprobar que falta:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8080/api/v1/usuarios/cuenta/1
# debería imprimir 404
```

Insertar el perfil (en **PC1**, dentro del contenedor de PostgreSQL de usuarios):

```bash
docker exec -i votacion-postgres-usuario \
  psql -U admin -d db_usuarios <<'SQL'
INSERT INTO usuario (id_cuenta, cedula, correo, nickname, nombre, apellido, rol)
VALUES (1, '0987654321', 'admin@unl.edu.ec', 'admin', 'Admin', 'Sistema', 'ADMIN');
SQL
```

Verificar que ya existe:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8080/api/v1/usuarios/cuenta/1
# ahora imprime 200
```

> `id_cuenta=1` es el id de la cuenta `admin` en `autenticaciondb` (PC2 en modo
> distribuido; aquí todo está en la misma red Docker).

Alternativa: crear un usuario nuevo (esto crea la cuenta en auth **y** el perfil en usuarios por RabbitMQ):

```bash
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H 'Content-Type: application/json' \
  -d '{
    "cedula": "0900000002",
    "correo": "votante02@unl.edu.ec",
    "nickname": "votante02",
    "nombre": "Votante",
    "apellido": "Dos",
    "clave": "votante123",
    "rol": "VOTANTE"
  }'
```

#### A.5 Abrir la aplicación

Navegador:

```
http://localhost:3000
```

Credenciales del administrador:

| Usuario | Clave |
|---------|-------|
| `admin` | `admin123` |

#### A.6 Frontend en modo desarrollo (opcional, solo frontend)

```bash
cd frontend
npm install
npm run dev
```

- Vite corre en `http://localhost:5173`
- Proxy automático de `/api` y `/ws` hacia `http://localhost:8080`

#### A.7 Apagar

```bash
docker compose -f compose.full.yml down
```

> Nunca uses `down -v`: borra los volúmenes de las bases de datos y tendrías que
> volver a insertar el perfil del administrador.

---

### Opción B — Distribuido en 4 PCs

Cada PC corre **un microservicio** más su PostgreSQL local. Se comunican entre sí
por Tailscale (o por el hotspot del celular). No hace falta que cada PC levante todo.

#### Quién define las IPs

**Nadie las escribe en el código.** Las asigna la red:

- Tailscale → `100.x.y.z` (estable mientras no borres el nodo)
- Hotspot del celular → `192.168.x.x` (DHCP, puede cambiar)

Se escriben en **un archivo por PC**: `.env.pc1`, `.env.pc2`, `.env.pc3`,
`.env.pc4`, en la raíz del repo. **Cada PC tiene el suyo y el contenido es distinto.**
Están en `.gitignore`: no viajan por Git, cada quien los crea en su máquina.

Regla mental: **cada PC necesita la IP de quien consume.**

| Variable | Archivo | Apunta a |
|----------|---------|----------|
| `AUTH_SERVICE_URI` | `.env.pc1` | `http://<IP-PC2>:8081` |
| `VOTACION_SERVICE_URI` | `.env.pc1` | `http://<IP-PC3>:8082` |
| `RESULTADOS_URL` | `.env.pc1` | `http://<IP-PC4>:8083` |
| `RESULTADOS_WS_URL` | `.env.pc1` | `ws://<IP-PC4>:8083/ws` |
| `USUARIO_SERVICE_URI` | `.env.pc1` | `http://usuario-service:8084` (local) |
| `CORS_ALLOWED_ORIGINS` | `.env.pc1` | incluye `http://<IP-PC1>:3000` |
| `GATEWAY_WS_ALLOWED_ORIGINS` | `.env.pc1` | incluye `http://<IP-PC1>:3000` |
| `RABBITMQ_HOST` | `.env.pc2`, `.env.pc3`, `.env.pc4` | `<IP-PC1>` |
| `RESULTADOS_CORS_ALLOWED_ORIGINS` | `.env.pc4` | incluye `http://<IP-PC1>:3000` |
| `RESULTADOS_WS_ALLOWED_ORIGINS` | `.env.pc4` | incluye `http://<IP-PC1>:3000` |
| `JWT_SECRET` | `.env.pc1`, `.env.pc2` | **el mismo valor en ambos** |

> `votacion-service` no lee `JWT_SECRET`, así que PC3 no lo necesita.

#### Paso 1 — Obtener las IPs

En cada PC:
```bash
tailscale ip -4
```
O todas de una vez desde cualquiera:
```bash
tailscale status
```

IPs actuales de este equipo:

| PC | Hostname | IP Tailscale | Rol |
|----|----------|--------------|-----|
| PC1 | nobara | 100.82.125.26 | api-gateway + frontend + RabbitMQ + usuario-service |
| PC2 | asusirlx | 100.97.199.50 | autenticacion-service |
| PC3 | jasielx | 100.117.1.27 | votacion-service |
| PC4 | are | 100.72.69.83 | resultados-service |

#### Paso 2 — Crear la red Docker (una sola vez por PC)

```bash
docker network create votacion-network
```
> Si dice "already exists", ignóralo.

#### Paso 3 — Generar el secreto JWT compartido (una vez)

En PC1 genera el secreto y pásaselo a PC2 (debe ser idéntico en ambos):

```bash
openssl rand -base64 48
```
> Debe tener al menos 32 caracteres (HS256 exige 256 bits).

#### Paso 4 — Crear el `.env.pcN` en cada PC

Copia la plantilla correspondiente y reemplaza las IPs:

```bash
cp .env.pc1.example .env.pc1
cp .env.pc2.example .env.pc2
cp .env.pc3.example .env.pc3
cp .env.pc4.example .env.pc4
```

Luego edita cada `.env.pcN` con las IPs reales de tu equipo (de la tabla del Paso 1).

**PC1 — nobara**, archivo `.env.pc1` (IPs de ejemplo, ajústalas):
```env
JWT_SECRET=<el_mismo_secreto_que_en_PC2>
RABBITMQ_USER=guest
RABBITMQ_PASS=guest

AUTH_SERVICE_URI=http://100.97.199.50:8081
VOTACION_SERVICE_URI=http://100.117.1.27:8082
RESULTADOS_URL=http://100.72.69.83:8083
RESULTADOS_WS_URL=ws://100.72.69.83:8083/ws
USUARIO_SERVICE_URI=http://usuario-service:8084

CORS_ALLOWED_ORIGINS=http://localhost:5173,http://100.82.125.26:3000
GATEWAY_WS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://100.82.125.26:3000

POSTGRES_USER=admin
POSTGRES_PASSWORD=admin
RABBITMQ_HOST=rabbitmq
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-usuario:5432/db_usuarios
FRONTEND_PORT=3000
GATEWAY_PORT=8080
```

**PC2 — asusirlx**, archivo `.env.pc2`:
```env
JWT_SECRET=<el_mismo_secreto_que_en_PC1>
JWT_EXPIRATION_MS=3600000
APP_ADMIN_ENABLED=true
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=admin123

RABBITMQ_HOST=100.82.125.26
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest

POSTGRES_USER=admin
POSTGRES_PASSWORD=admin
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-autenticacion:5432/autenticaciondb
```

**PC3 — jasielx**, archivo `.env.pc3`:
```env
RABBITMQ_HOST=100.82.125.26
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest

POSTGRES_USER=admin
POSTGRES_PASSWORD=admin
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-votacion:5432/votaciondb
```

**PC4 — are (Windows)**, archivo `.env.pc4`:
```env
RABBITMQ_HOST=100.82.125.26
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest

RESULTADOS_DB_URL=jdbc:postgresql://resultados-db:5432/resultadosdb
RESULTADOS_DB_USER=resultados
RESULTADOS_DB_PASSWORD=resultados

RESULTADOS_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://100.82.125.26:3000
RESULTADOS_WS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://100.82.125.26:3000
```

#### Paso 5 — Levantar cada PC

**Orden:** PC1 primero (RabbitMQ), después PC2, PC3 y PC4.

**PC1 — nobara:**
```bash
cd ~/Files/SD/Proyecto_U1_SistemasDistribuidos
docker compose --env-file .env.pc1 -f distributed/pc1.yml up -d --build
```
Queda en: `http://<IP-PC1>:3000`

**PC2 — asusirlx:**
```bash
cd ~/Files/SD/Proyecto_U1_SistemasDistribuidos
docker compose --env-file .env.pc2 \
  -f services/autenticacion-service/docker-compose.yml up -d --build
```

**PC3 — jasielx:**
```bash
cd ~/Files/SD/Proyecto_U1_SistemasDistribuidos
docker compose --env-file .env.pc3 \
  -f services/votacion-service/docker-compose.yml up -d --build
```

**PC4 — are (Windows PowerShell):**
```powershell
cd ~/Files/SD/Proyecto_U1_SistemasDistribuidos
docker compose --env-file .env.pc4 `
  -f services/resultados-service/docker-compose.yml up -d --build
```

#### Paso 6 — Crear el perfil del administrador en PC1

Es lo mismo que en Opción A: `admin` existe en auth (PC2), pero falta su perfil
en usuario-service (PC1).

En **PC1**:

```bash
docker exec -i votacion-postgres-usuario \
  psql -U admin -d db_usuarios <<'SQL'
INSERT INTO usuario (id_cuenta, cedula, correo, nickname, nombre, apellido, rol)
VALUES (1, '0987654321', 'admin@unl.edu.ec', 'admin', 'Admin', 'Sistema', 'ADMIN');
SQL
```

#### Paso 7 — Abrir puertos en el firewall

**nobara / jasielx (Fedora, firewalld):**
```bash
sudo firewall-cmd --permanent --add-port=3000/tcp --add-port=8080/tcp --add-port=5672/tcp --add-port=15672/tcp
sudo firewall-cmd --reload
```

**asusirlx (si usa ufw):**
```bash
sudo ufw allow 8081/tcp
```

**are (Windows, PowerShell como administrador):**
```powershell
New-NetFirewallRule -DisplayName "Votacion 8083" -Direction Inbound `
  -LocalPort 8083 -Protocol TCP -Action Allow
```

#### Paso 8 — Verificar

Desde cualquier PC:

```bash
curl http://100.82.125.26:3000/
curl http://100.97.199.50:8081/actuator/health
curl http://100.117.1.27:8082/actuator/health
curl http://100.72.69.83:8083/actuator/health
```

En PC1, RabbitMQ debe mostrar conexiones de las otras tres PCs:

```bash
docker exec votacion-rabbitmq rabbitmqctl list_connections user peer_host
```

Abrir en el navegador:

```
http://100.82.125.26:3000
```

Credenciales:

| Usuario | Clave |
|---------|-------|
| `admin` | `admin123` |

#### Apagar cada PC

**PC1:**
```bash
docker compose --env-file .env.pc1 -f distributed/pc1.yml down
```

**PC2:**
```bash
docker compose --env-file .env.pc2 \
  -f services/autenticacion-service/docker-compose.yml down
```

**PC3:**
```bash
docker compose --env-file .env.pc3 \
  -f services/votacion-service/docker-compose.yml down
```

**PC4:**
```bash
docker compose --env-file .env.pc4 \
  -f services/resultados-service/docker-compose.yml down
```

---

### Universidad / sin Tailscale (hotspot del celular)

Solo cambian las IPs. **No se toca código, Dockerfiles ni compose.**

1. Conecta las 4 PCs al mismo hotspot.
2. Obtén la IP de cada una:
   - Linux: `ip -4 addr show | grep 192.168`
   - Windows: `ipconfig`
3. Reemplaza en cada `.env.pcN`:

| Archivo | Qué reemplazar | Por |
|---------|----------------|-----|
| `.env.pc1` | `100.97.199.50` | IP de PC2 en el hotspot |
| | `100.117.1.27` | IP de PC3 en el hotspot |
| | `100.72.69.83` (2 veces) | IP de PC4 en el hotspot |
| | `100.82.125.26` (2 veces) | IP de PC1 en el hotspot |
| `.env.pc2` | `RABBITMQ_HOST` | IP de PC1 en el hotspot |
| `.env.pc3` | `RABBITMQ_HOST` | IP de PC1 en el hotspot |
| `.env.pc4` | `RABBITMQ_HOST` + las 2 de CORS/WS | IP de PC1 en el hotspot |

4. Vuelve a levantar con el mismo comando de cada PC.

> El hotspot **no debe tener aislamiento de clientes** (AP isolation), o las PCs
> no se ven entre sí. El DHCP puede cambiar las IPs al reconectar: verifica
> con `ip a` / `ipconfig` y actualiza el `.env.pcN` si cambió.

---

### Problemas comunes

| Síntoma | Causa probable |
|---------|----------------|
| `curl` a otra PC no responde | firewall cerrado o Tailscale desconectado (`tailscale status`) |
| Gateway da 502 | la PC destino no está arriba o la URI del `.env.pc1` está mal |
| RabbitMQ no recibe conexiones | `RABBITMQ_HOST` del `.env.pc2/3/4` apunta a una IP vieja |
| Error de CORS en el navegador | falta `http://<IP-PC1>:3000` en `CORS_ALLOWED_ORIGINS` |
| `include` no reconocido | Docker Compose < 2.20; actualiza (`docker compose version`) |
| JWT inválido entre gateway y auth | `JWT_SECRET` distinto en `.env.pc1` y `.env.pc2` |
| Login no avanza del formulario | falta el perfil del usuario en `db_usuarios` (ver A.4) |

### 3. Levantar SOLO tu microservicio (desarrollo diario)

Cada quien trabaja con **su** fragmento, sin tocar el de los demás. Primero crea la red compartida (solo la primera vez):
```bash
docker network create votacion-network
```
> El microservicio de autenticacion crea la cuenta inicial `admin` / `admin123`
> (configurable con `APP_ADMIN_USERNAME` y `APP_ADMIN_PASSWORD`).

Luego levanta RabbitMQ (infraestructura compartida) más tu servicio:
```bash
# RabbitMQ (infraestructura compartida)
docker compose -f docker-compose.yml up -d

# usuario-service (puerto 8084)
docker compose -f docker-compose.yml -f services/usuario-service/docker-compose.yml up -d

# autenticacion-service (puerto 8081)
docker compose -f docker-compose.yml -f services/autenticacion-service/docker-compose.yml up -d

# votacion-service (puerto 8082)
docker compose -f docker-compose.yml -f services/votacion-service/docker-compose.yml up -d

# api-gateway (puerto 8080)
docker compose -f docker-compose.yml -f services/api-gateway/docker-compose.yml up -d
```
> El microservicio de autenticacion crea la cuenta inicial `admin` / `admin123`
> (configurable con `APP_ADMIN_USERNAME` y `APP_ADMIN_PASSWORD`).

### 4. Ejecutar un microservicio desde el IDE

Los servicios se levantan con Docker, no es necesario ejecutar `mvnw` desde el IDE.
Usa `docker compose` desde la carpeta de cada servicio:

```bash
cd services/usuario-service
docker compose -f docker-compose.yml up -d
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
- [x] Dockerfiles + `compose.full.yml` completo (`docker compose -f compose.full.yml up`)

## Flujo de trabajo con Git
- Cada integrante trabaja en **su propia rama**.
- No subir **nunca** el archivo `.env` (ya está en `.gitignore`).
- Antes de commitear: `git status` y verificar que no aparezcan `.env` ni `target/`.
