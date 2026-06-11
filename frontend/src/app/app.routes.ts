import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { clientAreaGuard } from './core/auth/guards/client-area.guard';
import { roleGuard } from './core/auth/guards/role.guard';
import { ROLES } from './core/auth/constants/auth.constants';

const comingSoon = () =>
  import('./features/shared/coming-soon/coming-soon.component').then(
    (m) => m.ComingSoonComponent
  );

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
      import('./features/layout/advisor-shell/advisor-shell.component').then(
        (m) => m.AdvisorShellComponent
      ),
    canActivate: [authGuard, roleGuard],
    data: { roles: [ROLES.CONSEILLER] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/advisor/advisor-dashboard.component').then(
            (m) => m.AdvisorDashboardComponent
          ),
        data: {
          title: 'Tableau de bord',
          description: 'Vue d’ensemble de votre activité d’instruction.',
        },
      },
      {
        path: 'dossiers',
        loadComponent: () =>
          import('./features/loans/advisor/loan-applications-list/advisor-loan-applications-list.component').then(
            (m) => m.AdvisorLoanApplicationsListComponent
          ),
        data: {
          title: 'Mes dossiers',
          description: 'Liste des demandes de prêt à instruire.',
        },
      },
      {
        path: 'dossiers/:id',
        loadComponent: () =>
          import('./features/loans/advisor/loan-application-detail/advisor-loan-application-detail.component').then(
            (m) => m.AdvisorLoanApplicationDetailComponent
          ),
        data: {
          title: '',
          description: '',
          breadcrumb: 'Détail du dossier',
        },
      },
      {
        path: 'prets',
        loadComponent: comingSoon,
        data: {
          title: 'Prêts',
          description: 'Suivi des prêts accordés — bientôt disponible.',
        },
      },
      {
        path: 'messages',
        loadComponent: comingSoon,
        data: { title: 'Messages', description: 'Messagerie conseiller — bientôt disponible.' },
      },
      {
        path: 'notifications',
        loadComponent: comingSoon,
        data: {
          title: 'Notifications',
          description: 'Centre de notifications — bientôt disponible.',
        },
      },
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
        data: { title: 'Mon profil', description: 'Gérez votre profil et la sécurité de votre compte.' },
      },
      {
        path: 'aide',
        loadComponent: comingSoon,
        data: { title: 'Aide', description: 'FAQ et support — bientôt disponible.' },
      },
    ],
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./features/layout/admin-shell/admin-shell.component').then(
        (m) => m.AdminShellComponent
      ),
    canActivate: [authGuard, roleGuard],
    data: { roles: [ROLES.ADMIN] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/admin/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent
          ),
        data: {
          title: 'Tableau de bord',
          description: 'Supervision de la plateforme LoanlyFans.',
        },
      },
      {
        path: 'demandes',
        loadComponent: () =>
          import('./features/loans/admin/loan-applications-list/admin-loan-applications-list.component').then(
            (m) => m.AdminLoanApplicationsListComponent
          ),
        data: {
          title: 'Toutes les demandes',
          description: "Liste de l’ensemble des demandes de prêt.",
        },
      },
      {
        path: 'demandes/:id',
        loadComponent: () =>
          import('./features/loans/admin/loan-application-detail/admin-loan-application-detail.component').then(
            (m) => m.AdminLoanApplicationDetailComponent
          ),
        data: {
          title: 'Détail de la demande',
          description: 'Supervision du dossier de demande de prêt.',
        },
      },
      {
        path: 'prets',
        loadComponent: comingSoon,
        data: {
          title: 'Prêts',
          description: 'Prêts actifs sur la plateforme — bientôt disponible.',
        },
      },
      {
        path: 'utilisateurs',
        loadComponent: comingSoon,
        data: {
          title: 'Utilisateurs',
          description: 'Gestion des comptes — bientôt disponible.',
        },
      },
      {
        path: 'parametres',
        loadComponent: comingSoon,
        data: {
          title: 'Paramètres',
          description: 'Paramétrage plateforme — bientôt disponible.',
        },
      },
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
        data: { title: 'Mon profil', description: 'Gérez votre profil et la sécurité de votre compte.' },
      },
      {
        path: 'aide',
        loadComponent: comingSoon,
        data: { title: 'Aide', description: 'FAQ et support — bientôt disponible.' },
      },
    ],
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
          import('./features/dashboard/client/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
        data: {
          title: 'Tableau de bord',
          description:
            'Vue d’ensemble de votre espace : suivez vos demandes et accédez rapidement aux actions utiles.',
        },
      },
      {
        path: 'mes-demandes',
        loadComponent: () =>
          import('./features/loans/applicant/loan-list/loan-list.component').then(
            (m) => m.LoanListComponent
          ),
        data: {
          title: 'Mes demandes',
          description: "Gérez et suivez l'ensemble de vos demandes.",
        },
      },
      {
        path: 'mes-demandes/:id',
        loadComponent: () =>
          import('./features/loans/applicant/loan-detail/loan-detail.component').then(
            (m) => m.LoanDetailComponent
          ),
        data: {
          title: 'Détail de la demande',
          description: 'Suivez l’avancement de votre dossier et consultez les informations associées.',
        },
      },
      {
        path: 'nouvelle-demande/:id',
        loadComponent: () =>
          import('./features/loans/applicant/loan-wizard/loan-wizard.component').then(
            (m) => m.LoanWizardComponent
          ),
        data: {
          title: 'Reprendre la demande',
          description: '',
        },
      },
      {
        path: 'nouvelle-demande',
        loadComponent: () =>
          import('./features/loans/applicant/loan-wizard/loan-wizard.component').then(
            (m) => m.LoanWizardComponent
          ),
        data: {
          title: 'Nouvelle demande',
          description: '',
        },
      },
      {
        path: 'demande-soumise/:id',
        loadComponent: () =>
          import('./features/loans/applicant/loan-submitted/loan-submitted.component').then(
            (m) => m.LoanSubmittedComponent
          ),
        data: {
          title: 'Demande soumise',
          description: 'Votre dossier a été transmis avec succès.',
        },
      },
      {
        path: 'mes-prets',
        loadComponent: comingSoon,
        data: {
          title: 'Mes prêts',
          description:
            'Retrouvez vos prêts en cours, les montants et les échéances associées.',
        },
      },
      {
        path: 'paiements',
        loadComponent: comingSoon,
        data: {
          title: 'Paiements / Échéancier',
          description:
            'Visualisez vos prochains prélèvements et téléchargez votre planning de remboursement.',
        },
      },
      {
        path: 'documents',
        loadComponent: comingSoon,
        data: {
          title: 'Documents',
          description:
            'Centralisez vos justificatifs et les documents fournis par LoanlyFans.',
        },
      },
      {
        path: 'messages',
        loadComponent: comingSoon,
        data: {
          title: 'Messages',
          description:
            'Échangez avec nos équipes et consultez l’historique de vos conversations.',
        },
      },
      {
        path: 'notifications',
        loadComponent: comingSoon,
        data: {
          title: 'Notifications',
          description:
            'Retrouvez vos alertes, rappels et mises à jour importantes sur vos dossiers.',
        },
      },
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
        data: {
          title: 'Mon profil',
          description:
            'Mettez à jour votre email et changez votre mot de passe.',
        },
      },
      {
        path: 'parametres',
        loadComponent: comingSoon,
        data: {
          title: 'Paramètres',
          description:
            'Gérez la sécurité du compte, les préférences d’affichage et les notifications.',
        },
      },
      {
        path: 'aide',
        loadComponent: comingSoon,
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
