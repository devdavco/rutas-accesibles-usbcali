import { Injectable, signal } from '@angular/core';

/** Igual al punto de quiebre de styles/_quiebres.scss. */
const CONSULTA_MOVIL = '(max-width: 767px)';

/** Si la pantalla es móvil, como signal (cambia al girar el teléfono o redimensionar). */
@Injectable({ providedIn: 'root' })
export class Pantalla {
  // matchMedia no existe en algunos entornos (por ejemplo, las pruebas con jsdom)
  private readonly consulta = typeof matchMedia === 'function' ? matchMedia(CONSULTA_MOVIL) : null;
  readonly esMovil = signal(this.consulta?.matches ?? false);

  constructor() {
    this.consulta?.addEventListener('change', (e) => this.esMovil.set(e.matches));
  }
}
