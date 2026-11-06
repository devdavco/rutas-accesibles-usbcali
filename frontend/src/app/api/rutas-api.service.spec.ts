import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { RutasApiService } from './rutas-api.service';
import { RespuestaRuta } from './tipos';

describe('RutasApiService', () => {
  let api: RutasApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(RutasApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide los puntos seleccionables al gateway', async () => {
    const respuesta = firstValueFrom(api.puntos());
    const peticion = http.expectOne('http://localhost:8080/api/rutas/puntos');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ id: 1, nombre: 'Entrada Principal', tipo: 'ENTRADA_CAMPUS', edificio: null }]);
    expect(await respuesta).toEqual([{ id: 1, nombre: 'Entrada Principal', tipo: 'ENTRADA_CAMPUS', edificio: null }]);
  });

  it('pide el grafo para el mapa', async () => {
    const respuesta = firstValueFrom(api.grafo());
    http.expectOne('http://localhost:8080/api/rutas/grafo').flush({ puntos: [], tramos: [] });
    expect(await respuesta).toEqual({ puntos: [], tramos: [] });
  });

  it('pide la ruta con origen, destino y pasos como parámetros', async () => {
    const respuesta = firstValueFrom(api.ruta(1, 2, true));
    const peticion = http.expectOne((r) => r.url === 'http://localhost:8080/api/rutas');
    expect(peticion.request.params.get('origen')).toBe('1');
    expect(peticion.request.params.get('destino')).toBe('2');
    expect(peticion.request.params.get('pasos')).toBe('true');

    // Respuesta real del backend (recortada): los campos ausentes son los que el backend omite
    const cuerpo: RespuestaRuta = {
      encontrada: true,
      mensaje: 'Ruta accesible encontrada: 417,5 m.',
      origen: { id: 1, nombre: 'Entrada Principal' },
      destino: { id: 2, nombre: 'Edificio El Cedro (Entrada Lateral)' },
      distanciaM: 417.45,
      costo: 417.45,
      puntos: [{ id: 1, nombre: 'Entrada Principal', tipo: 'ENTRADA_CAMPUS', lat: 3.34252136, lng: -76.54407333 }],
      segmentos: [{ condicion: 'PLANO', metros: 286.24, desdeId: 1, desde: 'Entrada Principal', hastaId: 78, hasta: 'Cruce 78' }],
      pasos: [
        {
          numero: 1, tipo: 'INICIO', definitivos: [], costos: { '1': 0 }, predecesores: {},
          mensaje: 'Inicio: todos los puntos tienen costo ∞ (desconocido), excepto el origen, Entrada Principal, que vale 0.',
        },
      ],
    };
    peticion.flush(cuerpo);
    const ruta = await respuesta;
    expect(ruta.distanciaM).toBe(417.45);
    expect(ruta.pasos?.[0].costos['1']).toBe(0);
    expect(ruta.pasos?.[0].costoAnterior).toBeUndefined();
  });

  it('sin pasos envía pasos=false', () => {
    api.ruta(28, 49, false).subscribe();
    const peticion = http.expectOne((r) => r.url === 'http://localhost:8080/api/rutas');
    expect(peticion.request.params.get('pasos')).toBe('false');
    peticion.flush({});
  });
});
