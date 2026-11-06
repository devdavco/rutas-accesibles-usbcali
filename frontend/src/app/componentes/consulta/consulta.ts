import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output } from '@angular/core';
import { PuntoSeleccionable } from '../../api/tipos';
import { GrupoPuntos, SelectorPunto } from '../selector-punto/selector-punto';

export type Modo = 'directa' | 'pasos';

export interface SolicitudRuta {
  origen: number;
  destino: number;
  modo: Modo;
}

/** Formulario de consulta: origen, destino y cómo mostrar el resultado. */
@Component({
  selector: 'app-consulta',
  imports: [SelectorPunto],
  templateUrl: './consulta.html',
  styleUrl: './consulta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Consulta {
  readonly puntos = input.required<PuntoSeleccionable[]>();
  readonly origenInicial = input<number | null>(null);
  readonly destinoInicial = input<number | null>(null);
  readonly modoInicial = input<Modo>('directa');
  readonly calcular = output<SolicitudRuta>();

  // Empiezan con lo que venga en la URL y luego los cambia el usuario
  protected readonly origen = linkedSignal(() => this.origenInicial());
  protected readonly destino = linkedSignal(() => this.destinoInicial());
  protected readonly modo = linkedSignal(() => this.modoInicial());

  protected readonly puedeCalcular = computed(() => this.origen() !== null && this.destino() !== null);

  /** Entradas del campus primero; luego un grupo por edificio, en el orden del API. */
  protected readonly grupos = computed<GrupoPuntos[]>(() => {
    const grupos = new Map<string, PuntoSeleccionable[]>();
    for (const punto of this.puntos()) {
      const clave = punto.edificio ?? 'Entradas del campus';
      grupos.set(clave, [...(grupos.get(clave) ?? []), punto]);
    }
    return [...grupos].map(([etiqueta, puntos]) => ({ etiqueta, puntos }));
  });

  protected intercambiar(): void {
    const origen = this.origen();
    this.origen.set(this.destino());
    this.destino.set(origen);
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();
    const origen = this.origen();
    const destino = this.destino();
    if (origen !== null && destino !== null) {
      this.calcular.emit({ origen, destino, modo: this.modo() });
    }
  }
}
