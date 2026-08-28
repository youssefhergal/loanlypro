import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth/services/auth.service';
import { ROLES } from '../../core/auth/constants/auth.constants';
import { NotificationApiService } from '../../core/notifications/services/notification-api.service';
import { NotificationPageCoordinationService } from '../../core/notifications/services/notification-page-coordination.service';
import { NotificationUnreadService } from '../../core/notifications/services/notification-unread.service';
import { NotificationDto } from '../../core/notifications/models/notification.model';
import { groupNotificationsByPeriod } from '../../core/notifications/utils/notification-display.util';
import { getErrorMessage } from '../../core/loans/utils/api-error.util';
import { NotificationItemComponent } from '../../shared/notification-item/notification-item.component';

export type NotificationFilter = 'all' | 'unread';

interface NotificationsQuickLink {
  route: string;
  label: string;
  icon: string;
}

interface NotificationAlertType {
  icon: string;
  label: string;
  description: string;
}

const CLIENT_ASIDE_TIPS = [
  'Les notifications regroupent les mises à jour de vos demandes, documents et paiements.',
  'Cliquez sur une alerte pour consulter le dossier concerné — elle sera marquée comme lue.',
  'Utilisez le filtre « Non lues » pour retrouver rapidement ce qui nécessite votre attention.',
];

const ADVISOR_ASIDE_TIPS = [
  'Vous êtes alerté lors des actions client, des documents à traiter et des événements de paiement.',
  'Ouvrez une notification pour accéder directement au dossier associé.',
  'Le bouton « Tout marquer comme lu » est disponible en haut de page lorsque des alertes sont en attente.',
];

const CLIENT_ALERT_TYPES: NotificationAlertType[] = [
  {
    icon: 'folder_open',
    label: 'Dossier',
    description: 'Soumission, instruction, offre et décision.',
  },
  {
    icon: 'description',
    label: 'Documents',
    description: 'Justificatifs déposés, validés ou refusés.',
  },
  {
    icon: 'payments',
    label: 'Paiements',
    description: 'Prélèvements réussis, échecs et retards.',
  },
  {
    icon: 'verified_user',
    label: 'Mandat SEPA',
    description: 'Activation ou révocation du prélèvement.',
  },
];

const ADVISOR_ALERT_TYPES: NotificationAlertType[] = [
  {
    icon: 'inbox',
    label: 'Dossiers clients',
    description: 'Nouvelles demandes et mises à jour à instruire.',
  },
  {
    icon: 'upload_file',
    label: 'Justificatifs',
    description: 'Pièces déposées ou complétées par le client.',
  },
  {
    icon: 'money_off',
    label: 'Incidents paiement',
    description: 'Échecs de prélèvement et échéances en retard.',
  },
];

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonToggleModule,
    MatIconModule,
    MatProgressSpinnerModule,
    NotificationItemComponent,
  ],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export class NotificationsComponent implements OnInit {
  private readonly notificationApi = inject(NotificationApiService);
  private readonly unreadService = inject(NotificationUnreadService);
  private readonly pageCoordination = inject(NotificationPageCoordinationService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly notifications = signal<NotificationDto[]>([]);
  readonly filter = signal<NotificationFilter>('all');

  readonly isAdvisor = computed(() => this.auth.hasRole([ROLES.CONSEILLER]));

  readonly unreadCount = computed(
    () => this.notifications().filter((notification) => !notification.read).length,
  );

  readonly filteredNotifications = computed(() => {
    const items = this.notifications();
    return this.filter() === 'unread'
      ? items.filter((notification) => !notification.read)
      : items;
  });

  readonly groupedNotifications = computed(() =>
    groupNotificationsByPeriod(this.filteredNotifications()),
  );

  readonly asideTipParagraphs = computed(() =>
    this.isAdvisor() ? ADVISOR_ASIDE_TIPS : CLIENT_ASIDE_TIPS,
  );

  readonly alertTypes = computed(() =>
    this.isAdvisor() ? ADVISOR_ALERT_TYPES : CLIENT_ALERT_TYPES,
  );

  readonly quickLinks = computed((): NotificationsQuickLink[] => {
    if (this.isAdvisor()) {
      return [
        { route: '/conseiller/dossiers', label: 'Mes dossiers', icon: 'folder_open' },
        { route: '/conseiller/prets', label: 'Prêts clients', icon: 'account_balance' },
        { route: '/conseiller/dashboard', label: 'Tableau de bord', icon: 'dashboard' },
      ];
    }
    return [
      { route: '/mes-demandes', label: 'Mes demandes', icon: 'folder_open' },
      { route: '/documents', label: 'Mes documents', icon: 'description' },
      { route: '/paiements', label: 'Paiements', icon: 'calendar_month' },
      { route: '/profil', label: 'Mon profil', icon: 'person' },
    ];
  });

  ngOnInit(): void {
    this.loadNotifications();
    this.pageCoordination.refreshList$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadNotifications());
  }

  setFilter(filter: NotificationFilter): void {
    this.filter.set(filter);
  }

  loadNotifications(): void {
    this.loading.set(true);
    this.notificationApi.list(0, 50).subscribe({
      next: (page) => {
        this.notifications.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err, 'Impossible de charger les notifications.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  openNotification(notification: NotificationDto): void {
    if (!notification.read) {
      this.notificationApi.markAsRead(notification.id).subscribe({
        next: () => {
          this.notifications.update((items) =>
            items.map((item) =>
              item.id === notification.id
                ? { ...item, read: true, readAt: new Date().toISOString() }
                : item,
            ),
          );
          this.unreadService.refresh();
        },
      });
    }

    if (notification.referenceType === 'LOAN_APPLICATION' && notification.referenceId != null) {
      const base = this.isAdvisor() ? '/conseiller/dossiers' : '/mes-demandes';
      void this.router.navigate([`${base}/${notification.referenceId}`]);
    }
  }
}
