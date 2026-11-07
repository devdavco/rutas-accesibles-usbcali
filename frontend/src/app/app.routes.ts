import { Routes } from '@angular/router';
import { EditorGrafo } from './paginas/editor-grafo/editor-grafo';
import { IngresoAdmin } from './paginas/ingreso-admin/ingreso-admin';
import { PaginaRutas } from './paginas/pagina-rutas/pagina-rutas';
import { sesionActivaGuard } from './servicios/sesion-activa.guard';

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
  { path: 'admin', pathMatch: 'full', redirectTo: 'admin/editor' },
  { path: 'admin/ingresar', component: IngresoAdmin, title: 'Ingreso de administrador · Rutas accesibles USB Cali' },
  {
    path: 'admin/editor',
    component: EditorGrafo,
    canActivate: [sesionActivaGuard],
    title: 'Editor del grafo · Rutas accesibles USB Cali',
  },
  { path: '**', redirectTo: '' },
];
