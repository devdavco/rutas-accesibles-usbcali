import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { GrafoVisual, PuntoSeleccionable, RespuestaRuta } from './tipos';

/** Acceso al API de rutas a través del gateway. */
@Injectable({ providedIn: 'root' })
export class RutasApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/rutas`;

  /** Puntos que se pueden elegir como origen o destino. */
  puntos(): Observable<PuntoSeleccionable[]> {
    return this.http.get<PuntoSeleccionable[]>(`${this.base}/puntos`);
  }

  /** Grafo completo, solo para dibujarlo en el mapa. */
  grafo(): Observable<GrafoVisual> {
    return this.http.get<GrafoVisual>(`${this.base}/grafo`);
  }

  /** Ruta accesible de menor costo; con pasos=true incluye los pasos de Dijkstra. */
  ruta(origen: number, destino: number, pasos: boolean): Observable<RespuestaRuta> {
    const params = new HttpParams()
      .set('origen', origen)
      .set('destino', destino)
      .set('pasos', pasos);
    return this.http.get<RespuestaRuta>(this.base, { params });
  }
}
