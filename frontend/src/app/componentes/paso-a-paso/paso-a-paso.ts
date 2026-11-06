import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core';
import { PasoDijkstra, RespuestaRuta } from '../../api/tipos';
import { ETIQUETA_ESTADO_TABLA, EstadoPunto } from '../../dominio/estado-punto';
import { numero, porcentaje } from '../../dominio/formato';
import { filasDelPaso } from '../../dominio/tabla-pasos';

/** Filas de la tabla que se muestran antes de "Mostrar todos". */
const FILAS_VISIBLES = 6;

/** Milisegundos entre pasos para cada nivel de velocidad (1 = lenta, 5 = rápida). */
const INTERVALOS = [1600, 1000, 600, 300, 120];

const VARIABLE_ESTADO: Record<EstadoPunto, string> = {
  DEFINITIVO: 'var(--color-dij-definitivo)',
  EN_REVISION: 'var(--color-dij-actual)',
  CONOCIDO: 'var(--color-dij-conocido)',
  SIN_VISITAR: 'var(--color-dij-sinvisitar)',
};

/**
 * Animación de Dijkstra paso a paso. No arranca sola: empieza detenida en el paso 1.
 * Mientras se reproduce, el lector de pantalla solo oye "Reproduciendo" (no cada paso);
 * con "Paso siguiente" o en pausa oye el mensaje del paso.
 */
@Component({
  selector: 'app-paso-a-paso',
  templateUrl: './paso-a-paso.html',
  styleUrl: './paso-a-paso.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasoAPaso {
  readonly respuesta = input.required<RespuestaRuta>();
  readonly nombres = input.required<ReadonlyMap<number, string>>();
  /** Índice (desde 0) del paso mostrado; el mapa lo usa para colorear los puntos. */
  readonly indice = model(0);
  readonly volver = output<void>();

  protected readonly reproduciendo = signal(false);
  protected readonly velocidad = signal(3);
  protected readonly anuncio = signal('');

  protected readonly pasos = computed<PasoDijkstra[]>(() => this.respuesta().pasos ?? []);
  protected readonly total = computed(() => this.pasos().length);
  protected readonly paso = computed(() => this.pasos()[this.indice()]);
  protected readonly alFinal = computed(() => this.indice() >= this.total() - 1);
  protected readonly avance = computed(() => (this.total() > 1 ? this.indice() / (this.total() - 1) : 1));
  protected readonly textoAvance = computed(() => porcentaje(this.avance()));
  protected readonly filas = computed(() => (this.paso() ? filasDelPaso(this.paso(), this.nombres()) : []));
  protected readonly mostrarTodas = signal(false);
  protected readonly filasVisibles = computed(() =>
    this.mostrarTodas() ? this.filas() : this.filas().slice(0, FILAS_VISIBLES),
  );
  protected readonly hayMasFilas = computed(() => this.filas().length > FILAS_VISIBLES);
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_TABLA;
  protected readonly colorEstado = VARIABLE_ESTADO;
  protected readonly numero = numero;

  constructor() {
    // Reproducción: avanza un paso en cada intervalo mientras esté activa
    effect((alLimpiar) => {
      if (!this.reproduciendo()) return;
      const temporizador = setInterval(() => {
        if (this.alFinal()) {
          this.reproduciendo.set(false);
          this.anunciarPaso('Fin de la reproducción. ');
          return;
        }
        this.indice.update((i) => i + 1);
      }, INTERVALOS[this.velocidad() - 1]);
      alLimpiar(() => clearInterval(temporizador));
    });
  }

  protected reproducir(): void {
    if (this.reproduciendo()) return;
    if (this.alFinal()) this.indice.set(0);
    this.reproduciendo.set(true);
    this.anuncio.set('Reproduciendo');
  }

  protected pausar(): void {
    if (!this.reproduciendo()) return;
    this.reproduciendo.set(false);
    this.anunciarPaso(`En pausa en el paso ${this.indice() + 1}. `);
  }

  protected siguiente(): void {
    if (this.alFinal()) return;
    this.reproduciendo.set(false);
    this.indice.update((i) => Math.min(i + 1, this.total() - 1));
    this.anunciarPaso('');
  }

  protected reiniciar(): void {
    this.reproduciendo.set(false);
    this.indice.set(0);
    this.anunciarPaso('');
  }

  protected cambiarVelocidad(evento: Event): void {
    this.velocidad.set(Number((evento.target as HTMLInputElement).value));
  }

  private anunciarPaso(prefijo: string): void {
    const paso = this.paso();
    if (paso) this.anuncio.set(`${prefijo}Paso ${paso.numero}. ${paso.mensaje}`);
  }
}
