/*
 * Tipos de la API de rutas. Reflejan exactamente el JSON que entrega servicio-grafos
 * (a través del gateway). Los campos opcionales son los que el backend omite cuando valen null
 * (@JsonInclude(NON_NULL)). Las claves de los mapas llegan como texto en JSON.
 */

export type TipoPunto = 'ENTRADA_CAMPUS' | 'ENTRADA_EDIFICIO' | 'CRUCE';

export type Condicion = 'PLANO' | 'RAMPA' | 'PENDIENTE_FUERTE' | 'ESCALERAS';

/** GET /api/rutas/puntos */
export interface PuntoSeleccionable {
  id: number;
  nombre: string;
  tipo: TipoPunto;
  edificio: string | null;
}

/** GET /api/rutas/grafo (solo para dibujar) */
export interface GrafoVisual {
  puntos: PuntoVisual[];
  tramos: TramoVisual[];
}

export interface PuntoVisual {
  id: number;
  nombre: string;
  tipo: TipoPunto;
  edificio: string | null;
  seleccionable: boolean;
  lat: number;
  lng: number;
}

export interface TramoVisual {
  id: number;
  puntoInicialId: number;
  puntoFinalId: number;
  condicion: Condicion;
  /** Metros calculados por PostGIS */
  longitudM: number;
  /** Metros × factor de la condición; null para escaleras (no entra al grafo) */
  pesoCalculado: number | null;
}

/** GET /api/rutas?origen=&destino=&pasos= */
export interface RespuestaRuta {
  encontrada: boolean;
  mensaje: string;
  origen: ReferenciaPunto;
  destino: ReferenciaPunto;
  /** Ausente si no hay ruta */
  distanciaM?: number;
  /** Ausente si no hay ruta */
  costo?: number;
  puntos: PuntoRuta[];
  segmentos: Segmento[];
  /** Ausente si no hay ruta */
  geojson?: ColeccionGeoJson;
  /** Solo si se pidió pasos=true */
  pasos?: PasoDijkstra[];
}

export interface ReferenciaPunto {
  id: number;
  nombre: string;
}

export interface PuntoRuta {
  id: number;
  nombre: string;
  tipo: TipoPunto;
  lat: number;
  lng: number;
}

export interface Segmento {
  condicion: Condicion;
  metros: number;
  desdeId: number;
  desde: string;
  hastaId: number;
  hasta: string;
}

export type TipoPaso = 'INICIO' | 'SELECCION' | 'REVISION' | 'FIN' | 'SIN_RUTA';

export interface PasoDijkstra {
  numero: number;
  tipo: TipoPaso;
  actualId?: number;
  vecinoId?: number;
  /** Ausente significa infinito (costo aún desconocido) */
  costoAnterior?: number;
  costoNuevo?: number;
  actualizado?: boolean;
  definitivos: number[];
  /** id → costo acumulado; los puntos ausentes valen infinito */
  costos: Record<string, number>;
  /** id → id del predecesor */
  predecesores: Record<string, number>;
  mensaje: string;
}

/** GeoJSON (RFC 7946): coordenadas en orden [longitud, latitud] */
export interface ColeccionGeoJson {
  type: 'FeatureCollection';
  features: ElementoGeoJson[];
}

export interface ElementoGeoJson {
  type: 'Feature';
  geometry:
    | { type: 'LineString'; coordinates: [number, number][] }
    | { type: 'Point'; coordinates: [number, number] };
  properties: {
    tipo: 'ruta' | 'origen' | 'destino';
    id?: number;
    nombre?: string;
    distanciaM?: number;
  };
}

/** Error del API (RFC 9457) */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}
