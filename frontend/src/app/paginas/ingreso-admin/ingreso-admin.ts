import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { AdminApiService } from '../../api/admin-api.service';
import { ProblemDetail } from '../../api/tipos';
import { Sesion } from '../../servicios/sesion';

/** Ingreso del administrador (pantalla 05 del diseño). */
@Component({
  selector: 'app-ingreso-admin',
  templateUrl: './ingreso-admin.html',
  styleUrl: './ingreso-admin.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IngresoAdmin {
  private readonly api = inject(AdminApiService);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly correo = signal('');
  protected readonly contrasena = signal('');
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Se intentó enviar con campos vacíos: solo entonces se marcan como inválidos. */
  protected readonly faltanCampos = signal(false);
  /** Llegó aquí porque el token venció (lo marcan el guard y el interceptor). */
  protected readonly sesionExpirada = toSignal(
    inject(ActivatedRoute).queryParamMap.pipe(map((p) => p.get('motivo') === 'expirada')),
    { initialValue: false },
  );

  constructor() {
    if (this.sesion.activa()) {
      void this.router.navigate(['/admin/editor']);
    }
  }

  protected escribir(campo: 'correo' | 'contrasena', evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;
    (campo === 'correo' ? this.correo : this.contrasena).set(valor);
  }

  protected ingresar(evento: Event): void {
    evento.preventDefault();
    const correo = this.correo().trim();
    if (!correo || !this.contrasena()) {
      this.faltanCampos.set(true);
      this.error.set('Escriba el correo y la contraseña.');
      return;
    }
    this.faltanCampos.set(false);
    this.enviando.set(true);
    this.error.set(null);
    this.api.ingresar(correo, this.contrasena()).subscribe({
      next: (respuesta) => {
        this.sesion.iniciar(respuesta.token, correo, respuesta.expiraEn);
        void this.router.navigate(['/admin/editor']);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.contrasena.set('');
        // 401 (credenciales) y 429 (bloqueo) traen su mensaje; lo demás es un problema de conexión
        const detalle =
          e instanceof HttpErrorResponse && (e.status === 401 || e.status === 429)
            ? (e.error as ProblemDetail | null)?.detail
            : null;
        this.error.set(detalle ?? 'No se pudo conectar con el servidor. Intente de nuevo.');
      },
    });
  }
}
