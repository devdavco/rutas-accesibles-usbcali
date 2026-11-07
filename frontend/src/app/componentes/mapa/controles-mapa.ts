import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import type { Map as MapaLibre } from 'maplibre-gl';
import { duracion } from './mapa-campus';

/**
 * Botones del mapa (Acercar, Alejar, Solo plano) y atribución. Los comparten la consulta de
 * rutas y el editor del grafo. Se ubican sobre el mapa: el padre debe ser `position: relative`.
 */
@Component({
  selector: 'app-controles-mapa',
  templateUrl: './controles-mapa.html',
  styleUrl: './controles-mapa.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ControlesMapa {
  readonly mapa = input<MapaLibre>();
  readonly capasBase = input<string[]>([]);

  protected readonly soloPlano = signal(false);

  protected acercar(): void {
    this.mapa()?.zoomIn({ duration: duracion() });
  }

  protected alejar(): void {
    this.mapa()?.zoomOut({ duration: duracion() });
  }

  /** Oculta o muestra el mapa base; el plano del campus siempre queda visible. */
  protected alternarSoloPlano(): void {
    this.soloPlano.update((v) => !v);
    for (const id of this.capasBase()) {
      this.mapa()?.setLayoutProperty(id, 'visibility', this.soloPlano() ? 'none' : 'visible');
    }
  }
}
