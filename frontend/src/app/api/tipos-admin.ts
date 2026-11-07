/* Tipos del API de administración (servicio-negocio, /api/admin/**). */
import { Condicion, TipoPunto } from './tipos';

export interface RespuestaIngreso {
  token: string;
  /** Instante ISO 8601 */
  expiraEn: string;
}

export interface PuntoAdmin {
  id: number;
  nombre: string;
  tipo: TipoPunto;
  edificio: string | null;
  seleccionable: boolean;
  lat: number;
  lng: number;
}

export type DatosPunto = Omit<PuntoAdmin, 'id'>;

export interface TramoAdmin {
  id: number;
  puntoInicialId: number;
  puntoFinalId: number;
  condicion: Condicion;
  /** Metros calculados por PostGIS */
  longitudM: number;
}

export interface DatosTramo {
  puntoInicialId: number;
  puntoFinalId: number;
  condicion: Condicion;
}
