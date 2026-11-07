import { isDevMode } from '@angular/core';
import type { Map as MapaLibre, Marker } from 'maplibre-gl';
import type { FeatureCollection } from 'geojson';
import { mitadesDelPlano, type Esquinas } from '../../dominio/plano';

/**
 * Base común de los mapas del campus (consulta de rutas y editor del grafo): mapa base de
 * OpenFreeMap, plano del campus calibrado y vista limitada al campus.
 */

export type Coordenada = [number, number];

export interface MapaCampus {
  mapa: MapaLibre;
  /** Ids de las capas del mapa base (las que oculta "Solo plano"). */
  capasBase: string[];
  /** Caja [[oeste, sur], [este, norte]] del plano. */
  limites: [Coordenada, Coordenada];
  crearMarcador: (elemento: HTMLElement, opciones: ConstructorParameters<typeof Marker>[0]) => Marker;
}

interface Calibracion {
  imagen: string;
  esquinas: Esquinas;
}

const ESTILO_BASE = 'https://tiles.openfreemap.org/styles/positron';

/** Margen alrededor del plano dentro del cual se puede mover la vista (grados). */
const MARGEN_VISTA = 0.004;

/** Píxeles que cada mitad del plano se extiende más allá de la diagonal que las separa. */
const SOLAPE_PX = 2;

export const COLECCION_VACIA: FeatureCollection = { type: 'FeatureCollection', features: [] };

export const SIN_MOVIMIENTO = matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Duración de las animaciones del mapa (0 si el usuario prefiere reducir el movimiento). */
export function duracion(): number {
  return SIN_MOVIMIENTO ? 0 : 500;
}

/** Lee un token de diseño (MapLibre no entiende var(--…)). */
export function token(nombre: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(nombre).trim();
}

export function medida(nombre: string): number {
  return parseFloat(token(nombre));
}

/**
 * Crea el mapa en el contenedor y espera a que el mapa base y el plano estén listos.
 * MapLibre se carga bajo demanda: no pesa en el arranque de la página.
 */
export async function crearMapaCampus(contenedor: HTMLElement, etiquetaAccesible: string): Promise<MapaCampus> {
  const [maplibre, calibracion] = await Promise.all([
    import('maplibre-gl'),
    fetch('plano/calibracion.json').then((r) => r.json() as Promise<Calibracion>),
  ]);
  const imagenPlano = new Image();
  imagenPlano.src = calibracion.imagen;
  const planoListo = imagenPlano.decode();
  maplibre.setWorkerUrl(new URL('maplibre/maplibre-gl-worker.mjs', document.baseURI).href);

  const lngs = calibracion.esquinas.map((e) => e[0]);
  const lats = calibracion.esquinas.map((e) => e[1]);
  const limites: [Coordenada, Coordenada] = [
    [Math.min(...lngs), Math.min(...lats)],
    [Math.max(...lngs), Math.max(...lats)],
  ];
  const [[oeste, sur], [este, norte]] = limites;

  const mapa = new maplibre.Map({
    container: contenedor,
    style: ESTILO_BASE,
    bounds: limites,
    maxBounds: [
      [oeste - MARGEN_VISTA, sur - MARGEN_VISTA],
      [este + MARGEN_VISTA, norte + MARGEN_VISTA],
    ],
    attributionControl: false, // la atribución se muestra como texto propio (ver ControlesMapa)
    dragRotate: false,
    pitchWithRotate: false,
    fadeDuration: SIN_MOVIMIENTO ? 0 : 300,
  });
  if (isDevMode()) {
    // Solo en desarrollo: permite inspeccionar el mapa desde la consola o con Playwright
    (window as unknown as { mapaRutas?: MapaLibre }).mapaRutas = mapa;
  }
  mapa.getCanvas().setAttribute('aria-label', etiquetaAccesible);
  mapa.touchZoomRotate.disableRotation();
  mapa.on('error', (evento) => console.error('Error del mapa', evento.error));

  await new Promise<void>((listo) => mapa.once('load', () => listo()));
  const capasBase = mapa.getStyle().layers.map((capa) => capa.id);
  mapa.setMinZoom(mapa.getZoom() - 0.5);
  await planoListo;
  agregarPlano(mapa, imagenPlano, calibracion.esquinas);

  return {
    mapa,
    capasBase,
    limites,
    crearMarcador: (elemento, opciones) => new maplibre.Marker({ ...opciones, element: elemento }),
  };
}

/**
 * El plano se dibuja como dos mitades triangulares para colocarlo igual que el prototipo
 * (MapLibre 5), con el que se calibraron las esquinas. Ver dominio/plano.ts.
 */
function agregarPlano(mapa: MapaLibre, imagen: HTMLImageElement, esquinas: Esquinas): void {
  const ancho = imagen.naturalWidth;
  const alto = imagen.naturalHeight;
  // Cada triángulo se extiende un poco más allá de la diagonal para que no quede una costura
  // (en la diagonal las dos mitades coinciden). La normal apunta hacia abajo a la derecha.
  const largo = Math.hypot(ancho, alto);
  const normal: [number, number] = [(alto / largo) * SOLAPE_PX, (ancho / largo) * SOLAPE_PX];

  for (const mitad of mitadesDelPlano(esquinas)) {
    const lienzo = document.createElement('canvas');
    lienzo.width = ancho;
    lienzo.height = alto;
    const ctx = lienzo.getContext('2d')!;
    const [a, b, c] = mitad.triangulo.map(([x, y]) => [x * ancho, y * alto]);
    const mas = ([x, y]: number[]) => [x + normal[0], y + normal[1]];
    const menos = ([x, y]: number[]) => [x - normal[0], y - normal[1]];
    // Contorno en orden: el triángulo más una franja angosta al otro lado de la diagonal
    const contorno =
      mitad.id === 'plano-superior'
        ? [a, b, mas(b), mas(c), c] // diagonal b → c, franja hacia abajo a la derecha
        : [menos(a), a, b, c, menos(c)]; // diagonal a → c, franja hacia arriba a la izquierda
    ctx.beginPath();
    for (const [x, y] of contorno) {
      ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.clip();
    ctx.drawImage(imagen, 0, 0);

    mapa.addSource(mitad.id, { type: 'canvas', canvas: lienzo, coordinates: mitad.esquinas, animate: false });
    mapa.addLayer({ id: mitad.id, type: 'raster', source: mitad.id, paint: { 'raster-fade-duration': 0 } });
  }
}
