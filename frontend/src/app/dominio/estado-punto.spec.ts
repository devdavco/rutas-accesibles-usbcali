import { PasoDijkstra } from '../api/tipos';
import { estadoDePunto } from './estado-punto';

/** Paso 62 del diseño: se procesa el Cruce 55 y se revisa el Cruce 91. */
const paso: PasoDijkstra = {
  numero: 62,
  tipo: 'REVISION',
  actualId: 55,
  vecinoId: 91,
  costoNuevo: 335.2,
  actualizado: true,
  definitivos: [1, 11, 55, 74],
  costos: { '1': 0, '11': 317.1, '55': 323.4, '74': 323.0, '91': 335.2 },
  predecesores: { '11': 13, '55': 11, '74': 11, '91': 55 },
  mensaje: 'Desde Cruce 55 se revisa Cruce 91…',
};

describe('estadoDePunto', () => {
  it('el punto que se está procesando está en revisión aunque ya sea definitivo', () => {
    expect(estadoDePunto(paso, 55)).toBe('EN_REVISION');
  });

  it('los puntos definitivos', () => {
    expect(estadoDePunto(paso, 11)).toBe('DEFINITIVO');
    expect(estadoDePunto(paso, 1)).toBe('DEFINITIVO');
  });

  it('los puntos con costo conocido que aún no son definitivos', () => {
    expect(estadoDePunto(paso, 91)).toBe('CONOCIDO');
  });

  it('los puntos sin costo están sin visitar', () => {
    expect(estadoDePunto(paso, 2)).toBe('SIN_VISITAR');
  });

  it('en el paso FIN nadie queda en revisión: el destino es definitivo', () => {
    const fin: PasoDijkstra = { ...paso, tipo: 'FIN', actualId: 2, vecinoId: undefined, definitivos: [...paso.definitivos, 2], costos: { ...paso.costos, '2': 417.45 } };
    expect(estadoDePunto(fin, 2)).toBe('DEFINITIVO');
  });

  it('en el paso INICIO solo el origen tiene costo', () => {
    const inicio: PasoDijkstra = { numero: 1, tipo: 'INICIO', definitivos: [], costos: { '1': 0 }, predecesores: {}, mensaje: '' };
    expect(estadoDePunto(inicio, 1)).toBe('CONOCIDO');
    expect(estadoDePunto(inicio, 2)).toBe('SIN_VISITAR');
  });
});
