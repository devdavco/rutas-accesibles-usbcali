import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RespuestaRuta } from '../../api/tipos';
import { nombreVisible } from '../../dominio/formato';

/** No existe una ruta accesible entre origen y destino (RF06). */
@Component({
  selector: 'app-sin-ruta',
  templateUrl: './sin-ruta.html',
  styleUrl: './sin-ruta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SinRuta {
  readonly respuesta = input.required<RespuestaRuta>();
  readonly cambiarDestino = output<void>();
  protected readonly nombreVisible = nombreVisible;
}
