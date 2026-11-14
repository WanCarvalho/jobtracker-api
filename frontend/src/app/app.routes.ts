import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'candidaturas/:id/editar', loadComponent: () => import('./pages/application-detail').then(m => m.ApplicationDetail) },
  { path: 'candidaturas/:id', data: { readOnly: true }, loadComponent: () => import('./pages/application-detail').then(m => m.ApplicationDetail) },
  { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard').then(m => m.Dashboard) },
  { path: 'empresas', loadComponent: () => import('./pages/companies').then(m => m.Companies) },
  { path: 'etapas', loadComponent: () => import('./pages/stages').then(m => m.Stages) },
  { path: '**', redirectTo: '' }
];
