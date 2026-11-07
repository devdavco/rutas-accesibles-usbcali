# Despliegue

## Arquitectura de producción

Una instancia EC2 t3.small (x86, 2 vCPU, 2 GB de RAM) en us-east-1 con Docker Compose. Solo
Caddy publica puertos; el resto de contenedores vive en la red interna `interna`.

```
                 Internet
                    │  80 / 443 (HTTP/1.1, HTTP/2, HTTP/3)
                    ▼
        ┌───────────────────────────┐
        │ caddy  (imagen frontend)  │  HTTPS automático (Let's Encrypt) si hay DOMINIO
        │  /        → archivos de   │  Angular compilado en /srv (+ fallback a index.html)
        │             Angular       │
        │  /api/*   → gateway:8080  │  compresión, encabezados de seguridad
        └─────────────┬─────────────┘
          red interna │ (nada más está expuesto)
                      ▼
        ┌───────────────────────────┐
        │ gateway :8080             │  /api/admin/** → servicio-negocio
        │ (Spring Cloud Gateway)    │  /api/rutas/** → servicio-grafos
        └──────┬─────────────┬──────┘
               ▼             ▼
  ┌───────────────────┐   ┌────────────────────┐
  │ servicio-negocio  │◄──│ servicio-grafos    │  lee el grafo en cada cálculo
  │ :8081 JWT, CRUD   │   │ :8082 Dijkstra     │  (GET /interno/grafo, solo en la red interna)
  └─────────┬─────────┘   └────────────────────┘
            ▼
  ┌───────────────────┐
  │ postgis :5432     │  volumen datos-postgis
  └───────────────────┘
```

Frontend y API comparten el origen (el de Caddy): el navegador no necesita CORS. `/interno/**` y
`/actuator/**` no son alcanzables desde afuera: Caddy solo reenvía `/api/*` y el gateway solo
enruta `/api/admin/**` y `/api/rutas/**`.

## Archivos

| Archivo | Qué hace |
|---|---|
| `backend/<servicio>/Dockerfile` | Compila con Gradle (JDK 25) y ejecuta con un JRE 25 Alpine, usuario sin privilegios, capas de Spring Boot separadas, `HEALTHCHECK` contra `/actuator/health` y `JAVA_OPTS` para contenedores. Contexto: `backend/` |
| `backend/.dockerignore` | Deja fuera `build/`, `.gradle`, pruebas, logs y `.env` |
| `frontend/Dockerfile` | Compila Angular en producción (Node 24) y lo sirve con Caddy 2.11 |
| `frontend/.dockerignore` | Deja fuera `node_modules`, `dist`, `.angular` y `.env` |
| `frontend/Caddyfile` | Sitio `{$DOMINIO}`, proxy de `/api/*`, fallback de rutas de Angular, caché, compresión y encabezados de seguridad |
| `backend/*/src/main/resources/application-prod.yml` | Perfil `prod`: logs sin colores, Actuator solo `health`, errores sin detalles internos, pocos hilos; en el gateway, `forward-headers-strategy: framework` |
| `infra/docker-compose.prod.yml` | Los cinco servicios, red interna, volúmenes, salud y orden de arranque, límites de memoria. Imágenes `${REGISTRO}/<servicio>:${ETIQUETA}` |
| `infra/.env.prod.example` | Variables que necesita la composición |
| `infra/cargar-secretos.sh` | Genera `infra/.env.prod` (permisos 600) desde SSM Parameter Store |
| `scripts/probar-prod-local.sh` | Construye las imágenes, levanta la composición y la verifica a través de Caddy |

### Detalles que importan

- **Variables por contenedor.** El archivo de entorno solo alimenta la interpolación de compose;
  cada contenedor recibe en `environment:` únicamente sus variables. PostGIS no ve el secreto
  JWT y los servicios de Java no ven la contraseña de otros.
- **`forward-headers-strategy: framework` en el gateway.** Detrás de Caddy con HTTPS, el gateway
  recibe HTTP; sin esta opción, su filtro CORS ve un esquema distinto al del `Origin` del navegador
  y trata las peticiones POST/PUT/DELETE del mismo origen como CORS. Verificado en local:
  con un origen ajeno en `CORS_ORIGENES`, sin la opción el PUT del editor responde **403**; con la
  opción, pasa. Como respaldo, `CORS_ORIGENES` es el propio origen público (`ORIGEN_PUBLICO`).
- **HTTPS.** `DOMINIO` vacío → HTTP en el puerto 80 (para probar con la IP). Con dominio → Caddy
  obtiene y renueva el certificado de Let's Encrypt (el DNS debe apuntar a la instancia y los
  puertos 80 y 443 deben estar abiertos). Los certificados se guardan en el volumen `caddy-datos`.
  HSTS solo se envía por HTTPS.
- **Caché.** `index.html` (y las rutas de Angular) con `no-cache`, así un despliegue nuevo se ve
  de inmediato; los archivos con hash en el nombre, un año (`immutable`).
- **Imágenes x86 (`linux/amd64`)**, la arquitectura del equipo de construcción y de la t3.small.
- **Constructor de Docker.** Los Dockerfiles no usan funciones exclusivas de BuildKit, así que
  funcionan con el constructor heredado (el de este equipo). Se recomienda instalar
  `docker-buildx` antes de publicar en ECR (`sudo apt install docker-buildx`).

## Memoria (t3.small, 2 GB)

| Contenedor | Límite | Medido en reposo | Pico con carga* | JVM |
|---|---|---|---|---|
| postgis | 320 MB | 70 MiB | 41 MiB | `shared_buffers=64MB`, `max_connections=30` |
| servicio-negocio | 448 MB | 291 MiB | 318 MiB | `MaxRAMPercentage=60` (heap ≤ 269 MB), Hikari 5 conexiones |
| servicio-grafos | 288 MB | 146 MiB | 163 MiB | `MaxRAMPercentage=55` (heap ≤ 158 MB) |
| gateway | 288 MB | 159 MiB | 188 MiB | `MaxRAMPercentage=55` (heap ≤ 158 MB) |
| caddy | 64 MB | 14 MiB | 22 MiB | — |
| **Total** | **1.408 MB** | **≈ 680 MiB** | **≈ 730 MiB** | |

\* 400 rutas con pasos de Dijkstra y 200 lecturas del API de administración, 8 en paralelo, por
HTTPS a través de Caddy. Sin reinicios ni OOM.

Los límites dejan unos 600 MB para el sistema operativo, Docker y la caché de disco. Los JVM usan
SerialGC (menos memoria propia que G1 con heaps pequeños) y `-XX:+ExitOnOutOfMemoryError`: si
alguno se queda sin memoria, termina y `restart: unless-stopped` lo vuelve a levantar.
Recomendación para la instancia: **1 GB de swap** como colchón (no reemplaza los límites).

## Secretos (SSM Parameter Store)

Parámetros bajo `/rutas-accesibles/prod/` (SecureString para los secretos): `REGISTRO`,
`ETIQUETA`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRETO`, `ADMIN_CORREO`,
`ADMIN_CONTRASENA` y, opcionales, `DOMINIO`, `ORIGEN_PUBLICO`, `PUERTO_HTTP`, `PUERTO_HTTPS`.

```bash
infra/cargar-secretos.sh                         # → infra/.env.prod (600)
docker compose -f infra/docker-compose.prod.yml --env-file infra/.env.prod up -d
```

El script toma el último segmento de cada parámetro como nombre de variable, escribe los valores
entre comillas simples (compose los toma literalmente, sin interpretar `$`) y se detiene si falta
uno obligatorio. Como entre comillas simples no hay forma de escapar, rechaza valores con comillas
simples o saltos de línea; los secretos generados con `openssl rand -base64` nunca los tienen.
Nunca imprime los valores.

## Probar en local

```bash
scripts/probar-prod-local.sh             # HTTP en http://localhost
scripts/probar-prod-local.sh --https     # HTTPS en https://localhost (CA interna de Caddy)
scripts/probar-prod-local.sh --mantener  # deja todo corriendo para abrirlo en el navegador
scripts/probar-prod-local.sh --sin-construir  # reutiliza las imágenes ya construidas
```

El script:

1. Construye `rutas-accesibles/{servicio-negocio,servicio-grafos,gateway,frontend}:local`.
2. Genera una vez `logs/prod-local.env` con secretos aleatorios (se reutiliza: PostgreSQL fija la
   contraseña al crear su volumen).
3. Levanta la composición como proyecto `rutas-prod-local` (no toca el entorno de desarrollo).
4. Verifica a través de Caddy: página y rutas de Angular, plano, `/api/rutas/puntos` (16),
   ruta 1→2, ingreso con `Origin`, CRUD con y sin token, PUT del mismo origen sin 403,
   `/actuator` e `/interno` no expuestos, encabezados de seguridad, HSTS solo con HTTPS,
   compresión y caché de `index.html`.
5. Muestra el tamaño de las imágenes y la memoria de cada contenedor, y detiene todo
   (los volúmenes se conservan; `docker compose -p rutas-prod-local -f infra/docker-compose.prod.yml down -v` los borra).

Requiere el puerto 80 (y 443 con `--https`) libre. El desarrollo diario sigue con
`scripts/levantar.sh`.

## Pendiente

- **Content-Security-Policy.** No se puso todavía: MapLibre usa workers (`blob:`) y teselas de
  `tiles.openfreemap.org`, y una CSP incompleta rompe el mapa. Propuesta para probar:
  `default-src 'self'; connect-src 'self' https://tiles.openfreemap.org; img-src 'self' data: blob:; worker-src 'self' blob:; style-src 'self' 'unsafe-inline'`.
- **Copias de seguridad** del volumen `datos-postgis` (por ejemplo, `pg_dump` diario a S3).

## Pasos en AWS (pendiente)

Se completará en la etapa de despliegue: ECR, instancia EC2 t3.small, rol con acceso a SSM y ECR,
grupo de seguridad (80/443), DNS y script de despliegue.
