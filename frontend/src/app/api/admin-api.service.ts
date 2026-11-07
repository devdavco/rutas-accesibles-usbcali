import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Condicion } from './tipos';
import { DatosPunto, DatosTramo, PuntoAdmin, RespuestaIngreso, TramoAdmin } from './tipos-admin';

/** API de administración. El token lo agrega interceptorAdmin. */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/admin`;

  ingresar(correo: string, contrasena: string): Observable<RespuestaIngreso> {
    return this.http.post<RespuestaIngreso>(`${this.base}/autenticacion/ingresar`, { correo, contrasena });
  }

  puntos(): Observable<PuntoAdmin[]> {
    return this.http.get<PuntoAdmin[]>(`${this.base}/puntos`);
  }

  crearPunto(datos: DatosPunto): Observable<PuntoAdmin> {
    return this.http.post<PuntoAdmin>(`${this.base}/puntos`, datos);
  }

  editarPunto(id: number, datos: DatosPunto): Observable<PuntoAdmin> {
    return this.http.put<PuntoAdmin>(`${this.base}/puntos/${id}`, datos);
  }

  eliminarPunto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/puntos/${id}`);
  }

  tramos(): Observable<TramoAdmin[]> {
    return this.http.get<TramoAdmin[]>(`${this.base}/tramos`);
  }

  crearTramo(datos: DatosTramo): Observable<TramoAdmin> {
    return this.http.post<TramoAdmin>(`${this.base}/tramos`, datos);
  }

  cambiarCondicion(id: number, condicion: Condicion): Observable<TramoAdmin> {
    return this.http.put<TramoAdmin>(`${this.base}/tramos/${id}`, { condicion });
  }

  eliminarTramo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/tramos/${id}`);
  }
}
