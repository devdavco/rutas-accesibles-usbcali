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
| `infra/docker-compose.prod.yml` | Los cinco servicios, red interna, volúmenes, salud y orden de arranque, límites de memoria. Imágenes `${REGISTRO}/rutas-accesibles/<servicio>:${ETIQUETA}` (`REGISTRO` es solo el host del registro) |
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
  funcionan con el constructor heredado (el de este equipo) y con BuildKit (el de GitHub Actions,
  que es quien publica en ECR).
- **Nombres de imagen.** Los mismos en todas partes: `${REGISTRO}/rutas-accesibles/<servicio>:${ETIQUETA}`
  con `<servicio>` ∈ {`servicio-negocio`, `servicio-grafos`, `gateway`, `frontend`}.
  En ECR, `REGISTRO=248033171214.dkr.ecr.us-east-1.amazonaws.com` y `ETIQUETA` = SHA del commit;
  en la prueba local, `REGISTRO=localhost` y `ETIQUETA=local`.

## Memoria (t3.small, 2 GB)

| Contenedor | Límite | Medido en reposo | Pico con carga* | JVM |
|---|---|---|---|---|
| postgis | 320 MB | 70 MiB | 41 MiB | `shared_buffers=64MB`, `max_connections=30` |
| servicio-negocio | 448 MB | 291 MiB | 318 MiB | `MaxRAMPercentage=60` (heap ≤ 269 MB), Hikari 5 conexiones |
| servicio-grafos | 288 MB | 146 MiB | 163 MiB | `MaxRAMPercentage=55` (heap ≤ 158 MB) |
| gateway | 288 MB | 159 MiB | 188 MiB | `MaxRAMPercentage=55` (heap ≤ 158 MB) |
| caddy | 96 MB | 14–54 MiB† | 22 MiB | — |
| **Total** | **1.440 MB** | **≈ 680 MiB** | **≈ 730 MiB** | |

† Caddy mide 14 MiB recién iniciado y hasta 54 MiB después de servir el frontend: la diferencia
es caché de archivos que el kernel le cuenta al contenedor (recuperable). Por eso su límite es
96 MB y no 64.

\* 400 rutas con pasos de Dijkstra y 200 lecturas del API de administración, 8 en paralelo, por
HTTPS a través de Caddy. Sin reinicios ni OOM.

Los límites dejan unos 600 MB para el sistema operativo, Docker y la caché de disco. Los JVM usan
SerialGC (menos memoria propia que G1 con heaps pequeños) y `-XX:+ExitOnOutOfMemoryError`: si
alguno se queda sin memoria, termina y `restart: unless-stopped` lo vuelve a levantar.
Recomendación para la instancia: **1 GB de swap** como colchón (no reemplaza los límites).

## Secretos (SSM Parameter Store)

Parámetros bajo `/rutas-accesibles/prod/` (SecureString para los secretos): `REGISTRO` (solo el
host del registro), `ETIQUETA` (SHA publicado), `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
`JWT_SECRETO`, `ADMIN_CORREO`, `ADMIN_CONTRASENA` y, opcionales, `DOMINIO`, `ORIGEN_PUBLICO`,
`PUERTO_HTTP`, `PUERTO_HTTPS`.

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

1. Construye `localhost/rutas-accesibles/{servicio-negocio,servicio-grafos,gateway,frontend}:local` (los mismos nombres que en ECR, con `REGISTRO=localhost`).
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

## Pasos en AWS

Configurado en la consola de AWS (cuenta 248033171214, región us-east-1). Aquí no hay ningún valor
secreto: los secretos viven solo en SSM Parameter Store.

### Cuenta
- **MFA** activado en el usuario raíz; la raíz no se usa para el trabajo diario.
- **IAM Identity Center** para el acceso diario a la consola y a la CLI.
- **Presupuesto** de USD 25 al mes con alertas por correo.

### Amazon ECR
- Registro `248033171214.dkr.ecr.us-east-1.amazonaws.com` con 4 repositorios:
  `rutas-accesibles/servicio-negocio`, `rutas-accesibles/servicio-grafos`,
  `rutas-accesibles/gateway` y `rutas-accesibles/frontend`.
- **Etiquetas inmutables**: una etiqueta (el SHA del commit) siempre apunta a la misma imagen; no
  hay `latest`.
- **Escaneo al subir** (vulnerabilidades conocidas de la imagen).
- **Lifecycle policy** para borrar imágenes antiguas y no acumular almacenamiento.

### Publicación desde GitHub Actions (OIDC, sin llaves)
- **Proveedor de identidad OIDC** `token.actions.githubusercontent.com` (audiencia `sts.amazonaws.com`).
- **Rol `github-actions-ecr-push`**:
  - trust policy (`sts:AssumeRoleWithWebIdentity`): `aud` = `sts.amazonaws.com` y `sub` =
    `repo:devdavco@98057946/rutas-accesibles-usbcali@1448704103:ref:refs/heads/main`.
    Solo lo asume un job de la rama `main` de este repositorio (ni otras ramas, ni pull requests,
    ni otros repositorios).
    - **Formato del `sub` con identificadores inmutables.** Este repositorio emite el `sub` con el
      ID de la cuenta (`devdavco@98057946`) y del repositorio (`rutas-accesibles-usbcali@1448704103`),
      no con el formato antiguo `repo:devdavco/rutas-accesibles-usbcali:…`. Se puede consultar en
      `https://api.github.com/repos/devdavco/rutas-accesibles-usbcali/actions/oidc/customization/sub`
      (`sub_claim_prefix`). Con el formato antiguo en la trust policy, el job falla con
      `Not authorized to perform sts:AssumeRoleWithWebIdentity`. Los IDs además protegen contra
      que alguien recree una cuenta o repositorio con el mismo nombre y asuma el rol.
  - política en línea `subir-imagenes-ecr`: obtener el token de ECR, subir capas e imágenes y
    leerlas (`ecr:DescribeImages`, `ecr:BatchGetImage`) en los 4 repositorios.
- **Workflow** (`.github/workflows/ci.yml`, job `publicar-imagenes`): corre solo en push a `main`,
  después de que pasan las pruebas del backend y del frontend; construye las 4 imágenes
  (`linux/amd64`, caché de GitHub Actions por imagen) y las sube con la etiqueta `github.sha`.
  Si el workflow se repite para el mismo commit, se salta lo ya publicado (las etiquetas son
  inmutables). El resumen del job muestra la etiqueta y los 4 URIs.

### Instancia
- **Rol de instancia `ec2-rutas-accesibles`**:
  - `AmazonEC2ContainerRegistryPullOnly`: descargar las imágenes de ECR;
  - `AmazonSSMManagedInstanceCore`: Session Manager (consola sin SSH);
  - política en línea `leer-parametros-prod`: leer y descifrar solo `/rutas-accesibles/prod/*`.
- **Parámetros** en `/rutas-accesibles/prod/` (8):

  | Parámetro | Tipo |
  |---|---|
  | `REGISTRO` | String |
  | `ETIQUETA` | String |
  | `POSTGRES_DB` | String |
  | `POSTGRES_USER` | String |
  | `POSTGRES_PASSWORD` | SecureString |
  | `JWT_SECRETO` | SecureString |
  | `ADMIN_CORREO` | String |
  | `ADMIN_CONTRASENA` | SecureString |

- **Grupo de seguridad `rutas-accesibles-web`**: entrada solo por 80 y 443. Sin SSH: el acceso
  administrativo es por Session Manager.
- **EC2 t3.small** (x86, 2 vCPU, 2 GB) con **Amazon Linux 2023**, **IMDSv2** obligatorio y disco
  **gp3 de 20 GB cifrado**.
- **User data**: instala Docker y Docker Compose, y crea **1 GB de swap** con `dd`.
- **IP elástica** `100.49.209.43`.

### Flujo de una versión nueva
1. Push a `main` → CI (pruebas) → `publicar-imagenes` sube las 4 imágenes con el SHA.
2. Actualizar la etiqueta en SSM (el comando aparece en el resumen del job):
   `aws ssm put-parameter --name /rutas-accesibles/prod/ETIQUETA --value <SHA> --overwrite`.
3. En la instancia (Session Manager): `infra/cargar-secretos.sh` y
   `docker compose -f infra/docker-compose.prod.yml --env-file infra/.env.prod up -d`.
   El script de despliegue que automatiza este paso queda para la etapa siguiente.
