import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import type { GeoJSONSource, Map as MapaLibre, MapLayerMouseEvent, Marker } from 'maplibre-gl';
import type { FeatureCollection, LineString, Point } from 'geojson';
import { Condicion } from '../../api/tipos';
import { PuntoAdmin, TramoAdmin } from '../../api/tipos-admin';
import { ETIQUETA_CONDICION } from '../../dominio/recorrido-texto';
import { ControlesMapa } from '../mapa/controles-mapa';
import { COLECCION_VACIA, Coordenada, crearMapaCampus, medida, token } from '../mapa/mapa-campus';

export interface Ubicacion {
  lat: number;
  lng: number;
}

const COLOR_CONDICION: Record<Condicion, string> = {
  PLANO: '--color-plano',
  RAMPA: '--color-rampa',
  PENDIENTE_FUERTE: '--color-pendiente',
  ESCALERAS: '--color-escaleras',
};

/**
 * Mapa del editor del grafo: todos los tramos coloreados por condición (escaleras punteadas) y
 * todos los puntos. Hacer clic selecciona; el punto en edición se puede arrastrar. Solo dibuja:
 * las longitudes las calcula PostGIS y los pesos, servicio-grafos.
 */
@Component({
  selector: 'app-mapa-editor',
  imports: [ControlesMapa],
  templateUrl: './mapa-editor.html',
  styleUrl: './mapa-editor.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapaEditor {
  readonly puntos = input.required<PuntoAdmin[]>();
  readonly tramos = input.required<TramoAdmin[]>();
  /** Tramo resaltado. */
  readonly tramoSeleccionado = input<number | null>(null);
  /** Puntos resaltados (extremos del tramo, punto en edición o puntos elegidos para un tramo nuevo). */
  readonly puntosResaltados = input<number[]>([]);
  /** Ubicación del marcador arrastrable (punto en edición o punto nuevo); null para ocultarlo. */
  readonly marcador = input<Ubicacion | null>(null);

  readonly elegirTramo = output<number>();
  readonly elegirPunto = output<number>();
  readonly clicLibre = output<Ubicacion>();
  readonly marcadorMovido = output<Ubicacion>();

  protected readonly error = signal(false);
  protected readonly mapaListo = signal<MapaLibre | undefined>(undefined);
  protected readonly capasBase = signal<string[]>([]);
  protected readonly leyenda = (Object.keys(COLOR_CONDICION) as Condicion[]).map((condicion) => ({
    condicion,
    etiqueta: condicion === 'ESCALERAS' ? 'Escaleras (excluido)' : ETIQUETA_CONDICION[condicion],
    color: `var(${COLOR_CONDICION[condicion]})`,
  }));

  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');
  private mapa?: MapaLibre;
  private crearMarcador?: (elemento: HTMLElement, opciones: ConstructorParameters<typeof Marker>[0]) => Marker;
  private arrastrable?: Marker;

  constructor() {
    afterNextRender(() => void this.iniciar());
    inject(DestroyRef).onDestroy(() => this.mapa?.remove());

    effect(() => {
      const mapa = this.mapaListo();
      if (!mapa) return;
      this.dibujarGrafo(this.puntos(), this.tramos(), this.tramoSeleccionado(), this.puntosResaltados());
    });

    effect(() => {
      if (!this.mapaListo()) return;
      this.ponerMarcador(this.marcador());
    });
  }

  private async iniciar(): Promise<void> {
    try {
      const base = await crearMapaCampus(
        this.contenedor().nativeElement,
        'Mapa del grafo del campus. Los tramos y puntos también se pueden elegir en las listas del panel.',
      );
      this.mapa = base.mapa;
      this.crearMarcador = base.crearMarcador;
      this.agregarCapas();
      new ResizeObserver(() => base.mapa.resize()).observe(this.contenedor().nativeElement);
      this.capasBase.set(base.capasBase);
      this.mapaListo.set(base.mapa);
    } catch (e) {
      console.error('No se pudo iniciar el mapa del editor', e);
      this.error.set(true);
    }
  }

  private agregarCapas(): void {
    const mapa = this.mapa!;
    const blanco = token('--color-superficie');
    const colorPorCondicion = [
      'match',
      ['get', 'condicion'],
      ...(Object.entries(COLOR_CONDICION) as [Condicion, string][]).flatMap(([c, v]) => [c, token(v)]),
      token('--color-tramo-grafo'),
    ] as unknown as string;

    mapa.addSource('tramos', { type: 'geojson', data: COLECCION_VACIA });
    mapa.addSource('puntos', { type: 'geojson', data: COLECCION_VACIA });

    // Tramo seleccionado: halo del color primario debajo de su línea
    mapa.addLayer({
      id: 'tramo-seleccionado', type: 'line', source: 'tramos', filter: ['==', ['get', 'seleccionado'], true],
      layout: { 'line-cap': 'round' },
      paint: { 'line-color': token('--color-primario'), 'line-width': medida('--mapa-tramo-seleccionado-ancho'), 'line-opacity': 0.35 },
    });
    mapa.addLayer({
      id: 'tramos-borde', type: 'line', source: 'tramos', filter: ['!=', ['get', 'condicion'], 'ESCALERAS'],
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': blanco, 'line-width': medida('--mapa-tramo-borde-ancho') },
    });
    mapa.addLayer({
      id: 'tramos', type: 'line', source: 'tramos', filter: ['!=', ['get', 'condicion'], 'ESCALERAS'],
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': colorPorCondicion, 'line-width': medida('--mapa-arbol-ancho') },
    });
    // Escaleras: línea punteada (el tramo existe pero no entra al grafo)
    mapa.addLayer({
      id: 'tramos-escaleras', type: 'line', source: 'tramos', filter: ['==', ['get', 'condicion'], 'ESCALERAS'],
      paint: { 'line-color': token('--color-escaleras'), 'line-width': medida('--mapa-arbol-ancho'), 'line-dasharray': [1.5, 1] },
    });
    // Capa invisible y más ancha para que sea fácil tocar un tramo
    mapa.addLayer({
      id: 'tramos-toque', type: 'line', source: 'tramos',
      paint: { 'line-color': blanco, 'line-opacity': 0, 'line-width': medida('--area-tactil') / 2 },
    });
    mapa.addLayer({
      id: 'puntos', type: 'circle', source: 'puntos',
      paint: {
        'circle-radius': ['case', ['get', 'resaltado'], medida('--mapa-punto-radio-actual'), medida('--mapa-punto-radio')],
        'circle-color': ['case', ['==', ['get', 'tipo'], 'CRUCE'], blanco, token('--color-texto')],
        'circle-stroke-color': ['case', ['get', 'resaltado'], token('--color-primario'), token('--color-texto')],
        'circle-stroke-width': ['case', ['get', 'resaltado'], medida('--borde-alerta') / 2, medida('--mapa-punto-borde') / 2],
      },
    });

    // Clic: primero los puntos (son más pequeños), luego los tramos; si no hay nada, clic libre
    mapa.on('click', (evento: MapLayerMouseEvent) => {
      const [punto] = mapa.queryRenderedFeatures(evento.point, { layers: ['puntos'] });
      if (punto) {
        this.elegirPunto.emit(Number(punto.properties['id']));
        return;
      }
      const [tramo] = mapa.queryRenderedFeatures(evento.point, { layers: ['tramos-toque'] });
      if (tramo) {
        this.elegirTramo.emit(Number(tramo.properties['id']));
        return;
      }
      this.clicLibre.emit({ lat: evento.lngLat.lat, lng: evento.lngLat.lng });
    });
    for (const capa of ['puntos', 'tramos-toque']) {
      mapa.on('mouseenter', capa, () => (mapa.getCanvas().style.cursor = 'pointer'));
      mapa.on('mouseleave', capa, () => (mapa.getCanvas().style.cursor = ''));
    }
  }

  private dibujarGrafo(puntos: PuntoAdmin[], tramos: TramoAdmin[], tramoId: number | null, resaltados: number[]): void {
    const porId = new Map(puntos.map((p) => [p.id, p] as const));
    const lineas: FeatureCollection<LineString> = {
      type: 'FeatureCollection',
      features: tramos.flatMap((t) => {
        const a = porId.get(t.puntoInicialId);
        const b = porId.get(t.puntoFinalId);
        if (!a || !b) return [];
        return [{
          type: 'Feature' as const,
          properties: { id: t.id, condicion: t.condicion, seleccionado: t.id === tramoId },
          geometry: { type: 'LineString' as const, coordinates: [[a.lng, a.lat], [b.lng, b.lat]] as Coordenada[] },
        }];
      }),
    };
    const circulos: FeatureCollection<Point> = {
      type: 'FeatureCollection',
      features: puntos.map((p) => ({
        type: 'Feature' as const,
        properties: { id: p.id, tipo: p.tipo, resaltado: resaltados.includes(p.id) },
        geometry: { type: 'Point' as const, coordinates: [p.lng, p.lat] },
      })),
    };
    (this.mapa!.getSource('tramos') as GeoJSONSource).setData(lineas);
    (this.mapa!.getSource('puntos') as GeoJSONSource).setData(circulos);
  }

  /** Marcador arrastrable del punto en edición; al soltarlo avisa la nueva ubicación (sin guardar). */
  private ponerMarcador(ubicacion: Ubicacion | null): void {
    if (!ubicacion) {
      this.arrastrable?.remove();
      this.arrastrable = undefined;
      return;
    }
    if (!this.arrastrable) {
      const elemento = document.createElement('div');
      elemento.className = 'marcador-edicion';
      elemento.setAttribute('aria-hidden', 'true');
      this.arrastrable = this.crearMarcador!(elemento, { draggable: true })
        .setLngLat([ubicacion.lng, ubicacion.lat])
        .addTo(this.mapa!);
      this.arrastrable.on('dragend', () => {
        const { lat, lng } = this.arrastrable!.getLngLat();
        this.marcadorMovido.emit({ lat, lng });
      });
    } else {
      this.arrastrable.setLngLat([ubicacion.lng, ubicacion.lat]);
    }
  }
}
