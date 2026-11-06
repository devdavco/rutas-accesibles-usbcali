import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  isDevMode,
  signal,
  viewChild,
} from '@angular/core';
import type {
  GeoJSONSource,
  LngLatBoundsLike,
  Map as MapaLibre,
  Marker,
} from 'maplibre-gl';
import type { Feature, FeatureCollection, LineString, Point } from 'geojson';
import { GrafoVisual, PasoDijkstra, PuntoVisual, RespuestaRuta } from '../../api/tipos';
import { EstadoPunto, ETIQUETA_ESTADO, estadoDePunto } from '../../dominio/estado-punto';
import { nombreCorto, separarNombre } from '../../dominio/formato';
import { mitadesDelPlano } from '../../dominio/plano';

/** Qué dibuja el mapa: entradas y etiquetas, una ruta, o el avance de Dijkstra. */
export type VistaMapa = 'consulta' | 'resultado' | 'pasos';

interface Calibracion {
  imagen: string;
  esquinas: [[number, number], [number, number], [number, number], [number, number]];
}

const ESTILO_BASE = 'https://tiles.openfreemap.org/styles/positron';

/** Margen alrededor del plano dentro del cual se puede mover la vista (grados). */
const MARGEN_VISTA = 0.004;

const VARIABLE_ESTADO: Record<EstadoPunto, string> = {
  DEFINITIVO: '--color-dij-definitivo',
  EN_REVISION: '--color-dij-actual',
  CONOCIDO: '--color-dij-conocido',
  SIN_VISITAR: '--color-dij-sinvisitar',
};

/** Píxeles que cada mitad del plano se extiende más allá de la diagonal que las separa. */
const SOLAPE_PX = 2;

const COLECCION_VACIA: FeatureCollection = { type: 'FeatureCollection', features: [] };

/**
 * Mapa del campus: mapa base de OpenFreeMap atenuado, plano del campus superpuesto y las
 * capas de la vista actual. Solo visualiza: los cálculos llegan hechos desde el API.
 */
@Component({
  selector: 'app-mapa',
  templateUrl: './mapa.html',
  styleUrl: './mapa.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Mapa {
  readonly vista = input.required<VistaMapa>();
  readonly grafo = input<GrafoVisual>();
  readonly ruta = input<RespuestaRuta>();
  readonly paso = input<PasoDijkstra>();

  protected readonly soloPlano = signal(false);
  protected readonly error = signal(false);
  protected readonly estados = Object.entries(ETIQUETA_ESTADO).map(([estado, etiqueta]) => ({
    etiqueta,
    variable: VARIABLE_ESTADO[estado as EstadoPunto],
  }));

  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');
  private readonly cargado = signal(false);
  private mapa?: MapaLibre;
  private crearMarcador?: (elemento: HTMLElement, opciones: ConstructorParameters<typeof Marker>[0]) => Marker;
  private capasBase: string[] = [];
  private limitesCampus?: [[number, number], [number, number]];
  private marcadores: Marker[] = [];

  private readonly sinMovimiento = matchMedia('(prefers-reduced-motion: reduce)').matches;

  /** Puntos del grafo por id. */
  private readonly puntosPorId = computed(
    () => new Map((this.grafo()?.puntos ?? []).map((p) => [p.id, p] as const)),
  );

  constructor() {
    afterNextRender(() => void this.iniciar());
    inject(DestroyRef).onDestroy(() => this.mapa?.remove());

    // Capas que dependen de la vista, el grafo y la ruta
    effect(() => {
      if (!this.cargado()) return;
      this.dibujarVista(this.vista(), this.grafo(), this.ruta());
    });

    // Capas del paso a paso: cambian en cada paso
    effect(() => {
      if (!this.cargado()) return;
      this.dibujarPaso(this.vista() === 'pasos' ? this.paso() : undefined);
    });
  }

  protected acercar(): void {
    this.mapa?.zoomIn({ duration: this.duracion() });
  }

  protected alejar(): void {
    this.mapa?.zoomOut({ duration: this.duracion() });
  }

  /** Oculta o muestra el mapa base; el plano del campus siempre queda visible. */
  protected alternarSoloPlano(): void {
    this.soloPlano.update((v) => !v);
    for (const id of this.capasBase) {
      this.mapa?.setLayoutProperty(id, 'visibility', this.soloPlano() ? 'none' : 'visible');
    }
  }

  private async iniciar(): Promise<void> {
    try {
      // MapLibre se carga bajo demanda: no pesa en el arranque de la página
      const [maplibre, calibracion] = await Promise.all([
        import('maplibre-gl'),
        fetch('plano/calibracion.json').then((r) => r.json() as Promise<Calibracion>),
      ]);
      const imagenPlano = new Image();
      imagenPlano.src = calibracion.imagen;
      const planoListo = imagenPlano.decode();
      maplibre.setWorkerUrl(new URL('maplibre/maplibre-gl-worker.mjs', document.baseURI).href);
      this.crearMarcador = (elemento, opciones) => new maplibre.Marker({ ...opciones, element: elemento });

      const lngs = calibracion.esquinas.map((e) => e[0]);
      const lats = calibracion.esquinas.map((e) => e[1]);
      this.limitesCampus = [
        [Math.min(...lngs), Math.min(...lats)],
        [Math.max(...lngs), Math.max(...lats)],
      ];
      const [[oeste, sur], [este, norte]] = this.limitesCampus;

      const mapa = new maplibre.Map({
        container: this.contenedor().nativeElement,
        style: ESTILO_BASE,
        bounds: this.limitesCampus,
        maxBounds: [
          [oeste - MARGEN_VISTA, sur - MARGEN_VISTA],
          [este + MARGEN_VISTA, norte + MARGEN_VISTA],
        ],
        attributionControl: false, // la atribución se muestra como texto propio (ver plantilla)
        dragRotate: false,
        pitchWithRotate: false,
        fadeDuration: this.sinMovimiento ? 0 : 300,
      });
      this.mapa = mapa;
      if (isDevMode()) {
        // Solo en desarrollo: permite inspeccionar el mapa desde la consola o con Playwright
        (window as unknown as { mapaRutas?: MapaLibre }).mapaRutas = mapa;
      }
      mapa.getCanvas().setAttribute('aria-label', 'Mapa del campus. La información de la ruta está en el panel.');
      mapa.touchZoomRotate.disableRotation();

      mapa.on('load', async () => {
        this.capasBase = mapa.getStyle().layers.map((capa) => capa.id);
        mapa.setMinZoom(mapa.getZoom() - 0.5);
        await planoListo;
        this.agregarPlano(imagenPlano, calibracion.esquinas);
        this.agregarCapas();
        new ResizeObserver(() => mapa.resize()).observe(this.contenedor().nativeElement);
        this.cargado.set(true);
      });
      mapa.on('error', (evento) => console.error('Error del mapa', evento.error));
    } catch (e) {
      console.error('No se pudo iniciar el mapa', e);
      this.error.set(true);
    }
  }

  /**
   * El plano se dibuja como dos mitades triangulares para colocarlo igual que el prototipo
   * (MapLibre 5), con el que se calibraron las esquinas. Ver dominio/plano.ts.
   */
  private agregarPlano(imagen: HTMLImageElement, esquinas: Calibracion['esquinas']): void {
    const ancho = imagen.naturalWidth;
    const alto = imagen.naturalHeight;
    // Cada triángulo se extiende un poco más allá de la diagonal para que no quede una costura
    // (en la diagonal las dos mitades coinciden). La normal apunta hacia abajo a la derecha.
    const largo = Math.hypot(ancho, alto);
    const normal: [number, number] = [(alto / largo) * SOLAPE_PX, (ancho / largo) * SOLAPE_PX];

    for (const mitad of mitadesDelPlano(esquinas)) {
      const lienzo = document.createElement('canvas');
      lienzo.width = ancho;
      lienzo.height = alto;
      const ctx = lienzo.getContext('2d')!;
      const [a, b, c] = mitad.triangulo.map(([x, y]) => [x * ancho, y * alto]);
      const mas = ([x, y]: number[]) => [x + normal[0], y + normal[1]];
      const menos = ([x, y]: number[]) => [x - normal[0], y - normal[1]];
      // Contorno en orden: el triángulo más una franja angosta al otro lado de la diagonal
      const contorno =
        mitad.id === 'plano-superior'
          ? [a, b, mas(b), mas(c), c] // diagonal b → c, franja hacia abajo a la derecha
          : [menos(a), a, b, c, menos(c)]; // diagonal a → c, franja hacia arriba a la izquierda
      ctx.beginPath();
      for (const [x, y] of contorno) {
        ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.clip();
      ctx.drawImage(imagen, 0, 0);

      this.mapa!.addSource(mitad.id, { type: 'canvas', canvas: lienzo, coordinates: mitad.esquinas, animate: false });
      this.mapa!.addLayer({ id: mitad.id, type: 'raster', source: mitad.id, paint: { 'raster-fade-duration': 0 } });
    }
  }

  private agregarCapas(): void {
    const mapa = this.mapa!;
    const blanco = this.token('--color-superficie');

    for (const fuente of ['tramos', 'arbol', 'ruta', 'puntos']) {
      mapa.addSource(fuente, { type: 'geojson', data: COLECCION_VACIA });
    }

    // Grafo completo (paso a paso): tramos grises con borde blanco
    mapa.addLayer({
      id: 'tramos-borde', type: 'line', source: 'tramos',
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': blanco, 'line-width': this.medida('--mapa-tramo-borde-ancho') },
    });
    mapa.addLayer({
      id: 'tramos', type: 'line', source: 'tramos',
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': this.token('--color-tramo-grafo'), 'line-width': this.medida('--mapa-tramo-ancho') },
    });
    // Árbol de predecesores del paso actual, coloreado por el estado de cada punto
    mapa.addLayer({
      id: 'arbol', type: 'line', source: 'arbol',
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': ['get', 'color'], 'line-width': this.medida('--mapa-arbol-ancho') },
    });
    // Ruta encontrada: línea del color primario con borde blanco
    mapa.addLayer({
      id: 'ruta-borde', type: 'line', source: 'ruta',
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': blanco, 'line-width': this.medida('--mapa-ruta-borde-ancho') },
    });
    mapa.addLayer({
      id: 'ruta', type: 'line', source: 'ruta',
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': this.token('--color-primario'), 'line-width': this.medida('--mapa-ruta-ancho') },
    });
    // Puntos: entradas (consulta y resultado) o todos los puntos con su estado (paso a paso)
    mapa.addLayer({
      id: 'puntos', type: 'circle', source: 'puntos',
      paint: {
        'circle-color': ['get', 'color'],
        'circle-radius': ['get', 'radio'],
        'circle-stroke-color': blanco,
        'circle-stroke-width': this.medida('--mapa-punto-borde'),
      },
    });
  }

  /** Dibuja lo que no cambia entre pasos: ruta, grafo completo, entradas y etiquetas. */
  private dibujarVista(vista: VistaMapa, grafo: GrafoVisual | undefined, ruta: RespuestaRuta | undefined): void {
    const linea = ruta?.geojson?.features.find((f) => f.geometry.type === 'LineString');
    this.fuente('ruta').setData({
      type: 'FeatureCollection',
      features: vista === 'resultado' && linea ? [linea as Feature<LineString>] : [],
    });
    this.fuente('tramos').setData(vista === 'pasos' && grafo ? this.lineasDelGrafo(grafo) : COLECCION_VACIA);

    if (vista !== 'pasos') {
      const entradas = (grafo?.puntos ?? []).filter((p) => p.seleccionable);
      this.fuente('puntos').setData(
        this.coleccionPuntos(entradas, () => this.token('--color-texto'), () => this.medida('--mapa-punto-radio')),
      );
    }

    this.ponerEtiquetas(vista, grafo, ruta);
    this.encuadrar(vista, linea?.geometry.coordinates as [number, number][] | undefined);
  }

  /** Colorea los puntos y el árbol de predecesores según el paso actual. */
  private dibujarPaso(paso: PasoDijkstra | undefined): void {
    const grafo = this.grafo();
    if (!paso || !grafo) {
      this.fuente('arbol').setData(COLECCION_VACIA);
      return;
    }
    this.fuente('puntos').setData(
      this.coleccionPuntos(
        grafo.puntos,
        (p) => this.token(VARIABLE_ESTADO[estadoDePunto(paso, p.id)]),
        (p) => this.medida(estadoDePunto(paso, p.id) === 'EN_REVISION' ? '--mapa-punto-radio-actual' : '--mapa-punto-radio'),
      ),
    );
    const arbol: Feature<LineString>[] = [];
    for (const [clave, predecesor] of Object.entries(paso.predecesores)) {
      const desde = this.puntosPorId().get(predecesor);
      const hasta = this.puntosPorId().get(Number(clave));
      if (!desde || !hasta) continue;
      arbol.push({
        type: 'Feature',
        properties: { color: this.token(VARIABLE_ESTADO[estadoDePunto(paso, hasta.id)]) },
        geometry: { type: 'LineString', coordinates: [[desde.lng, desde.lat], [hasta.lng, hasta.lat]] },
      });
    }
    this.fuente('arbol').setData({ type: 'FeatureCollection', features: arbol });
  }

  /** Etiquetas como marcadores HTML (decorativas: la misma información está en el panel). */
  private ponerEtiquetas(vista: VistaMapa, grafo: GrafoVisual | undefined, ruta: RespuestaRuta | undefined): void {
    this.marcadores.forEach((m) => m.remove());
    this.marcadores = [];
    if (!grafo) return;

    const porId = this.puntosPorId();
    const origen = ruta ? porId.get(ruta.origen.id) : undefined;
    const destino = ruta ? porId.get(ruta.destino.id) : undefined;

    if (vista === 'pasos') {
      if (destino) this.marcar(destino, 'Destino', 'destino', 'bottom');
      return;
    }
    // Una etiqueta por edificio (en su primera entrada) y una por cada entrada del campus
    const grupos = new Map<string, PuntoVisual[]>();
    for (const p of grafo.puntos.filter((p) => p.seleccionable)) {
      const clave = p.edificio ?? separarNombre(p.nombre).principal;
      grupos.set(clave, [...(grupos.get(clave) ?? []), p]);
    }
    const enRuta = vista === 'resultado' && ruta?.encontrada;
    // En móvil no hay espacio para las etiquetas de edificio (como en los diseños móviles)
    if (this.esMovil()) grupos.clear();
    for (const [texto, puntos] of grupos) {
      // En el resultado, el origen y el destino llevan su propia etiqueta
      if (enRuta && puntos.some((p) => p.id === origen?.id || p.id === destino?.id)) continue;
      this.marcar(puntos[0], texto, '', 'bottom');
    }
    if (enRuta && origen && destino) {
      this.marcar(origen, `Origen: ${nombreCorto(origen.nombre, origen.edificio)}`, 'origen', 'top');
      this.marcar(destino, `Destino: ${nombreCorto(destino.nombre, destino.edificio)}`, 'destino', 'bottom');
    }
  }

  private marcar(punto: PuntoVisual, texto: string, clase: string, ancla: 'top' | 'bottom'): void {
    const elemento = document.createElement('div');
    elemento.className = `pastilla-mapa ${clase}`.trim();
    elemento.textContent = texto;
    elemento.setAttribute('aria-hidden', 'true');
    const separacion = this.medida('--espacio-8');
    const marcador = this.crearMarcador!(elemento, {
      anchor: ancla,
      offset: [0, ancla === 'top' ? separacion : -separacion],
    });
    this.marcadores.push(marcador.setLngLat([punto.lng, punto.lat]).addTo(this.mapa!));
  }

  /**
   * En escritorio se ve siempre el campus completo (como en el diseño); en móvil, el resultado
   * se acerca a la ruta dejando libres los botones de arriba.
   */
  private encuadrar(vista: VistaMapa, linea: [number, number][] | undefined): void {
    const mapa = this.mapa!;
    const lados = this.medida('--mapa-margen-movil-lados');
    const opciones = {
      padding: {
        top: this.medida('--mapa-margen-movil-arriba'),
        bottom: this.medida('--mapa-margen-movil-abajo'),
        left: lados,
        right: lados,
      },
      duration: this.duracion(),
    };
    if (this.esMovil() && vista === 'resultado' && linea && linea.length > 1) {
      const lngs = linea.map((c) => c[0]);
      const lats = linea.map((c) => c[1]);
      const limites: LngLatBoundsLike = [
        [Math.min(...lngs), Math.min(...lats)],
        [Math.max(...lngs), Math.max(...lats)],
      ];
      mapa.fitBounds(limites, opciones);
    } else if (this.limitesCampus) {
      mapa.fitBounds(this.limitesCampus, { padding: 0, duration: this.duracion() });
    }
  }

  private lineasDelGrafo(grafo: GrafoVisual): FeatureCollection<LineString> {
    const porId = this.puntosPorId();
    return {
      type: 'FeatureCollection',
      features: grafo.tramos
        .filter((t) => t.condicion !== 'ESCALERAS') // excluidos del grafo
        .flatMap((t) => {
          const a = porId.get(t.puntoInicialId);
          const b = porId.get(t.puntoFinalId);
          return a && b
            ? [{ type: 'Feature' as const, properties: {}, geometry: { type: 'LineString' as const, coordinates: [[a.lng, a.lat], [b.lng, b.lat]] } }]
            : [];
        }),
    };
  }

  private coleccionPuntos(
    puntos: PuntoVisual[],
    color: (p: PuntoVisual) => string,
    radio: (p: PuntoVisual) => number,
  ): FeatureCollection<Point> {
    return {
      type: 'FeatureCollection',
      features: puntos.map((p) => ({
        type: 'Feature',
        properties: { color: color(p), radio: radio(p) },
        geometry: { type: 'Point', coordinates: [p.lng, p.lat] },
      })),
    };
  }

  private fuente(id: string): GeoJSONSource {
    return this.mapa!.getSource(id) as GeoJSONSource;
  }

  /** Lee un token de diseño (MapLibre no entiende var(--…)). */
  private token(nombre: string): string {
    return getComputedStyle(document.documentElement).getPropertyValue(nombre).trim();
  }

  private medida(nombre: string): number {
    return parseFloat(this.token(nombre));
  }

  private esMovil(): boolean {
    return this.contenedor().nativeElement.clientWidth <= this.medida('--mapa-ancho-movil');
  }

  private duracion(): number {
    return this.sinMovimiento ? 0 : 500;
  }
}
