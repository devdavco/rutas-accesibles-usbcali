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

## Levantar todo con un comando

```bash
./scripts/levantar.sh   # base de datos, los tres servicios y el frontend (http://localhost:4200)
./scripts/detener.sh    # detiene todo; la base de datos conserva sus datos
```

`levantar.sh` carga `infra/.env`, compila los servicios y los arranca en orden (servicio-negocio,
servicio-grafos, gateway), esperando a que cada uno responda en `/actuator/health`. Los logs y los
PID quedan en `logs/`.

## Levantar todo para probar (paso a paso, a mano)

El orden importa: servicio-grafos lee el grafo de servicio-negocio en cada cálculo, y el gateway
reenvía a los dos. Cada servicio va en su propia terminal, desde `backend/`.

1. **Base de datos** (ver la sección anterior): `cd infra && docker compose up -d`.
2. **servicio-negocio** (8081). Toma las credenciales de `infra/.env`; al arrancar, Flyway crea
   el esquema y carga el grafo del campus:
   ```bash
   set -a; source ../infra/.env; set +a
   DB_USER=$POSTGRES_USER DB_PASSWORD=$POSTGRES_PASSWORD ./gradlew :servicio-negocio:bootRun
   ```
3. **servicio-grafos** (8082):
   ```bash
   ./gradlew :servicio-grafos:bootRun
   ```
4. **gateway** (8080):
   ```bash
   ./gradlew :gateway:bootRun
   ```

Variables opcionales:

| Variable | Servicio | Valor por defecto |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | servicio-negocio | `localhost`, `5432`, `rutas` |
| `NEGOCIO_URL` | servicio-grafos y gateway | `http://localhost:8081` |
| `GRAFOS_URL` | gateway | `http://localhost:8082` |
| `CORS_ORIGENES` | gateway (lista separada por comas) | `http://localhost:4200` |

Cada servicio responde en `http://localhost:<puerto>/actuator/health`. El gateway enruta
`/api/rutas/**` a servicio-grafos y `/api/admin/**` a servicio-negocio. La lectura interna del
grafo (`/interno/grafo`) no pasa por el gateway.

Para probar: abra `backend/http/rutas.http` en IntelliJ (salud, lista de puntos, una ruta con y
sin pasos, y errores 400), o use curl:

```bash
curl 'http://localhost:8080/api/rutas/puntos'
curl 'http://localhost:8080/api/rutas?origen=1&destino=2'
curl --compressed 'http://localhost:8080/api/rutas?origen=1&destino=2&pasos=true'
```

## Frontend

```bash
cd frontend
npm ci
npm start        # http://localhost:4200 (usa el gateway en http://localhost:8080)
```

Interfaz del usuario final: consulta, resultado, paso a paso de Dijkstra y caso sin ruta. El estado
queda en la URL (`/ruta?origen=1&destino=2&modo=pasos`). El mapa usa MapLibre GL JS con
OpenFreeMap y el plano del campus (`frontend/public/plano/`, copiado del prototipo con
`node backend/herramientas/copiar-plano.mjs`). Los estilos usan los tokens de `docs/diseno/tokens.css`.

## Compilar y probar

```bash
cd backend && ./gradlew build      # las pruebas de servicio-negocio usan Testcontainers (requiere Docker)
cd frontend && npm run test:ci && npx ng build --configuration production
```
