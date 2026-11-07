#!/usr/bin/env bash
# Detiene lo que arrancó scripts/levantar.sh. La base de datos se detiene sin borrar sus datos.
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$RAIZ/logs"

for nombre in frontend gateway servicio-grafos servicio-negocio; do
  pid="$LOGS/$nombre.pid"
  if [[ -f "$pid" ]]; then
    if kill -0 "$(cat "$pid")" 2> /dev/null; then
      # Se detiene todo el grupo de procesos (npm start lanza ng serve como hijo)
      kill -TERM -- "-$(cat "$pid")" 2> /dev/null || kill "$(cat "$pid")" 2> /dev/null || true
      echo "$nombre detenido"
    fi
    rm -f "$pid"
  fi
done

docker compose -f "$RAIZ/infra/docker-compose.yml" stop
