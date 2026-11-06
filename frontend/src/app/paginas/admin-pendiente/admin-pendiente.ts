import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Página provisional: el ingreso de administrador se construye en otra etapa. */
@Component({
  selector: 'app-admin-pendiente',
  imports: [RouterLink],
  template: `
    <section class="pendiente">
      <h2>Ingreso administrador</h2>
      <p class="parrafo">Esta sección estará disponible en una próxima versión.</p>
      <a class="enlace" routerLink="/">Volver a la consulta de rutas</a>
    </section>
  `,
  styles: `
    .pendiente {
      max-width: var(--ancho-panel);
      margin: var(--espacio-40) auto;
      padding: 0 var(--espacio-16);
      display: flex;
      flex-direction: column;
      gap: var(--espacio-12);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminPendiente {}
