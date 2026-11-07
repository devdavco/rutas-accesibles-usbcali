import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Sesion } from './sesion';
import { sesionActivaGuard } from './sesion-activa.guard';

describe('sesionActivaGuard', () => {
  let sesion: Sesion;
  const ejecutar = () =>
    TestBed.runInInjectionContext(() =>
      sesionActivaGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  const dentroDe = (ms: number) => new Date(Date.now() + ms).toISOString();

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    sesion = TestBed.inject(Sesion);
  });

  it('sin sesión redirige al ingreso', () => {
    const resultado = ejecutar() as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/admin/ingresar');
  });

  it('con una sesión vigente deja pasar', () => {
    sesion.iniciar('token', 'admin@usbcali.edu.co', dentroDe(60_000));
    expect(ejecutar()).toBe(true);
  });

  it('con el token vencido cierra la sesión y avisa que expiró', () => {
    sesion.iniciar('token', 'admin@usbcali.edu.co', dentroDe(-1_000));
    const resultado = ejecutar() as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/admin/ingresar?motivo=expirada');
    expect(sesion.token()).toBeNull();
  });
});
