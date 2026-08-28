import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificationPageCoordinationService {
  private readonly refreshListSubject = new Subject<void>();

  readonly refreshList$ = this.refreshListSubject.asObservable();

  requestListRefresh(): void {
    this.refreshListSubject.next();
  }
}
