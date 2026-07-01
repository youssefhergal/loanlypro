import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { clientAreaGuard } from './core/auth/guards/client-area.guard';
import { roleGuard } from './core/auth/guards/role.guard';
import { ROLES } from './core/auth/constants/auth.constants';

const comingSoon = () =>
  import('./features/shared/coming-soon/coming-soon.component').then(
    (m) => m.ComingSoonComponent
  );

const helpFaq = () =>
  import('./features/help/help-faq.component').then((m) => m.HelpFaqComponent);

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
          description: "Vue d'ensemble de votre activité d'instruction.",
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
        loadComponent: () =>
          import('./features/loans/repayment/advisor/advisor-loan-repayment-list/advisor-loan-repayment-list.component').then(
            (m) => m.AdvisorLoanRepaymentListComponent
          ),
        data: {
          title: 'Prêts',
          description: 'Suivi des remboursements de vos clients — consultation uniquement.',
        },
      },
      {
        path: 'prets/:id',
        loadComponent: () =>
          import('./features/loans/repayment/advisor/advisor-loan-repayment-detail/advisor-loan-repayment-detail.component').then(
            (m) => m.AdvisorLoanRepaymentDetailComponent
          ),
        data: {
          title: '',
          description: '',
          breadcrumb: 'Détail du prêt',
        },
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/messaging/messaging.component').then((m) => m.MessagingComponent),
        data: { title: 'Messages', description: 'Messagerie en temps réel.' },
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(
            (m) => m.NotificationsComponent
          ),
        data: {
          title: 'Notifications',
          description: 'Centre de notifications et alertes dossier.',
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
        loadComponent: helpFaq,
        data: { title: 'Aide', description: 'FAQ et support.' },
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
          description: 'Supervision de la plateforme LoanlyPro.',
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
          description: "Liste de l'ensemble des demandes de prêt.",
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
        loadComponent: () =>
          import('./features/loans/repayment/admin/admin-loan-dashboard/admin-loan-dashboard.component').then(
            (m) => m.AdminLoanDashboardComponent,
          ),
        data: {
          title: 'Prêts',
          description: 'Supervision du portefeuille de prêts et indicateurs de recouvrement.',
        },
      },
      {
        path: 'prets/:id',
        loadComponent: () =>
          import('./features/loans/repayment/admin/admin-loan-detail/admin-loan-detail.component').then(
            (m) => m.AdminLoanDetailComponent,
          ),
        data: {
          title: '',
          description: '',
        },
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/messaging/messaging.component').then((m) => m.MessagingComponent),
        data: { title: 'Messages', description: 'Messagerie en temps réel.' },
      },
      {
        path: 'utilisateurs',
        loadComponent: () =>
          import('./features/users/admin-users/admin-users.component').then(
            (m) => m.AdminUsersComponent,
          ),
        data: {
          title: 'Utilisateurs',
          description: 'Gestion des comptes (liste, filtres, création conseiller/admin).',
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
        loadComponent: helpFaq,
        data: { title: 'Aide', description: 'FAQ et support.' },
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
            "Vue d'ensemble de votre espace : suivez vos demandes et accédez rapidement aux actions utiles.",
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
          description: "Suivez l'avancement de votre dossier et consultez les informations associées.",
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
        loadComponent: () =>
          import('./features/loans/repayment/client/my-loans/my-loans.component').then(
            (m) => m.MyLoansComponent
          ),
        data: {
          title: 'Mes prêts',
          description:
            'Retrouvez vos prêts en cours, les montants et les échéances associées.',
        },
      },
      {
        path: 'mes-prets/:id/mandat',
        loadComponent: () =>
          import('./features/loans/repayment/client/mandate-setup/mandate-setup.component').then(
            (m) => m.MandateSetupComponent
          ),
        data: {
          title: '',
          description: '',
        },
      },
      {
        path: 'mes-prets/:id',
        loadComponent: () =>
          import('./features/loans/repayment/client/my-loan-detail/my-loan-detail.component').then(
            (m) => m.MyLoanDetailComponent
          ),
        data: {
          title: '',
          description: '',
          breadcrumb: 'Détail du prêt',
        },
      },
      {
        path: 'paiements',
        loadComponent: () =>
          import('./features/loans/repayment/client/payments-schedule/payments-schedule.component').then(
            (m) => m.PaymentsScheduleComponent
          ),
        data: {
          title: 'Paiements / Échéancier',
          description:
            "Visualisez vos prochains prélèvements et l\'historique des transactions.",
          breadcrumb: 'Paiements / Échéancier',
        },
      },
      {
        path: 'documents',
        loadComponent: () =>
          import('./features/documents/documents.component').then((m) => m.DocumentsComponent),
        data: {
          title: 'Documents',
          description:
            "Retrouvez vos justificatifs, vos documents de crédit et vos exports d\'échéancier.",
          breadcrumb: 'Documents',
        },
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/messaging/messaging.component').then((m) => m.MessagingComponent),
        data: {
          title: 'Messages',
          description: "Échangez avec nos équipes et consultez l\'historique de vos conversations.",
        },
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(
            (m) => m.NotificationsComponent
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
            "Gérez la sécurité du compte, les préférences d'affichage et les notifications.",
        },
      },
      {
        path: 'aide',
        loadComponent: helpFaq,
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
