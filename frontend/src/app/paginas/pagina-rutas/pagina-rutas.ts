import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  Injector,
  afterNextRender,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';
import { RutasApiService } from '../../api/rutas-api.service';
import { ProblemDetail } from '../../api/tipos';
import { Consulta, Modo, SolicitudRuta } from '../../componentes/consulta/consulta';
import { Mapa, VistaMapa } from '../../componentes/mapa/mapa';
import { PasoAPaso } from '../../componentes/paso-a-paso/paso-a-paso';
import { Resultado } from '../../componentes/resultado/resultado';
import { SinRuta } from '../../componentes/sin-ruta/sin-ruta';
import { metros } from '../../dominio/formato';

type Vista = 'consulta' | 'cargando' | 'error' | 'resultado' | 'pasos' | 'sin-ruta';

const ERROR_CONEXION = 'No se pudo conectar con el servidor. Intente de nuevo.';

/**
 * Página principal: panel (o hoja inferior en móvil) más mapa.
 * El estado vive en la URL (/ruta?origen=1&destino=2&modo=pasos) para poder compartir o recargar.
 */
@Component({
  selector: 'app-pagina-rutas',
  imports: [Consulta, Resultado, PasoAPaso, SinRuta, Mapa],
  templateUrl: './pagina-rutas.html',
  styleUrl: './pagina-rutas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaRutas {
  private readonly api = inject(RutasApiService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);

  /** URL actual, actualizada en cada navegación. */
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.parseUrl(this.router.url)),
    ),
    { initialValue: this.router.parseUrl(this.router.url) },
  );

  protected readonly origen = computed(() => this.entero(this.url().queryParams['origen']));
  protected readonly destino = computed(() => this.entero(this.url().queryParams['destino']));
  protected readonly modo = computed<Modo>(() => (this.url().queryParams['modo'] === 'pasos' ? 'pasos' : 'directa'));
  private readonly enRuta = computed(() => this.url().root.children['primary']?.segments[0]?.path === 'ruta');

  // Datos del API
  protected readonly puntos = rxResource({ stream: () => this.api.puntos() });
  protected readonly grafo = rxResource({ stream: () => this.api.grafo() });
  private readonly solicitud = computed(() => {
    const origen = this.origen();
    const destino = this.destino();
    return this.enRuta() && origen !== null && destino !== null
      ? { origen, destino, pasos: this.modo() === 'pasos' }
      : undefined;
  });
  protected readonly ruta = rxResource({
    params: () => this.solicitud(),
    stream: ({ params }) => this.api.ruta(params.origen, params.destino, params.pasos),
  });

  protected readonly nombres = computed(
    () => new Map((this.grafo.value()?.puntos ?? []).map((p) => [p.id, p.nombre] as const)),
  );

  /** Índice del paso mostrado (lo comparten el panel y el mapa). */
  protected readonly indicePaso = signal(0);

  protected readonly vista = computed<Vista>(() => {
    if (!this.solicitud()) return 'consulta';
    if (this.ruta.isLoading()) return 'cargando';
    if (this.ruta.error()) return 'error';
    const respuesta = this.ruta.value();
    if (!respuesta) return 'cargando';
    if (!respuesta.encontrada) return 'sin-ruta';
    if (this.modo() === 'pasos') return this.grafo.value() ? 'pasos' : 'cargando';
    return 'resultado';
  });

  protected readonly vistaMapa = computed<VistaMapa>(() => {
    const vista = this.vista();
    return vista === 'pasos' ? 'pasos' : vista === 'resultado' ? 'resultado' : 'consulta';
  });

  protected readonly pasoActual = computed(() => this.ruta.value()?.pasos?.[this.indicePaso()]);

  /** Mensaje del error de la ruta o de la lista de puntos, listo para mostrar. */
  protected readonly mensajeError = computed(() => {
    const error = this.ruta.error() ?? this.puntos.error();
    if (!error) return null;
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return (error.error as ProblemDetail | null)?.detail ?? 'La solicitud no es válida.';
    }
    return ERROR_CONEXION;
  });

  /** Lo que se anuncia al lector de pantalla cuando cambia el resultado. */
  protected readonly anuncio = computed(() => {
    switch (this.vista()) {
      case 'cargando':
        return 'Calculando ruta…';
      case 'resultado': {
        const r = this.ruta.value()!;
        return `Ruta accesible encontrada: ${metros(r.distanciaM ?? 0)} desde ${r.origen.nombre} hasta ${r.destino.nombre}.`;
      }
      case 'pasos':
        return `Paso a paso listo: ${this.ruta.value()?.pasos?.length ?? 0} iteraciones. Pulse Reproducir o Paso siguiente.`;
      default:
        return '';
    }
  });

  /** Elemento que recibe el foco al cambiar de vista ('titulo-panel' o el selector de destino). */
  private enfoque = 'titulo-panel';

  constructor() {
    // Cada ruta nueva empieza en el paso 1
    effect(() => {
      this.ruta.value();
      this.indicePaso.set(0);
    });

    // Al cambiar de vista por una acción del usuario, el foco va al título del panel
    let primeraVista = true;
    effect(() => {
      this.vista();
      if (primeraVista) {
        primeraVista = false;
        return;
      }
      const id = this.enfoque;
      this.enfoque = 'titulo-panel';
      afterNextRender(() => document.getElementById(id)?.focus(), { injector: this.injector });
    });
  }

  protected calcular(solicitud: SolicitudRuta): void {
    void this.router.navigate(['/ruta'], { queryParams: { ...solicitud } });
  }

  protected cambiarModo(modo: Modo): void {
    void this.router.navigate(['/ruta'], { queryParams: { origen: this.origen(), destino: this.destino(), modo } });
  }

  protected nuevaConsulta(): void {
    void this.router.navigate(['/'], { queryParams: { origen: this.origen(), destino: this.destino() } });
  }

  protected cambiarDestino(): void {
    this.enfoque = 'destino';
    void this.router.navigate(['/'], { queryParams: { origen: this.origen(), destino: this.destino() } });
  }

  protected reintentar(): void {
    this.puntos.reload();
    this.grafo.reload();
    this.ruta.reload();
  }

  private entero(valor: unknown): number | null {
    const n = Number(valor);
    return typeof valor === 'string' && Number.isInteger(n) ? n : null;
  }
}
