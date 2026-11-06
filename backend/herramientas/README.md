# Herramientas

## generar-carga-grafo.mjs

Genera la carga inicial del grafo desde el prototipo de validación
(`../USBCali_Accesible/js/datos.js`, solo lectura). Requiere Node 24 y no usa dependencias.

```bash
cd backend
node herramientas/generar-carga-grafo.mjs                      # usa ../../USBCali_Accesible/js/datos.js
node herramientas/generar-carga-grafo.mjs /ruta/a/datos.js     # otra ubicación
```

Produce (y sobrescribe):

- `servicio-negocio/src/main/resources/db/migration/V3__carga_grafo_prototipo.sql`
- `servicio-negocio/src/test/resources/prototipo/tramos-prototipo.csv` (metros y condiciones del
  prototipo; lo usan las pruebas, porque el CI no tiene el repositorio del prototipo)

`clasificacion-puntos.json` define, para cada id del prototipo, el nombre corregido, el tipo
(`ENTRADA_CAMPUS`, `ENTRADA_EDIFICIO`, `CRUCE`), el edificio y si es `seleccionable` como origen o
destino (falso para cruces y destinos fuera del alcance). El script se detiene con error si un
punto no está clasificado, si su nombre en el prototipo cambió, si hay nombres repetidos o si un
tramo es inválido.

**Si el prototipo cambia:** actualice `clasificacion-puntos.json` y vuelva a generar. Si V3 ya se
aplicó en una base que hay que conservar, el cambio va en una migración nueva (V4…), porque editar
V3 rompe su checksum en Flyway. En una base de desarrollo basta con recrearla.
