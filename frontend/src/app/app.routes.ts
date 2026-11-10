import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard').then(m => m.Dashboard) },
  { path: 'empresas', loadComponent: () => import('./pages/companies').then(m => m.Companies) },
  { path: 'etapas', loadComponent: () => import('./pages/stages').then(m => m.Stages) },
  { path: '**', redirectTo: '' }
];
