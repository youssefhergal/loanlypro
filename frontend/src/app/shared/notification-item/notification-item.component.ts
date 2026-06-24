import { Component, computed, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { NotificationDto } from '../../core/notifications/models/notification.model';
import {
  formatRelativeNotificationDate,
  getNotificationVisual,
  isRecentNotification,
} from '../../core/notifications/utils/notification-display.util';

@Component({
  selector: 'app-notification-item',
  standalone: true,
  imports: [MatCardModule, MatChipsModule, MatIconModule, MatRippleModule],
  templateUrl: './notification-item.component.html',
  styleUrl: './notification-item.component.scss',
})
export class NotificationItemComponent {
  readonly notification = input.required<NotificationDto>();
  readonly compact = input(false);

  readonly clicked = output<NotificationDto>();

  readonly visual = computed(() => getNotificationVisual(this.notification().eventType));
  readonly relativeDate = computed(() =>
    formatRelativeNotificationDate(this.notification().createdAt),
  );
  readonly isRecent = computed(() => isRecentNotification(this.notification().createdAt));

  onClick(): void {
    this.clicked.emit(this.notification());
  }
}
