import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
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

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
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
