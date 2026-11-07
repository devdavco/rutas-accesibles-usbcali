import { metros, nombreVisible, numero } from './formato';

describe('formato', () => {
  it('usa coma decimal (es-CO)', () => {
    expect(numero(262.44)).toBe('262,4');
    expect(metros(417.45)).toBe('417,5 m');
  });

  it('nombreVisible da el mismo texto para el mapa y la hoja', () => {
    expect(nombreVisible('Edificio El Cedro (Entrada Lateral)')).toBe('El Cedro (entrada lateral)');
    expect(nombreVisible('Entrada de Estudiantes (Parqueadero)')).toBe('Entrada de Estudiantes (parqueadero)');
    expect(nombreVisible('Edificio Higuerones 1 (Entrada)')).toBe('Higuerones 1 (entrada)');
    expect(nombreVisible('Biblioteca')).toBe('Biblioteca');
    expect(nombreVisible('Entrada Principal')).toBe('Entrada Principal');
    expect(nombreVisible('Cruce 55')).toBe('Cruce 55');
  });
});
