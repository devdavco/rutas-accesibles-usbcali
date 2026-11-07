#!/usr/bin/env node
/*
 * Copia el plano del campus del prototipo de validación al frontend, con su calibración.
 *
 * Entradas (solo lectura): plano_campus.png y js/datos.js (plano.esquinas) de ../USBCali_Accesible.
 * Salidas (se sobrescriben en cada ejecución):
 *   - frontend/public/plano/plano_campus.png
 *   - frontend/public/plano/calibracion.json
 *
 * Uso (desde backend/):  node herramientas/copiar-plano.mjs [ruta/a/USBCali_Accesible]
 */
import { copyFileSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const dirBackend = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const dirPrototipo = resolve(process.argv[2] ?? resolve(dirBackend, '../../USBCali_Accesible'));
const dirSalida = resolve(dirBackend, '../frontend/public/plano');

// datos.js se evalúa en un contexto aislado (sin require, fs ni red)
const codigo = readFileSync(resolve(dirPrototipo, 'js/datos.js'), 'utf8');
const plano = vm.runInNewContext(codigo + '\n;DATOS_CAMPUS.plano', {}, { filename: 'datos.js' });
if (!Array.isArray(plano?.esquinas) || plano.esquinas.length !== 4) {
  console.error('Error: js/datos.js no tiene las cuatro esquinas de calibración del plano');
  process.exit(1);
}

mkdirSync(dirSalida, { recursive: true });
copyFileSync(resolve(dirPrototipo, plano.imagen), resolve(dirSalida, 'plano_campus.png'));
writeFileSync(resolve(dirSalida, 'calibracion.json'), JSON.stringify({
  _descripcion: 'Esquinas [lng, lat] del plano: superior izquierda, superior derecha, inferior derecha, '
    + 'inferior izquierda. Copiadas de USBCali_Accesible/js/datos.js por backend/herramientas/copiar-plano.mjs.',
  imagen: 'plano/plano_campus.png',
  esquinas: plano.esquinas,
}, null, 2) + '\n');
console.log(`Plano y calibración copiados a ${dirSalida}`);
