/**
 * Dónde poner una etiqueta junto a un punto del mapa para que estorbe lo menos posible.
 * Todo en píxeles de pantalla. Se usa en móvil, donde el espacio es escaso.
 */

export interface Punto2D {
  x: number;
  y: number;
}

export interface Rect {
  x: number;
  y: number;
  ancho: number;
  alto: number;
}

/** Anclas de MapLibre: el punto de la etiqueta que queda sobre el punto del mapa. */
export type Ancla = 'top' | 'bottom' | 'left' | 'right' | 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';

/** En caso de empate gana la primera: arriba o abajo del punto, centrada, se lee mejor. */
export const ANCLAS: Ancla[] = ['bottom', 'top', 'bottom-left', 'bottom-right', 'top-left', 'top-right', 'left', 'right'];

/** Desplazamiento [x, y] de la etiqueta respecto al punto para cada ancla. */
export function desplazamiento(ancla: Ancla, separacion: number): [number, number] {
  const x = ancla.includes('left') ? separacion : ancla.includes('right') ? -separacion : 0;
  const y = ancla.startsWith('top') ? separacion : ancla.startsWith('bottom') ? -separacion : 0;
  return [x, y];
}

/** Rectángulo que ocupa una etiqueta de ancho × alto puesta con esa ancla. */
export function rectEtiqueta(p: Punto2D, ancho: number, alto: number, ancla: Ancla, separacion: number): Rect {
  const [dx, dy] = desplazamiento(ancla, separacion);
  const x = ancla.includes('left') ? p.x + dx : ancla.includes('right') ? p.x + dx - ancho : p.x - ancho / 2;
  const y = ancla.startsWith('top') ? p.y + dy : ancla.startsWith('bottom') ? p.y + dy - alto : p.y - alto / 2;
  return { x, y, ancho, alto };
}

function areaComun(a: Rect, b: Rect): number {
  const ancho = Math.min(a.x + a.ancho, b.x + b.ancho) - Math.max(a.x, b.x);
  const alto = Math.min(a.y + a.alto, b.y + b.alto) - Math.max(a.y, b.y);
  return ancho > 0 && alto > 0 ? ancho * alto : 0;
}

/** Cuántos pasos de 3 px de la línea caen dentro del rectángulo (con 3 px de holgura). */
function largoDentro(linea: Punto2D[], r: Rect): number {
  const PASO = 3;
  let dentro = 0;
  for (let i = 1; i < linea.length; i++) {
    const a = linea[i - 1];
    const b = linea[i];
    const n = Math.max(1, Math.ceil(Math.hypot(b.x - a.x, b.y - a.y) / PASO));
    for (let k = 0; k <= n; k++) {
      const x = a.x + ((b.x - a.x) * k) / n;
      const y = a.y + ((b.y - a.y) * k) / n;
      if (x >= r.x - PASO && x <= r.x + r.ancho + PASO && y >= r.y - PASO && y <= r.y + r.alto + PASO) dentro++;
    }
  }
  return dentro;
}

/**
 * Elige el ancla que menos estorba. Penaliza, de más a menos grave: salirse del área visible,
 * tapar la línea de la ruta y montarse sobre obstáculos (controles, atribución, otras etiquetas).
 */
export function elegirAncla(
  p: Punto2D,
  ancho: number,
  alto: number,
  separacion: number,
  area: Rect,
  linea: Punto2D[],
  obstaculos: Rect[],
): Ancla {
  let mejor: { ancla: Ancla; costo: number } | null = null;
  for (const ancla of ANCLAS) {
    const r = rectEtiqueta(p, ancho, alto, ancla, separacion);
    const fuera = ancho * alto - areaComun(r, area);
    const costo =
      fuera * 100 + largoDentro(linea, r) * 200 + obstaculos.reduce((suma, o) => suma + areaComun(r, o), 0) * 10;
    if (!mejor || costo < mejor.costo) mejor = { ancla, costo };
  }
  return mejor!.ancla;
}
