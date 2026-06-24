import { Component, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationApiService } from '../../core/notifications/services/notification-api.service';
import { NotificationUnreadService } from '../../core/notifications/services/notification-unread.service';
import { NotificationDto } from '../../core/notifications/models/notification.model';
import { NotificationItemComponent } from '../notification-item/notification-item.component';

@Component({
  selector: 'app-notification-bell-menu',
  standalone: true,
  imports: [
    RouterLink,
    MatBadgeModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    NotificationItemComponent,
  ],
  templateUrl: './notification-bell-menu.component.html',
  styleUrl: './notification-bell-menu.component.scss',
})
export class NotificationBellMenuComponent {
  private readonly notificationApi = inject(NotificationApiService);
  private readonly router = inject(Router);
  readonly unreadService = inject(NotificationUnreadService);

  /** Lien vers la page centre de notifications */
  readonly centerLink = input.required<string>();
  /** Base de navigation dossier, ex. /mes-demandes ou /conseiller/dossiers */
  readonly dossierLinkBase = input.required<string>();
  readonly previewSize = input(5);

  readonly loading = signal(false);
  readonly preview = signal<NotificationDto[]>([]);
  readonly loadError = signal(false);

  loadPreview(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.notificationApi.list(0, this.previewSize()).subscribe({
      next: (page) => {
        this.preview.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.preview.set([]);
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  openNotification(notification: NotificationDto): void {
    if (!notification.read) {
      this.notificationApi.markAsRead(notification.id).subscribe({
        next: () => {
          this.preview.update((items) =>
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
      void this.router.navigate([`${this.dossierLinkBase()}/${notification.referenceId}`]);
    }
  }
}
