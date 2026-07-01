import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { AiMessage } from '../models/ai-chat.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AiAdvisorService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/ai`;

  chat(messages: AiMessage[]): Observable<{ content: string }> {
    return this.http.post<{ content: string }>(`${this.base}/chat`, { messages });
  }
}
