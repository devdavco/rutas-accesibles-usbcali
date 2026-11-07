import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Sesion } from '../servicios/sesion';
import { interceptorAdmin } from './interceptor-admin';

describe('interceptorAdmin', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let sesion: Sesion;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([interceptorAdmin])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(Sesion);
    sesion.iniciar('abc.def.ghi', 'admin@usbcali.edu.co', new Date(Date.now() + 60_000).toISOString());
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('agrega el token a /api/admin/**', () => {
    http.get('http://localhost:8080/api/admin/puntos').subscribe();
    const peticion = backend.expectOne('http://localhost:8080/api/admin/puntos');
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    peticion.flush([]);
  });

  it('no agrega el token a las rutas públicas ni al ingreso', () => {
    http.get('http://localhost:8080/api/rutas/puntos').subscribe();
    http.post('http://localhost:8080/api/admin/autenticacion/ingresar', {}).subscribe();
    expect(backend.expectOne('http://localhost:8080/api/rutas/puntos').request.headers.has('Authorization')).toBe(false);
    const ingreso = backend.expectOne('http://localhost:8080/api/admin/autenticacion/ingresar');
    expect(ingreso.request.headers.has('Authorization')).toBe(false);
  });

  it('ante un 401 cierra la sesión y vuelve al ingreso avisando que expiró', () => {
    let fallo = false;
    http.get('http://localhost:8080/api/admin/tramos').subscribe({ error: () => (fallo = true) });
    backend.expectOne('http://localhost:8080/api/admin/tramos').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(fallo).toBe(true);
    expect(sesion.token()).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/admin/ingresar'], { queryParams: { motivo: 'expirada' } });
  });

  it('el 401 y el 429 del ingreso NO se tratan como sesión expirada (los muestra el formulario)', () => {
    const url = 'http://localhost:8080/api/admin/autenticacion/ingresar';
    const errores: number[] = [];
    http.post(url, {}).subscribe({ error: (e) => errores.push(e.status) });
    backend.expectOne(url).flush({ detail: 'Correo o contraseña incorrectos.' }, { status: 401, statusText: 'Unauthorized' });
    http.post(url, {}).subscribe({ error: (e) => errores.push(e.status) });
    backend.expectOne(url).flush({ detail: 'Demasiados intentos.' }, { status: 429, statusText: 'Too Many Requests' });

    expect(errores).toEqual([401, 429]);
    expect(sesion.token()).toBe('abc.def.ghi');
    expect(navegar).not.toHaveBeenCalled();
  });

  it('otros errores no cierran la sesión', () => {
    http.delete('http://localhost:8080/api/admin/puntos/1').subscribe({ error: () => undefined });
    backend.expectOne('http://localhost:8080/api/admin/puntos/1').flush({}, { status: 409, statusText: 'Conflict' });
    expect(sesion.token()).toBe('abc.def.ghi');
  });
});
