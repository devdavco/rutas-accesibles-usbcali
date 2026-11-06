import { PasoDijkstra } from '../api/tipos';
import { EstadoPunto, estadoDePunto } from './estado-punto';

export interface FilaPaso {
  id: number;
  nombre: string;
  costo: number;
  vieneDe: string | null;
  estado: EstadoPunto;
  /** Fila del punto que se está procesando (resaltada) */
  actual: boolean;
}

/**
 * Filas de la tabla del paso a paso: primero el punto actual y el vecino revisado, luego el
 * resto de puntos con costo conocido, de menor a mayor costo. Los puntos sin visitar no se
 * listan (su costo es infinito); el mapa sí los muestra.
 */
export function filasDelPaso(paso: PasoDijkstra, nombres: ReadonlyMap<number, string>): FilaPaso[] {
  const nombre = (id: number) => nombres.get(id) ?? `Punto ${id}`;
  const filas = Object.entries(paso.costos).map(([clave, costo]): FilaPaso => {
    const id = Number(clave);
    const predecesor = paso.predecesores[clave];
    return {
      id,
      nombre: nombre(id),
      costo,
      vieneDe: predecesor === undefined ? null : nombre(predecesor),
      estado: estadoDePunto(paso, id),
      actual: id === paso.actualId && (paso.tipo === 'SELECCION' || paso.tipo === 'REVISION'),
    };
  });
  const prioridad = (fila: FilaPaso) => (fila.id === paso.actualId ? 0 : fila.id === paso.vecinoId ? 1 : 2);
  return filas.sort((a, b) => prioridad(a) - prioridad(b) || a.costo - b.costo || a.id - b.id);
}
