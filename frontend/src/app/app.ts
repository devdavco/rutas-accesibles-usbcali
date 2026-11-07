import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Sesion } from './servicios/sesion';

@Component({
  imports: [RouterOutlet, RouterLink],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly router = inject(Router);
  protected readonly sesion = inject(Sesion);

  private readonly ruta = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url.split('?')[0]),
    ),
    { initialValue: this.router.url.split('?')[0] },
  );

  /** El encabezado cambia según la sección: consulta, ingreso o editor. */
  protected readonly seccion = computed(() => {
    const ruta = this.ruta();
    if (ruta.startsWith('/admin/editor')) return 'editor';
    if (ruta.startsWith('/admin')) return 'ingreso';
    return 'consulta';
  });

  protected cerrarSesion(): void {
    this.sesion.cerrar();
    void this.router.navigate(['/admin/ingresar']);
  }
}
