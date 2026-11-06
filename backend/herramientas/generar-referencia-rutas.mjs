#!/usr/bin/env node
/*
 * Genera los resultados esperados de la prueba de extremo a extremo ejecutando el
 * Dijkstra del prototipo de validación (js/dijkstra.js) sobre su propio grafo (js/datos.js).
 *
 * El prototipo se lee en solo lectura y se ejecuta en un contexto aislado (sin require, fs
 * ni red). Su código no se porta: solo se usa para obtener la ruta y los metros de referencia.
 *
 * Salida (se sobrescribe en cada ejecución):
 *   - servicio-grafos/src/test/resources/prototipo/rutas-esperadas.json
 *
 * Uso (desde backend/):  node herramientas/generar-referencia-rutas.mjs [ruta/a/USBCali_Accesible]
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const dirHerramientas = dirname(fileURLToPath(import.meta.url));
const dirBackend = resolve(dirHerramientas, '..');
const dirPrototipo = resolve(process.argv[2] ?? resolve(dirBackend, '../../USBCali_Accesible'));
const rutaSalida = resolve(dirBackend, 'servicio-grafos/src/test/resources/prototipo/rutas-esperadas.json');

// 15 pares: las dos entradas del campus y al menos un punto de cada edificio del alcance
const PARES = [
  // Desde la Entrada Principal
  [1, 2], [1, 5], [1, 6], [1, 7], [1, 9], [1, 11], [1, 39], [1, 42],
  // Desde la Entrada de Estudiantes
  [28, 3], [28, 20], [28, 32], [28, 44], [28, 49],
  // Entre edificios
  [49, 6], [42, 11],
];

// 1. Cargar datos.js y dijkstra.js en el mismo contexto aislado
const codigo = ['js/datos.js', 'js/dijkstra.js']
  .map((archivo) => readFileSync(resolve(dirPrototipo, archivo), 'utf8'))
  .join('\n;\n');
const prototipo = vm.runInNewContext(
  codigo + '\n;({ datos: DATOS_CAMPUS, construirGrafo: construirGrafo, dijkstra: dijkstra })',
  {}, { filename: 'prototipo' });

// 2. Ejecutar el Dijkstra del prototipo para cada par
const grafo = prototipo.construirGrafo(prototipo.datos.puntos, prototipo.datos.tramos);
const rutas = PARES.map(([origen, destino]) => {
  const resultado = prototipo.dijkstra(grafo, origen, destino);
  if (resultado.ruta.length === 0) {
    console.error(`Error: el prototipo no encuentra ruta de ${origen} a ${destino}`);
    process.exit(1);
  }
  return {
    origen,
    destino,
    origenNombre: grafo.nombres.get(origen),
    destinoNombre: grafo.nombres.get(destino),
    ruta: Array.from(resultado.ruta),
    metros: resultado.metrosTotal,
    costo: resultado.costoTotal,
  };
});

// 3. Guardar el fixture (los nombres son los originales del prototipo, solo como referencia)
mkdirSync(dirname(rutaSalida), { recursive: true });
writeFileSync(rutaSalida, JSON.stringify({
  _descripcion: 'Resultados del Dijkstra del prototipo (js/dijkstra.js) para la prueba de extremo a extremo. '
    + 'Generado por backend/herramientas/generar-referencia-rutas.mjs: no editar a mano.',
  rutas,
}, null, 2) + '\n');
console.log(`Generadas ${rutas.length} rutas de referencia en ${rutaSalida}`);
