import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Sesion } from './sesion';

/** Solo deja entrar al editor con una sesión vigente; si no, lleva al ingreso. */
export const sesionActivaGuard: CanActivateFn = () => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  if (sesion.activa()) {
    return true;
  }
  // Había token pero venció: se avisa en el ingreso
  const vencida = sesion.token() !== null;
  sesion.cerrar();
  return router.createUrlTree(['/admin/ingresar'], vencida ? { queryParams: { motivo: 'expirada' } } : {});
};
