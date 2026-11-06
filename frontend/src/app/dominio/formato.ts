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
 * Texto corto para las etiquetas del mapa:
 * ("Edificio El Cedro (Entrada Lateral)", "El Cedro") → "El Cedro, entrada lateral".
 */
export function nombreCorto(nombre: string, edificio: string | null): string {
  const { principal, detalle } = separarNombre(nombre);
  const base = edificio ?? principal;
  return detalle ? `${base}, ${detalle.toLocaleLowerCase('es-CO')}` : base;
}
