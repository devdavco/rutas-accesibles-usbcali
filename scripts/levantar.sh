#!/usr/bin/env bash
# Levanta todo para desarrollo: base de datos, servicio-negocio, servicio-grafos, gateway y frontend.
# Los servicios quedan en segundo plano; sus logs y PID van en logs/. Para detener: scripts/detener.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$RAIZ/logs"
mkdir -p "$LOGS"

if [[ ! -f "$RAIZ/infra/.env" ]]; then
  echo "Falta infra/.env. Cópielo de infra/.env.example y complete las credenciales." >&2
  exit 1
fi
set -a
# shellcheck source=/dev/null
source "$RAIZ/infra/.env"
set +a

# servicio-negocio no arranca sin estas variables (ver infra/.env.example)
for variable in JWT_SECRETO ADMIN_CORREO ADMIN_CONTRASENA; do
  if [[ -z "${!variable:-}" ]]; then
    echo "Falta $variable en infra/.env (ver infra/.env.example)." >&2
    exit 1
  fi
done

# Espera hasta que una URL responda (máximo $3 segundos). Si no responde, muestra el final del log.
esperar() {
  local nombre="$1" url="$2" limite="${3:-90}" log="$LOGS/$1.log" pid="$LOGS/$1.pid"
  for ((i = 0; i < limite; i++)); do
    # Si el proceso terminó (por ejemplo, el puerto ya estaba ocupado), responder en la URL no basta
    if [[ -f "$pid" ]] && ! kill -0 "$(cat "$pid")" 2> /dev/null; then
      echo "  $nombre terminó al arrancar. Últimas líneas de $log:" >&2
      tail -n 30 "$log" >&2
      exit 1
    fi
    if curl -sf "$url" > /dev/null; then
      echo "  $nombre listo"
      return 0
    fi
    sleep 1
  done
  echo "  $nombre no respondió en $limite s. Últimas líneas de $log:" >&2
  tail -n 30 "$log" >&2
  exit 1
}

# Arranca un proceso en segundo plano si no está corriendo ya.
arrancar() {
  local nombre="$1"; shift
  local pid="$LOGS/$nombre.pid"
  if [[ -f "$pid" ]] && kill -0 "$(cat "$pid")" 2> /dev/null; then
    echo "  $nombre ya está corriendo (PID $(cat "$pid"))"
    return 0
  fi
  # setsid: cada proceso queda en su propio grupo, así detener.sh puede cerrar también sus hijos
  setsid nohup "$@" > "$LOGS/$nombre.log" 2>&1 < /dev/null &
  echo $! > "$pid"
}

echo "1/5 Base de datos (PostGIS)"
docker compose -f "$RAIZ/infra/docker-compose.yml" up -d --wait

echo "2/5 Compilando los servicios"
(cd "$RAIZ/backend" && ./gradlew -q bootJar)

echo "3/5 servicio-negocio (8081)"
DB_USER="$POSTGRES_USER" DB_PASSWORD="$POSTGRES_PASSWORD" DB_PORT="${DB_PORT:-5432}" \
  JWT_SECRETO="$JWT_SECRETO" ADMIN_CORREO="$ADMIN_CORREO" ADMIN_CONTRASENA="$ADMIN_CONTRASENA" \
  arrancar servicio-negocio java -jar "$RAIZ/backend/servicio-negocio/build/libs/servicio-negocio-0.0.1-SNAPSHOT.jar"
esperar servicio-negocio http://localhost:8081/actuator/health

echo "4/5 servicio-grafos (8082) y gateway (8080)"
arrancar servicio-grafos java -jar "$RAIZ/backend/servicio-grafos/build/libs/servicio-grafos-0.0.1-SNAPSHOT.jar"
esperar servicio-grafos http://localhost:8082/actuator/health
arrancar gateway java -jar "$RAIZ/backend/gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar"
esperar gateway http://localhost:8080/actuator/health

echo "5/5 Frontend (4200)"
if [[ ! -d "$RAIZ/frontend/node_modules" ]]; then
  (cd "$RAIZ/frontend" && npm ci)
fi
(cd "$RAIZ/frontend" && arrancar frontend npm start)
esperar frontend http://localhost:4200 120

echo
echo "Todo listo: http://localhost:4200  (API: http://localhost:8080). Logs en logs/."
