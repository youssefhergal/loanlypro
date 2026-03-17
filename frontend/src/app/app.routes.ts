import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { roleGuard } from './core/auth/guards/role.guard';
import { ROLES } from './core/auth/constants/auth.constants';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: 'conseiller',
    loadComponent: () =>
      import('./features/conseiller/conseiller.component').then((m) => m.ConseillerComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: [ROLES.CONSEILLER] },
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./features/admin/admin.component').then((m) => m.AdminComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: [ROLES.ADMIN] },
  },
  { path: '**', redirectTo: 'dashboard' },
];
