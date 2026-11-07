#!/usr/bin/env bash
# Prueba la composición de producción en local: construye las cuatro imágenes, levanta
# infra/docker-compose.prod.yml y verifica todo a través de Caddy, como lo vería el navegador.
#
#   scripts/probar-prod-local.sh [--https] [--mantener] [--sin-construir]
#     --https          Caddy con DOMINIO=localhost (HTTPS con su CA interna) en vez de HTTP :80
#     --mantener       deja los contenedores corriendo al terminar
#     --sin-construir  usa las imágenes ya construidas
#
# Usa el proyecto de compose "rutas-prod-local": no toca el entorno de desarrollo
# (scripts/levantar.sh), que sigue funcionando igual.
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
PROYECTO=rutas-prod-local
# Las imágenes se llaman como en ECR: ${REGISTRO}/rutas-accesibles/<servicio>:${ETIQUETA}.
# REGISTRO=localhost: Docker las toma como locales y no intenta descargarlas
REGISTRO=localhost
PREFIJO=rutas-accesibles
ETIQUETA=local
ENTORNO="$RAIZ/logs/prod-local.env"
HTTPS=false MANTENER=false CONSTRUIR=true
for opcion in "$@"; do
  case "$opcion" in
    --https) HTTPS=true ;;
    --mantener) MANTENER=true ;;
    --sin-construir) CONSTRUIR=false ;;
    *) echo "Opción desconocida: $opcion" >&2; exit 2 ;;
  esac
done

compose() {
  docker compose -p "$PROYECTO" -f "$RAIZ/infra/docker-compose.prod.yml" --env-file "$ENTORNO" "$@"
}

fallos=0
comprobar() { # comprobar "descripción" comando...
  if "${@:2}" > /dev/null 2>&1; then
    echo "  ✔ $1"
  else
    echo "  ✘ $1" >&2
    fallos=$((fallos + 1))
  fi
}

# --- 1. Imágenes -------------------------------------------------------------------------
if $CONSTRUIR; then
  echo "1/4 Construyendo imágenes ($REGISTRO/$PREFIJO/*:$ETIQUETA, linux/amd64)"
  for servicio in servicio-negocio servicio-grafos gateway; do
    echo "  · $servicio"
    docker build -q --platform linux/amd64 -t "$REGISTRO/$PREFIJO/$servicio:$ETIQUETA" \
      -f "$RAIZ/backend/$servicio/Dockerfile" "$RAIZ/backend" > /dev/null
  done
  echo "  · frontend (Caddy)"
  docker build -q --platform linux/amd64 -t "$REGISTRO/$PREFIJO/frontend:$ETIQUETA" "$RAIZ/frontend" > /dev/null
else
  echo "1/4 Usando las imágenes ya construidas"
fi

# --- 2. Entorno de prueba ----------------------------------------------------------------
# Se genera una sola vez y se reutiliza: PostgreSQL fija la contraseña al crear su volumen
mkdir -p "$RAIZ/logs"
if [[ ! -f "$ENTORNO" ]]; then
  (
    umask 077
    cat > "$ENTORNO" <<EOF
# Entorno de la prueba local de producción (scripts/probar-prod-local.sh). No se sube a git.
REGISTRO=$REGISTRO
ETIQUETA=$ETIQUETA
POSTGRES_DB=rutas
POSTGRES_USER=rutas
POSTGRES_PASSWORD='$(openssl rand -hex 24)'
JWT_SECRETO='$(openssl rand -base64 48 | tr -d '\n')'
ADMIN_CORREO=admin@usbcali.edu.co
ADMIN_CONTRASENA='$(openssl rand -hex 12)'
EOF
  )
fi
# Entornos generados antes con la convención anterior (REGISTRO=rutas-accesibles): se corrige
# solo esa línea, sin tocar los secretos (PostgreSQL ya fijó su contraseña)
sed -i "s|^REGISTRO=.*|REGISTRO=$REGISTRO|" "$ENTORNO"
# DOMINIO y ORIGEN_PUBLICO dependen del modo (se pasan por el entorno, que tiene prioridad)
if $HTTPS; then
  export DOMINIO=localhost ORIGEN_PUBLICO=https://localhost
  BASE=https://localhost
else
  export DOMINIO=:80 ORIGEN_PUBLICO=http://localhost
  BASE=http://localhost
fi
CURL=(curl -sS --max-time 20 -k)
CONTRASENA="$(sed -n "s/^ADMIN_CONTRASENA='\(.*\)'$/\1/p" "$ENTORNO")"

# --- 3. Levantar -------------------------------------------------------------------------
echo "2/4 Levantando la composición de producción ($BASE)"
compose up -d --wait --wait-timeout 240

# --- 4. Verificar a través de Caddy ------------------------------------------------------
echo "3/4 Verificando a través de Caddy"
comprobar "la página carga (index.html con <app-root>)" \
  bash -c "${CURL[*]} $BASE/ | grep -q '<app-root'"
comprobar "una ruta de Angular (/admin/editor) también entrega index.html" \
  bash -c "${CURL[*]} $BASE/admin/editor | grep -q '<app-root'"
comprobar "el plano del campus se sirve" \
  bash -c "[[ \$(${CURL[*]} -o /dev/null -w '%{http_code}' $BASE/plano/calibracion.json) == 200 ]]"
comprobar "/api/rutas/puntos responde con los 16 puntos seleccionables" \
  bash -c "${CURL[*]} $BASE/api/rutas/puntos | python3 -c 'import json,sys; assert len(json.load(sys.stdin)) == 16'"
comprobar "/api/rutas calcula la ruta 1 → 2" \
  bash -c "${CURL[*]} '$BASE/api/rutas?origen=1&destino=2' | grep -q '\"encontrada\":true'"

# El ingreso lleva Origin, como el navegador: así se prueba que el gateway no lo trate como CORS
TOKEN="$("${CURL[@]}" -H "Origin: $ORIGEN_PUBLICO" -H 'Content-Type: application/json' \
  -d "{\"correo\":\"admin@usbcali.edu.co\",\"contrasena\":\"$CONTRASENA\"}" \
  "$BASE/api/admin/autenticacion/ingresar" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("token",""))' || true)"
comprobar "el ingreso del administrador entrega un token (con encabezado Origin)" test -n "$TOKEN"
comprobar "/api/admin/puntos con el token responde 200" \
  bash -c "[[ \$(${CURL[*]} -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $TOKEN' $BASE/api/admin/puntos) == 200 ]]"
comprobar "/api/admin/puntos sin token responde 401" \
  bash -c "[[ \$(${CURL[*]} -o /dev/null -w '%{http_code}' $BASE/api/admin/puntos) == 401 ]]"
comprobar "un PUT del mismo origen pasa el CORS del gateway (no es 403)" \
  bash -c "[[ \$(${CURL[*]} -o /dev/null -w '%{http_code}' -X PUT -H 'Origin: $ORIGEN_PUBLICO' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{\"condicion\":\"PLANO\"}' $BASE/api/admin/tramos/999999) == 404 ]]"
comprobar "/actuator/health no se expone afuera" \
  bash -c "! ${CURL[*]} $BASE/actuator/health | grep -q '\"UP\"'"
comprobar "/interno/grafo no se expone afuera" \
  bash -c "! ${CURL[*]} $BASE/interno/grafo | grep -q '\"tramos\"'"
comprobar "encabezados de seguridad (nosniff, Referrer-Policy)" \
  bash -c "${CURL[*]} -D - -o /dev/null $BASE/ | grep -qi 'x-content-type-options: nosniff' && ${CURL[*]} -D - -o /dev/null $BASE/ | grep -qi 'referrer-policy'"
if $HTTPS; then
  comprobar "HSTS presente con HTTPS" bash -c "${CURL[*]} -D - -o /dev/null $BASE/ | grep -qi 'strict-transport-security'"
else
  comprobar "sin HSTS cuando se sirve por HTTP" bash -c "! ${CURL[*]} -D - -o /dev/null $BASE/ | grep -qi 'strict-transport-security'"
fi
comprobar "compresión de los archivos estáticos" \
  bash -c "${CURL[*]} -H 'Accept-Encoding: gzip, zstd' -D - -o /dev/null $BASE/ | grep -qiE 'content-encoding: (gzip|zstd)'"
comprobar "index.html sin caché (Cache-Control: no-cache)" \
  bash -c "${CURL[*]} -D - -o /dev/null $BASE/admin/editor | grep -qi 'cache-control: no-cache'"

# --- 5. Tamaños y memoria ----------------------------------------------------------------
echo "4/4 Imágenes y memoria"
docker image ls --format 'table {{.Repository}}:{{.Tag}}\t{{.Size}}' | grep -E "^(REPOSITORY|$REGISTRO/$PREFIJO/)" || true
docker image ls --format '{{.Repository}}:{{.Tag}}\t{{.Size}}' postgis/postgis:18-3.7 || true
echo
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.CPUPerc}}' \
  $(compose ps -q)

if ! $MANTENER; then
  echo
  echo "Deteniendo (los volúmenes se conservan; para borrarlos: docker compose -p $PROYECTO -f infra/docker-compose.prod.yml down -v)"
  compose down
fi

if (( fallos > 0 )); then
  echo "$fallos comprobaciones fallaron." >&2
  exit 1
fi
echo "Todas las comprobaciones pasaron."
