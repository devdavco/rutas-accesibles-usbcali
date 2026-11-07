# Imagen del frontend: Angular compilado en producción, servido por Caddy, que además es la
# puerta de entrada única (proxy de /api/* hacia el gateway).
# Contexto: frontend/  →  docker build -f frontend/Dockerfile -t <imagen> frontend

# --- 1. Compilación de Angular -----------------------------------------------------------
FROM node:24-alpine AS compilacion
WORKDIR /fuente
# Dependencias primero: la capa se reutiliza mientras no cambie package-lock.json
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# --- 2. Caddy sirviendo los archivos estáticos --------------------------------------------
FROM caddy:2.11-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=compilacion /fuente/dist/frontend/browser /srv
EXPOSE 80 443
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO /dev/null http://127.0.0.1:2019/config/ || exit 1
