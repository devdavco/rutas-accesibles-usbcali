import { Routes } from '@angular/router';
import { AdminPendiente } from './paginas/admin-pendiente/admin-pendiente';
import { PaginaRutas } from './paginas/pagina-rutas/pagina-rutas';

export const routes: Routes = [
  {
    // La consulta ('') y la ruta ('ruta') comparten la página: así el mapa no se vuelve a crear
    path: '',
    component: PaginaRutas,
    children: [
      { path: '', children: [] },
      { path: 'ruta', children: [] },
    ],
  },
  { path: 'admin', component: AdminPendiente, title: 'Ingreso administrador · Rutas accesibles USB Cali' },
  { path: '**', redirectTo: '' },
];
