import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { Api } from './api';

const superAdminGuard: CanActivateFn = () =>
  inject(Api).session()?.role === 'SUPER_ADMIN' ? true : inject(Router).createUrlTree(['/']);

export const routes: Routes = [
  {
    path: 'admin/empresas-pendentes',
    canActivate: [superAdminGuard],
    loadComponent: () => import('./pages/admin-companies').then((m) => m.AdminCompanies),
  },
  {
    path: 'candidaturas/:id/editar',
    loadComponent: () => import('./pages/application-detail').then((m) => m.ApplicationDetail),
  },
  {
    path: 'candidaturas/:id',
    data: { readOnly: true },
    loadComponent: () => import('./pages/application-detail').then((m) => m.ApplicationDetail),
  },
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pages/dashboard').then((m) => m.Dashboard),
  },
  { path: 'empresas', loadComponent: () => import('./pages/companies').then((m) => m.Companies) },
  { path: 'etapas', loadComponent: () => import('./pages/stages').then((m) => m.Stages) },
  { path: '**', redirectTo: '' },
];
