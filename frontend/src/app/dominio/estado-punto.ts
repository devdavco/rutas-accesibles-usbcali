import { PasoDijkstra } from '../api/tipos';

/** Estado de un punto en un paso del algoritmo (colores del mapa y columna "Estado"). */
export type EstadoPunto = 'DEFINITIVO' | 'EN_REVISION' | 'CONOCIDO' | 'SIN_VISITAR';

export const ETIQUETA_ESTADO: Record<EstadoPunto, string> = {
  DEFINITIVO: 'Definitivo',
  EN_REVISION: 'En revisión',
  CONOCIDO: 'Costo conocido',
  SIN_VISITAR: 'Sin visitar',
};

/** Etiqueta corta para la tabla, como en el diseño. */
export const ETIQUETA_ESTADO_TABLA: Record<EstadoPunto, string> = {
  DEFINITIVO: 'Definitivo',
  EN_REVISION: 'Revisión',
  CONOCIDO: 'Conocido',
  SIN_VISITAR: 'Sin visitar',
};

/**
 * Deriva el estado de un punto a partir de un paso:
 * - el punto que se está procesando (SELECCION o REVISION) está "en revisión";
 * - los puntos definitivos, "definitivo";
 * - los que ya tienen costo, "costo conocido";
 * - el resto, "sin visitar".
 * En FIN y SIN_RUTA nadie está en revisión.
 */
export function estadoDePunto(paso: PasoDijkstra, puntoId: number): EstadoPunto {
  const procesando = paso.tipo === 'SELECCION' || paso.tipo === 'REVISION';
  if (procesando && paso.actualId === puntoId) {
    return 'EN_REVISION';
  }
  if (paso.definitivos.includes(puntoId)) {
    return 'DEFINITIVO';
  }
  if (paso.costos[puntoId] !== undefined) {
    return 'CONOCIDO';
  }
  return 'SIN_VISITAR';
}
