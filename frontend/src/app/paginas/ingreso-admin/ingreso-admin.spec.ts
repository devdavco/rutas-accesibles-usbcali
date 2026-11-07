import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Sesion } from '../../servicios/sesion';
import { IngresoAdmin } from './ingreso-admin';

describe('IngresoAdmin', () => {
  let fixture: ComponentFixture<IngresoAdmin>;
  let elemento: HTMLElement;
  let backend: HttpTestingController;
  const URL = 'http://localhost:8080/api/admin/autenticacion/ingresar';

  const escribir = (id: string, valor: string) => {
    const campo = elemento.querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  };
  const enviar = async () => {
    elemento.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
  };
  const alerta = () => elemento.querySelector('[role=alert]')?.textContent ?? '';

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(IngresoAdmin);
    await fixture.whenStable();
    elemento = fixture.nativeElement;
  });

  it('los campos tienen etiquetas y autocompletado', () => {
    expect(elemento.querySelector('label[for=correo]')?.textContent?.trim()).toBe('Correo institucional');
    expect(elemento.querySelector('label[for=contrasena]')?.textContent?.trim()).toBe('Contraseña');
    expect(elemento.querySelector('#correo')?.getAttribute('autocomplete')).toBe('username');
    expect(elemento.querySelector('#contrasena')?.getAttribute('autocomplete')).toBe('current-password');
  });

  it('exige correo y contraseña antes de enviar', async () => {
    await enviar();
    expect(alerta()).toContain('Escriba el correo y la contraseña.');
    backend.expectNone(URL);
  });

  it('con credenciales correctas guarda la sesión y abre el editor', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    escribir('correo', 'admin@usbcali.edu.co');
    escribir('contrasena', 'clave');
    await enviar();

    const peticion = backend.expectOne(URL);
    expect(peticion.request.body).toEqual({ correo: 'admin@usbcali.edu.co', contrasena: 'clave' });
    peticion.flush({ token: 'abc', expiraEn: new Date(Date.now() + 7_200_000).toISOString() });

    expect(TestBed.inject(Sesion).token()).toBe('abc');
    expect(navegar).toHaveBeenCalledWith(['/admin/editor']);
  });

  it('muestra el mensaje del servidor si las credenciales son incorrectas', async () => {
    escribir('correo', 'admin@usbcali.edu.co');
    escribir('contrasena', 'mala');
    await enviar();
    backend.expectOne(URL).flush(
      { status: 401, detail: 'Correo o contraseña incorrectos.' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await fixture.whenStable();

    expect(alerta()).toContain('Correo o contraseña incorrectos.');
    expect(elemento.querySelector<HTMLInputElement>('#contrasena')!.value).toBe('');
  });

  it('muestra el aviso de bloqueo (429)', async () => {
    escribir('correo', 'admin@usbcali.edu.co');
    escribir('contrasena', 'mala');
    await enviar();
    backend.expectOne(URL).flush(
      { status: 429, detail: 'Demasiados intentos. Intente de nuevo en unos minutos.' },
      { status: 429, statusText: 'Too Many Requests' },
    );
    await fixture.whenStable();
    expect(alerta()).toContain('Demasiados intentos.');
  });
});
