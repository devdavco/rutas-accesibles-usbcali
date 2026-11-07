import { Rect, elegirAncla, rectEtiqueta } from './etiquetas';

const AREA: Rect = { x: 0, y: 0, ancho: 390, alto: 300 };

describe('elegirAncla', () => {
  it('sin nada que estorbe deja la etiqueta encima del punto, centrada', () => {
    expect(elegirAncla({ x: 200, y: 150 }, 100, 24, 8, AREA, [], [])).toBe('bottom');
  });

  it('no tapa la ruta: si la ruta sube desde el punto, la etiqueta va debajo', () => {
    const ruta = [{ x: 200, y: 150 }, { x: 200, y: 20 }];
    const ancla = elegirAncla({ x: 200, y: 150 }, 100, 24, 8, AREA, ruta, []);
    expect(ancla.startsWith('top')).toBe(true);
  });

  it('no se sale del área visible: junto al borde inferior la etiqueta sube', () => {
    expect(elegirAncla({ x: 200, y: 290 }, 100, 24, 8, AREA, [{ x: 200, y: 290 }, { x: 200, y: 100 }], [])).not.toMatch(/^top/);
  });

  it('esquiva los obstáculos (por ejemplo, los controles del mapa)', () => {
    const controles: Rect = { x: 150, y: 100, ancho: 240, alto: 42 };
    const ancla = elegirAncla({ x: 200, y: 150 }, 100, 24, 8, AREA, [], [controles]);
    const r = rectEtiqueta({ x: 200, y: 150 }, 100, 24, ancla, 8);
    expect(r.y).toBeGreaterThanOrEqual(142);
  });
});
