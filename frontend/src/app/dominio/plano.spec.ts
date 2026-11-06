import { Esquinas, mitadesDelPlano } from './plano';

/** Calibración real del prototipo (js/datos.js). */
const PROTOTIPO: Esquinas = [
  [-76.54691322, 3.34745654],
  [-76.53971562, 3.34760492],
  [-76.5398272, 3.34231359],
  [-76.54697496, 3.34232267],
];

/** Iguales hasta 8 decimales de grado (medio milímetro). */
const cerca = (a: [number, number], b: [number, number]) => {
  expect(a[0]).toBeCloseTo(b[0], 8);
  expect(a[1]).toBeCloseTo(b[1], 8);
};

describe('mitadesDelPlano', () => {
  it('cada mitad conserva las tres esquinas reales de su triángulo', () => {
    const [superior, inferior] = mitadesDelPlano(PROTOTIPO);
    const [si, sd, id, ii] = PROTOTIPO;
    expect(superior.esquinas[0]).toEqual(si);
    expect(superior.esquinas[1]).toEqual(sd);
    expect(superior.esquinas[3]).toEqual(ii);
    expect(inferior.esquinas[1]).toEqual(sd);
    expect(inferior.esquinas[2]).toEqual(id);
    expect(inferior.esquinas[3]).toEqual(ii);
  });

  it('el cuarto vértice completa un paralelogramo (diagonales con el mismo punto medio)', () => {
    for (const mitad of mitadesDelPlano(PROTOTIPO)) {
      const [a, b, c, d] = mitad.esquinas;
      // En un paralelogramo a + c = b + d; en grados queda una diferencia de milímetros por la
      // curvatura de Mercator, donde se construye el paralelogramo
      cerca([a[0] + c[0], a[1] + c[1]], [b[0] + d[0], b[1] + d[1]]);
    }
  });

  it('si las esquinas ya forman un paralelogramo, las dos mitades usan las esquinas originales', () => {
    const rectangulo: Esquinas = [[-76.546, 3.347], [-76.54, 3.347], [-76.54, 3.342], [-76.546, 3.342]];
    for (const mitad of mitadesDelPlano(rectangulo)) {
      mitad.esquinas.forEach((esquina, i) => cerca(esquina, rectangulo[i]));
    }
  });

  it('con la calibración del prototipo el cuadrilátero no es un paralelogramo', () => {
    // Por eso MapLibre 6 lo deformaba en perspectiva y el plano se corría
    const [superior] = mitadesDelPlano(PROTOTIPO);
    const infDerReal = PROTOTIPO[2];
    const infDerParalelogramo = superior.esquinas[2];
    const metrosPorGrado = 111_320;
    const separacion = Math.hypot(infDerReal[0] - infDerParalelogramo[0], infDerReal[1] - infDerParalelogramo[1]) * metrosPorGrado;
    expect(separacion).toBeGreaterThan(5);
  });
});
