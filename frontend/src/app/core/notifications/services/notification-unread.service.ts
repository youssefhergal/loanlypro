import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from '../../auth/services/auth.service';
import { NotificationApiService } from './notification-api.service';

const DEFAULT_POLL_INTERVAL_MS = 30_000;

@Injectable({ providedIn: 'root' })
export class NotificationUnreadService {
  private readonly api = inject(NotificationApiService);
  private readonly auth = inject(AuthService);

  private pollIntervalId: ReturnType<typeof setInterval> | null = null;
  private pollingConsumers = 0;

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

  startPolling(intervalMs = DEFAULT_POLL_INTERVAL_MS): void {
    this.pollingConsumers += 1;
    if (this.pollIntervalId !== null) {
      return;
    }
    this.pollIntervalId = setInterval(() => this.refresh(), intervalMs);
  }

  stopPolling(): void {
    if (this.pollingConsumers === 0) {
      return;
    }
    this.pollingConsumers -= 1;
    if (this.pollingConsumers > 0 || this.pollIntervalId === null) {
      return;
    }
    clearInterval(this.pollIntervalId);
    this.pollIntervalId = null;
  }
}
