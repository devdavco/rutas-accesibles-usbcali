#!/usr/bin/env bash
# Genera el archivo de entorno de producción a partir de AWS SSM Parameter Store.
#
#   infra/cargar-secretos.sh [ruta] [archivo]
#     ruta:    prefijo de los parámetros (por defecto /rutas-accesibles/prod/)
#     archivo: destino (por defecto infra/.env.prod), con permisos 600
#
# Cada parámetro /rutas-accesibles/prod/NOMBRE se escribe como NOMBRE='valor'. Los valores van
# entre comillas simples para que docker compose los tome literalmente (sin interpretar $); por
# eso no pueden contener comillas simples ni saltos de línea (el script lo verifica).
# Requiere la CLI de AWS con permiso ssm:GetParametersByPath (y kms:Decrypt para SecureString).
# Nunca imprime los valores.
set -euo pipefail

RUTA="${1:-/rutas-accesibles/prod/}"
[[ "$RUTA" == */ ]] || RUTA="$RUTA/"
DESTINO="${2:-$(cd "$(dirname "$0")" && pwd)/.env.prod}"
OBLIGATORIAS=(REGISTRO ETIQUETA POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD JWT_SECRETO ADMIN_CORREO ADMIN_CONTRASENA)

command -v aws > /dev/null || { echo "Falta la CLI de AWS (aws)." >&2; exit 1; }

# Se escribe primero en un temporal con permisos restringidos y luego se mueve
umask 077
TEMPORAL="$(mktemp "${DESTINO}.XXXXXX")"
trap 'rm -f "$TEMPORAL" "$TEMPORAL.crudo"' EXIT

# JSON → líneas NOMBRE<TAB>valor en base64 (así un valor con saltos de línea o tabuladores no
# rompe el formato); la paginación la resuelve la CLI
aws ssm get-parameters-by-path --path "$RUTA" --recursive --with-decryption \
  --query 'Parameters[].[Name, Value]' --output json \
  | python3 -c '
import base64, json, sys
for nombre, valor in json.load(sys.stdin):
    print(nombre.rsplit("/", 1)[-1] + "\t" + base64.b64encode(valor.encode()).decode())
' > "$TEMPORAL.crudo"

encontradas=()
{
  echo "# Generado por infra/cargar-secretos.sh desde $RUTA el $(date -u +%Y-%m-%dT%H:%M:%SZ). No editar ni subir a git."
  while IFS=$'\t' read -r nombre valor64; do
    if [[ ! "$nombre" =~ ^[A-Z_][A-Z0-9_]*$ ]]; then
      echo "Se ignora el parámetro «$nombre»: no es un nombre de variable válido." >&2
      continue
    fi
    valor="$(printf '%s' "$valor64" | base64 -d)"
    # Entre comillas simples compose toma el valor literal, pero no admite ningún escape: un valor
    # con comilla simple o salto de línea no se puede representar y se rechaza
    if [[ "$valor" == *"'"* || "$valor" == *$'\n'* ]]; then
      echo "El valor de $nombre contiene comillas simples o saltos de línea; cámbielo en SSM." >&2
      exit 1
    fi
    printf "%s='%s'\n" "$nombre" "$valor"
    encontradas+=("$nombre")
  done < "$TEMPORAL.crudo"
} > "$TEMPORAL"
rm -f "$TEMPORAL.crudo"

faltan=()
for variable in "${OBLIGATORIAS[@]}"; do
  [[ " ${encontradas[*]} " == *" $variable "* ]] || faltan+=("$variable")
done
if (( ${#faltan[@]} > 0 )); then
  echo "Faltan parámetros en $RUTA: ${faltan[*]}" >&2
  exit 1
fi

chmod 600 "$TEMPORAL"
mv "$TEMPORAL" "$DESTINO"
trap - EXIT
echo "Escrito $DESTINO con ${#encontradas[@]} variables (permisos 600)."
