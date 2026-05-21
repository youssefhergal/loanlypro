import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { clientAreaGuard } from './core/auth/guards/client-area.guard';
import { roleGuard } from './core/auth/guards/role.guard';
import { ROLES } from './core/auth/constants/auth.constants';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./features/auth/verify-email/verify-email.component').then(
        (m) => m.VerifyEmailComponent
      ),
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
  {
    path: '',
    loadComponent: () =>
      import('./features/layout/client-shell/client-shell.component').then(
        (m) => m.ClientShellComponent
      ),
    canActivate: [authGuard, clientAreaGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        data: {
          title: 'Tableau de bord',
          description:
            'Vue d’ensemble de votre espace : suivez vos demandes et accédez rapidement aux actions utiles.',
        },
      },
      {
        path: 'mes-demandes',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Mes demandes',
          description:
            'Consultez l’état de vos dossiers, les pièces demandées et les prochaines étapes.',
        },
      },
      {
        path: 'nouvelle-demande',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Nouvelle demande de prêt',
          description:
            'Démarrez une nouvelle demande et complétez les informations étape par étape.',
        },
      },
      {
        path: 'mes-prets',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Mes prêts',
          description:
            'Retrouvez vos prêts en cours, les montants et les échéances associées.',
        },
      },
      {
        path: 'paiements',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Paiements / Échéancier',
          description:
            'Visualisez vos prochains prélèvements et téléchargez votre planning de remboursement.',
        },
      },
      {
        path: 'documents',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Documents',
          description:
            'Centralisez vos justificatifs et les documents fournis par LoanlyFans.',
        },
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Messages',
          description:
            'Échangez avec nos équipes et consultez l’historique de vos conversations.',
        },
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Notifications',
          description:
            'Retrouvez vos alertes, rappels et mises à jour importantes sur vos dossiers.',
        },
      },
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Mon profil',
          description:
            'Mettez à jour vos coordonnées et vos préférences de compte.',
        },
      },
      {
        path: 'parametres',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Paramètres',
          description:
            'Gérez la sécurité du compte, les préférences d’affichage et les notifications.',
        },
      },
      {
        path: 'aide',
        loadComponent: () =>
          import('./features/client/client-placeholder.component').then(
            (m) => m.ClientPlaceholderComponent
          ),
        data: {
          title: 'Aide / FAQ',
          description:
            'Questions fréquentes et ressources pour vous accompagner dans votre parcours.',
        },
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
