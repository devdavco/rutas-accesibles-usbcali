import { Segmento } from '../api/tipos';
import { frase, recorridoTexto } from './recorrido-texto';

const segmento = (condicion: Segmento['condicion'], metros: number, desde = 'A', hasta = 'B'): Segmento => ({
  condicion, metros, desdeId: 1, desde, hastaId: 2, hasta,
});

describe('recorridoTexto', () => {
  it('describe una ruta de varios segmentos como en el diseño, con textos neutros para la rampa', () => {
    const indicaciones = recorridoTexto([
      segmento('PLANO', 286.24, 'Entrada Principal', 'Cruce 78'),
      segmento('RAMPA', 11.36, 'Cruce 78', 'Giro Biblioteca–El Lago'),
      segmento('PLANO', 70.8, 'Giro Biblioteca–El Lago', 'Cruce 57'),
      segmento('RAMPA', 10.24, 'Cruce 57', 'Cruce 58'),
      segmento('PLANO', 38.81, 'Cruce 58', 'Edificio El Cedro (Entrada Lateral)'),
    ]);

    expect(indicaciones.map(frase)).toEqual([
      'Desde Entrada Principal, avance 286,2 m.',
      'Tome la rampa 11,4 m.',
      'Continúe por el andén 70,8 m.',
      'Tome la rampa 10,2 m.',
      'Avance hasta El Cedro (entrada lateral) 38,8 m.',
    ]);
    expect(indicaciones.map((i) => i.etiqueta)).toEqual(['Plano', 'Rampa', 'Plano', 'Rampa', 'Plano']);
  });

  it('nunca dice si la rampa sube o baja (el grafo no tiene alturas)', () => {
    const textos = recorridoTexto([segmento('PLANO', 5), segmento('RAMPA', 5), segmento('PLANO', 5)]).map(frase);
    for (const texto of textos) {
      expect(texto).not.toMatch(/suba|baje/i);
    }
  });

  it('usa "Recorra la pendiente fuerte" para la pendiente', () => {
    const [, medio] = recorridoTexto([segmento('PLANO', 1), segmento('PENDIENTE_FUERTE', 12.5), segmento('PLANO', 1)]);
    expect(frase(medio)).toBe('Recorra la pendiente fuerte 12,5 m.');
    expect(medio.etiqueta).toBe('Pendiente fuerte');
  });

  it('con un solo segmento indica desde dónde y hasta dónde', () => {
    const [unico] = recorridoTexto([
      segmento('PLANO', 45.04, 'Entrada de Estudiantes (Parqueadero)', 'Edificio El Lago (Entrada de Estudiantes)'),
    ]);
    // Los nombres usan el mismo formato que las etiquetas del mapa
    expect(frase(unico)).toBe('Desde Entrada de Estudiantes (parqueadero), avance hasta El Lago (entrada de estudiantes) 45,0 m.');
  });

  it('una rampa al principio o al final también lleva el punto de partida o de llegada', () => {
    const textos = recorridoTexto([segmento('RAMPA', 3, 'X', 'Y'), segmento('PLANO', 4), segmento('RAMPA', 2.05, 'M', 'Z')]).map(frase);
    expect(textos[0]).toBe('Desde X, tome la rampa 3,0 m.');
    expect(textos[2]).toBe('Tome la rampa hasta Z 2,1 m.');
  });

  it('sin segmentos (origen igual a destino) no hay indicaciones', () => {
    expect(recorridoTexto([])).toEqual([]);
  });
});
