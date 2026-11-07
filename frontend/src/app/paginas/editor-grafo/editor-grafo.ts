import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { AdminApiService } from '../../api/admin-api.service';
import { RutasApiService } from '../../api/rutas-api.service';
import { Condicion, ProblemDetail, TipoPunto } from '../../api/tipos';
import { DatosPunto, PuntoAdmin, TramoAdmin } from '../../api/tipos-admin';
import { MapaEditor, Ubicacion } from '../../componentes/mapa-editor/mapa-editor';
import { metros, nombreVisible, numero } from '../../dominio/formato';
import { ETIQUETA_CONDICION } from '../../dominio/recorrido-texto';

type Modo = 'tramos' | 'puntos';

/** Lo que se está editando en el panel. */
type Seleccion =
  | { tipo: 'tramo'; id: number }
  | { tipo: 'nuevo-tramo' }
  | { tipo: 'punto'; id: number }
  | { tipo: 'nuevo-punto' }
  | null;

export const CONDICIONES: { valor: Condicion; descripcion: string }[] = [
  { valor: 'PLANO', descripcion: 'Plano · factor 1' },
  { valor: 'RAMPA', descripcion: 'Rampa · factor 1' },
  { valor: 'PENDIENTE_FUERTE', descripcion: 'Pendiente fuerte · factor 2' },
  { valor: 'ESCALERAS', descripcion: 'Escaleras · excluido' },
];

export const TIPOS: { valor: TipoPunto; etiqueta: string }[] = [
  { valor: 'ENTRADA_CAMPUS', etiqueta: 'Entrada del campus' },
  { valor: 'ENTRADA_EDIFICIO', etiqueta: 'Entrada de edificio' },
  { valor: 'CRUCE', etiqueta: 'Cruce (solo de paso)' },
];

const PUNTO_VACIO: DatosPunto = { nombre: '', tipo: 'CRUCE', edificio: null, seleccionable: false, lat: NaN, lng: NaN };

/** Quita tildes y mayúsculas para buscar ("tecnologico" encuentra "Tecnológico"). */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('es-CO');
}

/**
 * Editor del grafo (pantalla 06 del diseño). Todo lo que se puede hacer en el mapa también se
 * puede hacer desde el panel con el teclado (listas con búsqueda y coordenadas editables).
 */
@Component({
  selector: 'app-editor-grafo',
  imports: [MapaEditor, NgTemplateOutlet],
  templateUrl: './editor-grafo.html',
  styleUrl: './editor-grafo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorGrafo {
  private readonly admin = inject(AdminApiService);
  private readonly rutas = inject(RutasApiService);

  protected readonly condiciones = CONDICIONES;
  protected readonly tipos = TIPOS;
  protected readonly etiquetaCondicion = ETIQUETA_CONDICION;
  protected readonly nombreVisible = nombreVisible;

  // Datos guardados
  protected readonly puntos = signal<PuntoAdmin[]>([]);
  protected readonly tramos = signal<TramoAdmin[]>([]);
  /** Peso de cada tramo según servicio-grafos (null: escaleras). */
  private readonly pesos = signal(new Map<number, number | null>());
  protected readonly cargando = signal(true);

  // Estado del panel
  protected readonly modo = signal<Modo>('tramos');
  protected readonly seleccion = signal<Seleccion>(null);
  protected readonly busqueda = signal('');
  protected readonly condicion = signal<Condicion>('PLANO');
  protected readonly nuevoInicial = signal<number | null>(null);
  protected readonly nuevoFinal = signal<number | null>(null);
  protected readonly formPunto = signal<DatosPunto>(PUNTO_VACIO);
  /** Ubicación nueva de un punto existente tras arrastrarlo, a la espera de confirmación. */
  protected readonly ubicacionPendiente = signal<Ubicacion | null>(null);
  protected readonly confirmandoEliminar = signal(false);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly porId = computed(() => new Map(this.puntos().map((p) => [p.id, p] as const)));
  protected readonly puntosOrdenados = computed(() =>
    [...this.puntos()].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es-CO', { numeric: true })),
  );

  protected readonly tramoActual = computed(() => {
    const s = this.seleccion();
    return s?.tipo === 'tramo' ? this.tramos().find((t) => t.id === s.id) : undefined;
  });
  protected readonly puntoActual = computed(() => {
    const s = this.seleccion();
    return s?.tipo === 'punto' ? this.porId().get(s.id) : undefined;
  });

  protected readonly listaTramos = computed(() => {
    const filtro = normalizar(this.busqueda());
    return this.tramos()
      .map((t) => ({ id: t.id, texto: `${this.nombreTramo(t)} · ${ETIQUETA_CONDICION[t.condicion]}` }))
      .filter((t) => normalizar(t.texto).includes(filtro));
  });
  protected readonly listaPuntos = computed(() => {
    const filtro = normalizar(this.busqueda());
    return this.puntosOrdenados()
      .map((p) => ({ id: p.id, texto: `${p.nombre} · ${TIPOS.find((t) => t.valor === p.tipo)!.etiqueta}` }))
      .filter((p) => normalizar(p.texto).includes(filtro));
  });

  protected readonly resumen = computed(() => {
    const tramos = this.tramos();
    const contar = (c: Condicion) => tramos.filter((t) => t.condicion === c).length;
    const partes = [`${tramos.length} tramos`, `${contar('PLANO')} planos`, `${contar('RAMPA')} rampas`];
    if (contar('PENDIENTE_FUERTE')) partes.push(`${contar('PENDIENTE_FUERTE')} con pendiente fuerte`);
    partes.push(`${contar('ESCALERAS')} con escaleras`);
    return `${partes.join(' · ')} · ${this.puntos().length} puntos`;
  });

  protected readonly longitudActual = computed(() => {
    const t = this.tramoActual();
    return t ? metros(t.longitudM) : '';
  });
  protected readonly pesoActual = computed(() => {
    const t = this.tramoActual();
    if (!t) return '';
    const peso = this.pesos().get(t.id);
    return peso === null || peso === undefined ? 'Excluido (escaleras)' : numero(peso);
  });

  protected readonly tramosDelPunto = computed(() => {
    const id = this.puntoActual()?.id;
    return id === undefined ? 0 : this.tramos().filter((t) => t.puntoInicialId === id || t.puntoFinalId === id).length;
  });

  /** Hay cambios sin guardar en el elemento seleccionado. */
  protected readonly hayCambios = computed(() => {
    const s = this.seleccion();
    if (s?.tipo === 'tramo') return this.condicion() !== this.tramoActual()?.condicion;
    if (s?.tipo === 'punto') {
      const p = this.puntoActual();
      const f = this.formPunto();
      return !!p && (p.nombre !== f.nombre || p.tipo !== f.tipo || (p.edificio ?? '') !== (f.edificio ?? '')
        || p.seleccionable !== f.seleccionable || p.lat !== f.lat || p.lng !== f.lng || this.ubicacionPendiente() !== null);
    }
    return s !== null;
  });

  // Lo que resalta el mapa
  protected readonly tramoResaltado = computed(() => this.tramoActual()?.id ?? null);
  protected readonly puntosResaltados = computed(() => {
    const s = this.seleccion();
    const t = this.tramoActual();
    if (t) return [t.puntoInicialId, t.puntoFinalId];
    if (s?.tipo === 'nuevo-tramo') return [this.nuevoInicial(), this.nuevoFinal()].filter((id): id is number => id !== null);
    if (s?.tipo === 'punto') return [s.id];
    return [];
  });
  protected readonly marcador = computed<Ubicacion | null>(() => {
    const s = this.seleccion();
    if (s?.tipo !== 'punto' && s?.tipo !== 'nuevo-punto') return null;
    const pendiente = this.ubicacionPendiente();
    if (pendiente) return pendiente;
    const { lat, lng } = this.formPunto();
    return Number.isFinite(lat) && Number.isFinite(lng) ? { lat, lng } : null;
  });

  constructor() {
    this.recargar();
  }

  protected nombreTramo(tramo: { puntoInicialId: number; puntoFinalId: number }): string {
    const nombre = (id: number) => nombreVisible(this.porId().get(id)?.nombre ?? `Punto ${id}`);
    return `${nombre(tramo.puntoInicialId)} – ${nombre(tramo.puntoFinalId)}`;
  }

  // --- Selección ---------------------------------------------------------------------------

  protected cambiarModo(modo: Modo): void {
    this.modo.set(modo);
    this.busqueda.set('');
    this.limpiarSeleccion();
  }

  protected elegirTramo(id: number): void {
    const tramo = this.tramos().find((t) => t.id === id);
    if (!tramo) return;
    this.modo.set('tramos');
    this.limpiarSeleccion();
    this.seleccion.set({ tipo: 'tramo', id });
    this.condicion.set(tramo.condicion);
  }

  /** Clic en un punto del mapa: si se está creando un tramo, elige sus extremos. */
  protected elegirPunto(id: number): void {
    if (this.seleccion()?.tipo === 'nuevo-tramo') {
      if (this.nuevoInicial() === null || this.nuevoFinal() !== null) {
        this.nuevoInicial.set(id);
        this.nuevoFinal.set(null);
      } else {
        this.nuevoFinal.set(id);
      }
      return;
    }
    const punto = this.porId().get(id);
    if (!punto) return;
    this.modo.set('puntos');
    this.limpiarSeleccion();
    this.seleccion.set({ tipo: 'punto', id });
    this.formPunto.set({ ...punto });
  }

  protected elegirDeLista(evento: Event): void {
    const id = Number((evento.target as HTMLSelectElement).value);
    if (this.modo() === 'tramos') this.elegirTramo(id);
    else this.elegirPunto(id);
  }

  protected clicLibre(ubicacion: Ubicacion): void {
    if (this.seleccion()?.tipo === 'nuevo-punto') {
      this.formPunto.update((f) => ({ ...f, lat: ubicacion.lat, lng: ubicacion.lng }));
    }
  }

  protected marcadorMovido(ubicacion: Ubicacion): void {
    if (this.seleccion()?.tipo === 'nuevo-punto') {
      this.formPunto.update((f) => ({ ...f, lat: ubicacion.lat, lng: ubicacion.lng }));
    } else {
      // Punto existente: se pide confirmación antes de guardar la nueva ubicación
      this.ubicacionPendiente.set(ubicacion);
    }
  }

  protected empezarTramo(): void {
    this.limpiarSeleccion();
    this.seleccion.set({ tipo: 'nuevo-tramo' });
    this.condicion.set('PLANO');
  }

  protected empezarPunto(): void {
    this.limpiarSeleccion();
    this.seleccion.set({ tipo: 'nuevo-punto' });
    this.formPunto.set({ ...PUNTO_VACIO });
  }

  /** Descarta los cambios sin guardar del elemento seleccionado. */
  protected cancelar(): void {
    const s = this.seleccion();
    this.confirmandoEliminar.set(false);
    this.ubicacionPendiente.set(null);
    this.error.set(null);
    if (s?.tipo === 'tramo') this.condicion.set(this.tramoActual()!.condicion);
    else if (s?.tipo === 'punto') this.formPunto.set({ ...this.puntoActual()! });
    else this.limpiarSeleccion();
  }

  // --- Formulario de punto -----------------------------------------------------------------

  protected cambiarCampo(campo: 'nombre' | 'edificio' | 'lat' | 'lng', evento: Event): void {
    const texto = (evento.target as HTMLInputElement).value;
    this.formPunto.update((f) => ({
      ...f,
      [campo]: campo === 'lat' || campo === 'lng' ? (texto === '' ? NaN : Number(texto)) : texto,
    }));
  }

  protected cambiarTipo(evento: Event): void {
    const tipo = (evento.target as HTMLSelectElement).value as TipoPunto;
    // Un cruce nunca es seleccionable y una entrada del campus no tiene edificio
    this.formPunto.update((f) => ({
      ...f,
      tipo,
      seleccionable: tipo === 'CRUCE' ? false : f.seleccionable,
      edificio: tipo === 'ENTRADA_EDIFICIO' ? f.edificio : null,
    }));
  }

  protected cambiarSeleccionable(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).checked;
    this.formPunto.update((f) => ({ ...f, seleccionable: valor }));
  }

  // --- Guardar y eliminar ------------------------------------------------------------------

  protected guardarTramo(): void {
    const s = this.seleccion();
    if (s?.tipo === 'nuevo-tramo') {
      const inicial = this.nuevoInicial();
      const final = this.nuevoFinal();
      if (inicial === null || final === null) {
        this.error.set('Elija los dos puntos del tramo (en las listas o haciendo clic en el mapa).');
        return;
      }
      this.ejecutar(this.admin.crearTramo({ puntoInicialId: inicial, puntoFinalId: final, condicion: this.condicion() }),
        (creado) => this.elegirTramo(creado.id));
    } else if (s?.tipo === 'tramo' && this.hayCambios()) {
      this.ejecutar(this.admin.cambiarCondicion(s.id, this.condicion()), (t) => this.elegirTramo(t.id));
    }
  }

  protected guardarPunto(): void {
    const s = this.seleccion();
    const datos = this.datosPunto(this.formPunto());
    if (!datos) return;
    if (s?.tipo === 'nuevo-punto') {
      this.ejecutar(this.admin.crearPunto(datos), (p) => this.elegirPunto(p.id));
    } else if (s?.tipo === 'punto') {
      this.ejecutar(this.admin.editarPunto(s.id, datos), (p) => this.elegirPunto(p.id));
    }
  }

  /** Confirma la ubicación a la que se arrastró el punto (sus tramos se recalculan). */
  protected guardarUbicacion(): void {
    const punto = this.puntoActual();
    const ubicacion = this.ubicacionPendiente();
    if (!punto || !ubicacion) return;
    const { id, ...guardado } = punto;
    this.ejecutar(this.admin.editarPunto(id, { ...guardado, ...ubicacion }), (p) => this.elegirPunto(p.id));
  }

  protected eliminar(): void {
    const s = this.seleccion();
    if (s?.tipo === 'tramo') {
      this.ejecutar(this.admin.eliminarTramo(s.id), () => this.limpiarSeleccion());
    } else if (s?.tipo === 'punto') {
      this.ejecutar(this.admin.eliminarPunto(s.id), () => this.limpiarSeleccion());
    }
  }

  /** Valida en el navegador lo que se puede validar, para avisar antes de enviar. */
  private datosPunto(f: DatosPunto): DatosPunto | null {
    const nombre = f.nombre.trim();
    const edificio = f.edificio?.trim() || null;
    let problema: string | null = null;
    if (!nombre) problema = 'Escriba el nombre del punto.';
    else if (!Number.isFinite(f.lat) || !Number.isFinite(f.lng)) {
      problema = 'Indique la ubicación: haga clic en el mapa o escriba la latitud y la longitud.';
    } else if (f.tipo === 'ENTRADA_EDIFICIO' && !edificio) problema = 'Una entrada de edificio debe indicar el edificio.';
    if (problema) {
      this.error.set(problema);
      return null;
    }
    return { ...f, nombre, edificio, seleccionable: f.tipo === 'CRUCE' ? false : f.seleccionable };
  }

  /** Envía un cambio, recarga los datos y muestra "Cambios guardados" o el error del API. */
  private ejecutar<T>(peticion: Observable<T>, alTerminar: (resultado: T) => void): void {
    this.guardando.set(true);
    this.mensaje.set(null);
    this.error.set(null);
    peticion.subscribe({
      next: (resultado) => {
        this.recargar(() => {
          alTerminar(resultado);
          this.mensaje.set('Cambios guardados');
        });
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.confirmandoEliminar.set(false);
        const detalle = e instanceof HttpErrorResponse ? (e.error as ProblemDetail | null)?.detail : null;
        this.error.set(detalle ?? 'No se pudo guardar. Revise la conexión e intente de nuevo.');
      },
    });
  }

  /** Vuelve a leer puntos, tramos y pesos (los pesos los calcula servicio-grafos). */
  private recargar(despues?: () => void): void {
    forkJoin({ puntos: this.admin.puntos(), tramos: this.admin.tramos(), grafo: this.rutas.grafo() }).subscribe({
      next: ({ puntos, tramos, grafo }) => {
        this.puntos.set(puntos);
        this.tramos.set(tramos);
        this.pesos.set(new Map(grafo.tramos.map((t) => [t.id, t.pesoCalculado] as const)));
        this.cargando.set(false);
        this.guardando.set(false);
        despues?.();
      },
      error: () => {
        this.cargando.set(false);
        this.guardando.set(false);
        this.error.set('No se pudieron cargar los datos del grafo. Revise la conexión e intente de nuevo.');
      },
    });
  }

  private limpiarSeleccion(): void {
    this.seleccion.set(null);
    this.nuevoInicial.set(null);
    this.nuevoFinal.set(null);
    this.ubicacionPendiente.set(null);
    this.confirmandoEliminar.set(false);
    this.error.set(null);
    this.mensaje.set(null);
  }
}
