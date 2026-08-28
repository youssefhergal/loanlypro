import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import type { User } from '../../auth/models/user.model';

export interface UpdateEmailResponse {
  token: string;
  user: User;
}

export interface UpdateEmailRequest {
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly baseUrl = `${environment.apiUrl}/profile`;

  constructor(private readonly http: HttpClient) {}

  getMe(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/me`);
    }

  updateEmail(payload: UpdateEmailRequest): Observable<UpdateEmailResponse> {
    return this.http.put<UpdateEmailResponse>(`${this.baseUrl}/me`, payload);
  }

  changePassword(payload: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/change-password`, payload);
  }
}
