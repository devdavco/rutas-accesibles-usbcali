import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { PuntoSeleccionable } from '../../api/tipos';
import { separarNombre } from '../../dominio/formato';

export interface GrupoPuntos {
  etiqueta: string;
  puntos: PuntoSeleccionable[];
}

/**
 * Selector nativo (<select> con <optgroup> por edificio): funciona con teclado y lectores de
 * pantalla. Encima se dibuja, solo como apariencia, el nombre con su detalle y "Cambiar".
 */
@Component({
  selector: 'app-selector-punto',
  templateUrl: './selector-punto.html',
  styleUrl: './selector-punto.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectorPunto {
  readonly idCampo = input.required<string>();
  readonly etiqueta = input.required<string>();
  readonly grupos = input.required<GrupoPuntos[]>();
  readonly valor = model<number | null>(null);

  protected readonly seleccionado = computed(() => {
    const id = this.valor();
    const punto = this.grupos().flatMap((g) => g.puntos).find((p) => p.id === id);
    return punto ? separarNombre(punto.nombre) : null;
  });

  protected cambiar(evento: Event): void {
    const texto = (evento.target as HTMLSelectElement).value;
    this.valor.set(texto === '' ? null : Number(texto));
  }
}
