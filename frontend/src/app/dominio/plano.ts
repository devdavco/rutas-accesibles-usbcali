/**
 * Colocación del plano del campus igual que en el prototipo de validación.
 *
 * Las esquinas de calibración se fijaron a ojo en el prototipo, que usa MapLibre 5. MapLibre 5
 * dibuja una imagen con cuatro esquinas como DOS TRIÁNGULOS AFINES separados por la diagonal
 * superior derecha → inferior izquierda. MapLibre 6 (el del frontend) deforma en perspectiva un
 * cuadrilátero que no es paralelogramo, y como estas esquinas no lo son, el plano se corría
 * varios metros respecto a los puntos.
 *
 * Para reproducir MapLibre 5, la imagen se parte en sus dos triángulos y cada mitad se coloca en
 * el paralelogramo que completa su triángulo: sobre un paralelogramo, MapLibre 6 aplica
 * exactamente la transformación afín. Los cálculos se hacen en coordenadas Mercator (como
 * MapLibre), no en grados.
 */

export type Esquinas = [[number, number], [number, number], [number, number], [number, number]];

/** Una mitad del plano: el triángulo de la imagen que se dibuja y dónde va su paralelogramo. */
export interface MitadPlano {
  id: 'plano-superior' | 'plano-inferior';
  /** Vértices del triángulo en fracciones del ancho y alto de la imagen (0 a 1). */
  triangulo: [[number, number], [number, number], [number, number]];
  /** Esquinas [lng, lat] (sup. izq., sup. der., inf. der., inf. izq.) del paralelogramo. */
  esquinas: Esquinas;
}

type Mercator = [number, number];

function aMercator([lng, lat]: [number, number]): Mercator {
  const y = 180 - (180 / Math.PI) * Math.log(Math.tan(Math.PI / 4 + (lat * Math.PI) / 360));
  return [(180 + lng) / 360, y / 360];
}

function aLngLat([x, y]: Mercator): [number, number] {
  const y2 = 180 - y * 360;
  return [x * 360 - 180, (360 / Math.PI) * Math.atan(Math.exp((y2 * Math.PI) / 180)) - 90];
}

/** Cuarto vértice del paralelogramo con vértices a, b y el opuesto a c: a + b − c. */
function completar(a: Mercator, b: Mercator, c: Mercator): [number, number] {
  return aLngLat([a[0] + b[0] - c[0], a[1] + b[1] - c[1]]);
}

/**
 * Las dos mitades del plano. La superior es el triángulo sup. izq. – sup. der. – inf. izq. y la
 * inferior, sup. der. – inf. der. – inf. izq.; las dos comparten la diagonal, donde coinciden.
 */
export function mitadesDelPlano(esquinas: Esquinas): MitadPlano[] {
  const [si, sd, id, ii] = esquinas;
  const [msi, msd, mid, mii] = esquinas.map(aMercator);
  return [
    {
      id: 'plano-superior',
      triangulo: [[0, 0], [1, 0], [0, 1]],
      esquinas: [si, sd, completar(msd, mii, msi), ii],
    },
    {
      id: 'plano-inferior',
      triangulo: [[1, 0], [1, 1], [0, 1]],
      esquinas: [completar(msd, mii, mid), sd, id, ii],
    },
  ];
}
