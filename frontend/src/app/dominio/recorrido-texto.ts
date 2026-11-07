import { Condicion, Segmento } from '../api/tipos';
import { metros, nombreVisible } from './formato';

/** Una indicación del recorrido: texto, cantidad (en negrita) y condición del tramo. */
export interface Indicacion {
  texto: string;
  metros: string;
  condicion: Condicion;
  etiqueta: string;
}

export const ETIQUETA_CONDICION: Record<Condicion, string> = {
  PLANO: 'Plano',
  RAMPA: 'Rampa',
  PENDIENTE_FUERTE: 'Pendiente fuerte',
  ESCALERAS: 'Escaleras',
};

/**
 * Convierte los segmentos de la ruta en indicaciones para leer (RF05).
 * El grafo no tiene alturas, así que no se sabe si una rampa sube o baja:
 * los textos son neutros ("Tome la rampa", "Recorra la pendiente fuerte").
 */
export function recorridoTexto(segmentos: Segmento[]): Indicacion[] {
  return segmentos.map((segmento, i) => {
    const primero = i === 0;
    const ultimo = i === segmentos.length - 1;
    let texto = verbo(segmento.condicion, primero, ultimo);
    if (primero) {
      texto = `Desde ${nombreVisible(segmento.desde)}, ${minusculaInicial(texto)}`;
    }
    if (ultimo) {
      texto = `${texto} hasta ${nombreVisible(segmento.hasta)}`;
    }
    return {
      texto,
      metros: metros(segmento.metros),
      condicion: segmento.condicion,
      etiqueta: ETIQUETA_CONDICION[segmento.condicion],
    };
  });
}

/** La indicación completa como una sola frase (para pruebas y lectores de pantalla). */
export function frase(indicacion: Indicacion): string {
  return `${indicacion.texto} ${indicacion.metros}.`;
}

function verbo(condicion: Condicion, primero: boolean, ultimo: boolean): string {
  switch (condicion) {
    case 'RAMPA':
      return 'Tome la rampa';
    case 'PENDIENTE_FUERTE':
      return 'Recorra la pendiente fuerte';
    case 'ESCALERAS':
      // No debería ocurrir: los tramos con escaleras no entran al grafo
      return 'Tramo con escaleras';
    case 'PLANO':
      if (primero || ultimo) {
        return 'Avance';
      }
      return 'Continúe por el andén';
  }
}

function minusculaInicial(texto: string): string {
  return texto.charAt(0).toLocaleLowerCase('es-CO') + texto.slice(1);
}
