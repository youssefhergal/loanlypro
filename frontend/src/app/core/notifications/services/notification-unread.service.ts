import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from '../../auth/services/auth.service';
import { NotificationApiService } from './notification-api.service';

@Injectable({ providedIn: 'root' })
export class NotificationUnreadService {
  private readonly api = inject(NotificationApiService);
  private readonly auth = inject(AuthService);

  readonly count = signal(0);

  refresh(): void {
    if (!this.auth.isLoggedIn()) {
      this.count.set(0);
      return;
    }
    this.api.unreadCount().subscribe({
      next: (res) => this.count.set(res.count),
      error: () => this.count.set(0),
    });
  }
}
