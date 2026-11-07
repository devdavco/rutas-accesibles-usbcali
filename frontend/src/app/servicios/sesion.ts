import { Injectable, computed, signal } from '@angular/core';

interface DatosSesion {
  token: string;
  correo: string;
  /** Instante ISO 8601 en que vence el token */
  expiraEn: string;
}

const CLAVE = 'rutas.sesion';

/**
 * Sesión del administrador. El token se guarda en sessionStorage: se borra al cerrar la pestaña
 * (decisión aceptada para la tesis, ver docs/seguridad.md).
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly datos = signal<DatosSesion | null>(leer());

  readonly token = computed(() => this.datos()?.token ?? null);
  readonly correo = computed(() => this.datos()?.correo ?? null);

  /** Hay token y todavía no vence. */
  activa(): boolean {
    const datos = this.datos();
    return datos !== null && Date.parse(datos.expiraEn) > Date.now();
  }

  iniciar(token: string, correo: string, expiraEn: string): void {
    const datos = { token, correo, expiraEn };
    this.datos.set(datos);
    try {
      sessionStorage.setItem(CLAVE, JSON.stringify(datos));
    } catch {
      // Sin sessionStorage (modo privado estricto): la sesión dura mientras la página siga abierta
    }
  }

  cerrar(): void {
    this.datos.set(null);
    try {
      sessionStorage.removeItem(CLAVE);
    } catch {
      // nada que borrar
    }
  }
}

function leer(): DatosSesion | null {
  try {
    const texto = sessionStorage.getItem(CLAVE);
    return texto ? (JSON.parse(texto) as DatosSesion) : null;
  } catch {
    return null;
  }
}
