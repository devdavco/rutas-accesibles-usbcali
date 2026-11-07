# Seguridad del administrador

## Modelo
- **Un solo rol**: el administrador edita puntos y tramos. Las consultas de rutas son públicas.
- **Usuario inicial**: servicio-negocio lo crea al arrancar con `ADMIN_CORREO` y `ADMIN_CONTRASENA`
  si no existe. No hay endpoint de registro. La contraseña se guarda cifrada con BCrypt.
- **Ingreso**: `POST /api/admin/autenticacion/ingresar` devuelve un JWT firmado con HS256
  (`JWT_SECRETO`, mínimo 256 bits; el servicio no arranca sin él) que vence en 2 horas.
  - Se usa el soporte nativo de Spring Security (OAuth2 Resource Server con Nimbus), sin
    librerías JWT de terceros.
  - Si el correo o la contraseña no coinciden, la respuesta es la misma (401, "Correo o
    contraseña incorrectos.") y tarda lo mismo: con un correo inexistente también se compara
    un hash BCrypt.
- **Autorización**: todo `/api/admin/**` exige el token en `Authorization: Bearer …`, salvo el
  ingreso. Siguen públicos `/actuator/health` e `/interno/**`, que en producción no se publica
  fuera de la red de Docker.
- **Sin sesión ni CSRF**: el token viaja en un encabezado, no en una cookie, así que el navegador
  no lo envía solo a otros sitios.
- **CORS** (gateway): solo los orígenes de `CORS_ORIGENES` y los encabezados `Authorization` y
  `Content-Type`.

## Decisiones aceptadas para el alcance de la tesis
1. **Bloqueo por correo en memoria.** Tras 5 ingresos fallidos seguidos para un correo, ese
   correo queda bloqueado 5 minutos (respuesta 429).
   - Un tercero que conozca el correo del administrador puede bloquearlo temporalmente a
     propósito: basta con fallar 5 veces.
   - El estado vive en la memoria del servicio: se pierde al reiniciarlo y no se comparte entre
     varias instancias.
   - El bloqueo se aplica a cualquier correo, exista o no, así que no sirve para averiguar cuáles
     existen.
2. **Token en `sessionStorage`.** El frontend guarda el JWT en `sessionStorage`.
   - Se borra al cerrar la pestaña, pero un script malicioso inyectado en la página (XSS) podría
     leerlo.
   - La alternativa más segura sería una cookie `HttpOnly` con protección CSRF.
   - Mitigaciones actuales: Angular escapa el contenido por defecto, no se inserta HTML de
     terceros y el token vence en 2 horas.
3. **Un único secreto compartido (HS256).** Lo conoce solo servicio-negocio, que emite y
   verifica los tokens. Si otro servicio tuviera que verificarlos, convendría una firma
   asimétrica (RS256).
