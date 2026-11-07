/** Formato de números y nombres para mostrar (es-CO: coma decimal). */

const UN_DECIMAL = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const ENTERO = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });

/** 417.45 → "417,5" */
export function numero(valor: number): string {
  return UN_DECIMAL.format(valor);
}

/** 417.45 → "417,5 m" */
export function metros(valor: number): string {
  return `${numero(valor)} m`;
}

/** 0.4512 → "45 %" */
export function porcentaje(fraccion: number): string {
  return `${ENTERO.format(fraccion * 100)} %`;
}

/** Costo de un paso; ausente significa infinito. */
export function costo(valor: number | undefined): string {
  return valor === undefined ? '∞' : numero(valor);
}

export interface NombreSeparado {
  principal: string;
  detalle: string | null;
}

/** "Edificio El Cedro (Entrada Lateral)" → { principal: "Edificio El Cedro", detalle: "Entrada Lateral" } */
export function separarNombre(nombre: string): NombreSeparado {
  const partes = /^(.*?)\s*\((.+)\)$/.exec(nombre);
  return partes ? { principal: partes[1], detalle: partes[2] } : { principal: nombre, detalle: null };
}

/**
 * Nombre de un punto tal como se muestra en toda la interfaz (etiquetas del mapa, hoja,
 * recorrido y paso a paso), para que el mismo punto se lea siempre igual:
 * "Edificio El Cedro (Entrada Lateral)" → "El Cedro (entrada lateral)";
 * "Entrada de Estudiantes (Parqueadero)" → "Entrada de Estudiantes (parqueadero)";
 * "Biblioteca" → "Biblioteca".
 */
export function nombreVisible(nombre: string): string {
  const { principal, detalle } = separarNombre(nombre);
  const base = principal.replace(/^Edificio\s+/, '');
  return detalle ? `${base} (${detalle.toLocaleLowerCase('es-CO')})` : base;
}
