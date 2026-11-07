import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Sesion } from '../servicios/sesion';

const RUTA_ADMIN = '/api/admin/';
const RUTA_INGRESO = '/api/admin/autenticacion/ingresar';

/**
 * Agrega el token del administrador a /api/admin/** y, si el servidor responde 401, cierra la
 * sesión y vuelve al ingreso con el aviso "La sesión expiró".
 * El ingreso no lleva token, y su 401 o 429 los muestra el propio formulario.
 */
export const interceptorAdmin: HttpInterceptorFn = (peticion, siguiente) => {
  const ruta = new URL(peticion.url, location.origin).pathname;
  if (!ruta.startsWith(RUTA_ADMIN) || ruta === RUTA_INGRESO) {
    return siguiente(peticion);
  }
  const sesion = inject(Sesion);
  const router = inject(Router);
  const token = sesion.token();
  const conToken = token ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : peticion;
  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        sesion.cerrar();
        void router.navigate(['/admin/ingresar'], { queryParams: { motivo: 'expirada' } });
      }
      return throwError(() => error);
    }),
  );
};
