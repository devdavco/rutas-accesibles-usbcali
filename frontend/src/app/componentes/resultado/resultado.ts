import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RespuestaRuta } from '../../api/tipos';
import { metros, numero, separarNombre } from '../../dominio/formato';
import { recorridoTexto } from '../../dominio/recorrido-texto';

/** Ruta encontrada: distancia, costo y recorrido en texto (RF05). */
@Component({
  selector: 'app-resultado',
  templateUrl: './resultado.html',
  styleUrl: './resultado.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Resultado {
  readonly respuesta = input.required<RespuestaRuta>();
  readonly nuevaConsulta = output<void>();
  readonly verPasos = output<void>();

  protected readonly distancia = computed(() => metros(this.respuesta().distanciaM ?? 0));
  protected readonly costo = computed(() => numero(this.respuesta().costo ?? 0));
  protected readonly desde = computed(() => separarNombre(this.respuesta().origen.nombre));
  protected readonly hasta = computed(() => separarNombre(this.respuesta().destino.nombre));
  protected readonly indicaciones = computed(() => recorridoTexto(this.respuesta().segmentos));

  /** La nota explica por qué el costo coincide o no con la distancia. */
  protected readonly notaCosto = computed(() => {
    const r = this.respuesta();
    if (r.segmentos.length === 0) {
      return 'El origen y el destino son el mismo punto.';
    }
    return r.costo === r.distanciaM
      ? 'Costo = metros × factor de cada tramo. Aquí coincide con la distancia porque la ruta solo tiene tramos planos y rampas.'
      : 'Costo = metros × factor de cada tramo. Es mayor que la distancia porque la ruta incluye pendiente fuerte, que cuenta el doble.';
  });
}
