# Despliegue distribuido por PC

Cada PC levanta **solo** sus servicios usando su archivo `pcN.yml`. Estos archivos
usan `include` para reutilizar los `docker-compose.yml` de cada servicio, de modo
que dichos compose siguen funcionando de forma individual (sin cambios).

| PC | Host Tailscale | IP | Archivo | Servicios |
|---|---|---|---|---|
| PC1 | nobara | 100.82.125.26 | `pc1.yml` | api-gateway, frontend, usuario-service, rabbitmq |
| PC2 | asusirlx | 100.97.199.50 | `pc2.yml` | autenticacion-service (+ su postgres) |
| PC3 | jasielx | 100.117.1.27 | `pc3.yml` | votacion-service (+ su postgres) |
| PC4 | are | 100.72.69.83 | `pc4.yml` | resultados-service (+ su postgres) |

## Requisito previo (solo una vez, en PC1)

La red externa debe existir antes de levantar cualquier PC:

```bash
docker network create votacion-network
```

## Levantar cada PC

Siempre desde la **raíz del repositorio** y con el `--env-file` de esa PC:

```bash
# PC1
docker compose --env-file .env.pc1 -f distributed/pc1.yml up -d --build

# PC2
docker compose --env-file .env.pc2 -f distributed/pc2.yml up -d --build

# PC3
docker compose --env-file .env.pc3 -f distributed/pc3.yml up -d --build

# PC4
docker compose --env-file .env.pc4 -f distributed/pc4.yml up -d --build
```

> No uses `-f services/<servicio>/docker-compose.yml` en modo distribuido: esos
> archivos asumen que RabbitMQ y los demas servicios estan en la misma maquina.

## RabbitMQ

RabbitMQ vive **solo en PC1**. Las demas PCs se conectan a `RABBITMQ_HOST`
(`100.82.125.26` por defecto, definido en cada `.env.pcN`).

Verificar que un servicio remoto se conecto:

```bash
docker exec votacion-rabbitmq rabbitmqctl list_connections
```

Verificar consumidores (creacion de usuarios usa `cuentas.crear`):

```bash
docker exec votacion-rabbitmq rabbitmqctl list_queues name messages consumers
```

## Variables que deben coincidir entre PCs

- `JWT_SECRET`: identico en PC1, PC2 y PC3, o los tokens no se validan.
- `CORS_ALLOWED_ORIGINS` (PC1): debe incluir el origen exacto desde el que se
  abre el frontend (`http://localhost:3000`, `http://127.0.0.1:3000`,
  `http://100.82.125.26:3000`). Si cambia, recrear el gateway:

  ```bash
  docker compose --env-file .env.pc1 -f distributed/pc1.yml up -d --force-recreate api-gateway
  ```

## Frontend y WebSocket

`frontend/nginx.conf` envia `/api` y `/ws` al `api-gateway`. El gateway reenvia
`/ws` a `RESULTADOS_WS_URL` (PC4), asi que el navegador solo necesita conocer PC1.
