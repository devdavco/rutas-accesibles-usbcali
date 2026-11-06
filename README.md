# Rutas accesibles USB Cali

Herramienta web que calcula rutas accesibles para personas en silla de ruedas en el campus
de la Universidad de San Buenaventura Cali, modelando el campus como un grafo ponderado y
usando el algoritmo de Dijkstra. Trabajo de grado.

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | Gradle multimódulo: `servicio-negocio` (8081), `servicio-grafos` (8082), `gateway` (8080) |
| `frontend/` | Aplicación Angular |
| `infra/` | `docker-compose.yml` con PostgreSQL + PostGIS |
| `docs/` | Documentación del proyecto (incluye el registro de uso de IA) |

## Requisitos

- Java 25 (Temurin)
- Node.js 24 y npm
- Docker con Docker Compose

## Levantar la base de datos

```bash
cd infra
cp .env.example .env   # complete POSTGRES_USER y POSTGRES_PASSWORD
docker compose up -d
```

## Levantar los servicios

Desde `backend/`, cada uno en una terminal:

```bash
# servicio-negocio: necesita las mismas credenciales del .env
DB_USER=<usuario> DB_PASSWORD=<clave> ./gradlew :servicio-negocio:bootRun
./gradlew :servicio-grafos:bootRun
./gradlew :gateway:bootRun
```

Variables opcionales: `DB_HOST` (localhost), `DB_PORT` (5432), `DB_NAME` (rutas) para
servicio-negocio; `NEGOCIO_URL` y `GRAFOS_URL` para el gateway.

Cada servicio responde en `http://localhost:<puerto>/actuator/health`. El gateway enruta
`/api/admin/**` a servicio-negocio y `/api/rutas/**` a servicio-grafos.

## Frontend

```bash
cd frontend
npm ci
npm start        # http://localhost:4200
```

## Compilar y probar

```bash
cd backend && ./gradlew build      # las pruebas de servicio-negocio usan Testcontainers (requiere Docker)
cd frontend && npm run test:ci && npx ng build --configuration production
```
