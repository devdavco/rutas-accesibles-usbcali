#!/usr/bin/env node
/*
 * Genera la carga inicial del grafo a partir del prototipo de validación.
 *
 * Entradas:
 *   - js/datos.js del prototipo (../USBCali_Accesible, solo lectura)
 *   - clasificacion-puntos.json (nombre corregido, tipo, edificio y alcance de cada punto)
 *
 * Salidas (se sobrescriben en cada ejecución):
 *   - servicio-negocio/src/main/resources/db/migration/V3__carga_grafo_prototipo.sql
 *   - servicio-negocio/src/test/resources/prototipo/tramos-prototipo.csv
 *
 * Uso (desde backend/):  node herramientas/generar-carga-grafo.mjs [ruta/a/datos.js]
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const dirHerramientas = dirname(fileURLToPath(import.meta.url));
const dirBackend = resolve(dirHerramientas, '..');
const rutaDatos = resolve(process.argv[2] ?? resolve(dirBackend, '../../USBCali_Accesible/js/datos.js'));
const rutaSql = resolve(dirBackend, 'servicio-negocio/src/main/resources/db/migration/V3__carga_grafo_prototipo.sql');
const rutaCsv = resolve(dirBackend, 'servicio-negocio/src/test/resources/prototipo/tramos-prototipo.csv');

// Condiciones del prototipo → condiciones del modelo
const CONDICIONES = { plano: 'PLANO', rampa: 'RAMPA', pendiente: 'PENDIENTE_FUERTE', escaleras: 'ESCALERAS' };
const TIPOS = ['ENTRADA_CAMPUS', 'ENTRADA_EDIFICIO', 'CRUCE'];

function fallar(mensaje) {
  console.error('Error: ' + mensaje);
  process.exit(1);
}

// 1. Leer datos.js en un contexto aislado (sin require, fs ni red) y obtener DATOS_CAMPUS
const codigo = readFileSync(rutaDatos, 'utf8');
const datos = vm.runInNewContext(codigo + '\n;DATOS_CAMPUS', {}, { filename: rutaDatos });
const clasificacion = JSON.parse(readFileSync(resolve(dirHerramientas, 'clasificacion-puntos.json'), 'utf8')).puntos;

// 2. Validar puntos contra la clasificación
const ids = new Set();
for (const p of datos.puntos) {
  if (ids.has(p.id)) fallar(`id de punto repetido: ${p.id}`);
  ids.add(p.id);
  const c = clasificacion[p.id];
  if (!c) fallar(`el punto ${p.id} ("${p.nombre}") no está en clasificacion-puntos.json`);
  if (c.nombreOriginal !== p.nombre) fallar(`el punto ${p.id} cambió de nombre en el prototipo: "${p.nombre}"; revise la clasificación`);
  if (!TIPOS.includes(c.tipo)) fallar(`tipo inválido en el punto ${p.id}: ${c.tipo}`);
  if ((c.tipo === 'CRUCE') !== (p.tipo === 'cruce')) fallar(`el punto ${p.id} es "${p.tipo}" en el prototipo pero ${c.tipo} en la clasificación`);
}
for (const id of Object.keys(clasificacion)) {
  if (!ids.has(Number(id))) fallar(`la clasificación tiene el punto ${id}, que no existe en el prototipo`);
}
const nombres = datos.puntos.map((p) => clasificacion[p.id].nombre);
const repetidos = nombres.filter((n, i) => nombres.indexOf(n) !== i);
if (repetidos.length) fallar(`nombres repetidos: ${repetidos.join(', ')}`);

// 3. Validar tramos
const pares = new Set();
for (const t of datos.tramos) {
  if (!ids.has(t.desde) || !ids.has(t.hasta)) fallar(`tramo ${t.desde}-${t.hasta} apunta a un punto que no existe`);
  if (t.desde === t.hasta) fallar(`tramo de un punto a sí mismo: ${t.desde}`);
  if (!CONDICIONES[t.condicion]) fallar(`condición desconocida en ${t.desde}-${t.hasta}: ${t.condicion}`);
  const par = [Math.min(t.desde, t.hasta), Math.max(t.desde, t.hasta)].join('-');
  if (pares.has(par)) fallar(`tramo duplicado: ${par}`);
  pares.add(par);
}

// 4. Generar el SQL
const texto = (v) => (v === null ? 'NULL' : `'${String(v).replaceAll("'", "''")}'`);
const filasPuntos = datos.puntos.map((p) => {
  const c = clasificacion[p.id];
  return `    (${p.id}, ${texto(c.nombre)}, '${c.tipo}', ${texto(c.edificio)}, ${c.seleccionable}, ST_SetSRID(ST_MakePoint(${p.lng}, ${p.lat}), 4326))`;
});
const filasTramos = datos.tramos.map((t) => `    (${t.desde}, ${t.hasta}, '${CONDICIONES[t.condicion]}')`);

const sql = `-- ARCHIVO GENERADO por backend/herramientas/generar-carga-grafo.mjs: no editar a mano.
-- Carga el grafo del prototipo de validación (USBCali_Accesible/js/datos.js):
-- ${datos.puntos.length} puntos y ${datos.tramos.length} tramos. Los nombres van corregidos según clasificacion-puntos.json.

-- Puntos: se conserva el id del prototipo para poder rastrear cada punto
INSERT INTO punto (id, nombre, tipo, edificio, seleccionable, ubicacion) OVERRIDING SYSTEM VALUE VALUES
${filasPuntos.join(',\n')};

-- Tramos: la longitud no se envía, la calcula el trigger con PostGIS
INSERT INTO tramo (punto_inicial_id, punto_final_id, condicion) VALUES
${filasTramos.join(',\n')};

-- Las secuencias continúan después del último id cargado
SELECT setval(pg_get_serial_sequence('punto', 'id'), (SELECT max(id) FROM punto));
`;

// 5. Generar el CSV de referencia para las pruebas (el CI no tiene el repositorio del prototipo)
const csv = ['desde,hasta,metros,condicion']
  .concat(datos.tramos.map((t) => `${t.desde},${t.hasta},${t.metros},${CONDICIONES[t.condicion]}`))
  .join('\n') + '\n';

writeFileSync(rutaSql, sql);
mkdirSync(dirname(rutaCsv), { recursive: true });
writeFileSync(rutaCsv, csv);
console.log(`Generados ${datos.puntos.length} puntos y ${datos.tramos.length} tramos:\n  ${rutaSql}\n  ${rutaCsv}`);
