import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../loans/models/page.model';
import { NotificationDto, UnreadNotificationCountDto } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly baseUrl = `${environment.apiUrl}/v1/notifications`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, size = 20): Observable<Page<NotificationDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<NotificationDto>>(`${this.baseUrl}/me`, { params });
  }

  unreadCount(): Observable<UnreadNotificationCountDto> {
    return this.http.get<UnreadNotificationCountDto>(`${this.baseUrl}/unread-count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/read`, null);
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/read-all`, null);
  }
}
