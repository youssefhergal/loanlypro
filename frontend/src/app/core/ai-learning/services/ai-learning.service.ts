import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import type { LearningSession } from '../models/learning.model';

@Injectable({ providedIn: 'root' })
export class AiLearningService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/ai/learning`;

  generateRoadmap(objective: string): Observable<LearningSession> {
    return this.http.post<LearningSession>(`${this.base}/roadmap`, { objective });
  }

  getSessions(): Observable<LearningSession[]> {
    return this.http.get<LearningSession[]>(`${this.base}/sessions`);
  }

  getSession(sessionId: number): Observable<LearningSession> {
    return this.http.get<LearningSession>(`${this.base}/sessions/${sessionId}`);
  }

  getStepContent(sessionId: number, stepIndex: number): Observable<{ content: string }> {
    return this.http.post<{ content: string }>(
      `${this.base}/sessions/${sessionId}/steps/${stepIndex}/content`,
      {}
    );
  }

  completeStep(sessionId: number, stepIndex: number): Observable<LearningSession> {
    return this.http.patch<LearningSession>(
      `${this.base}/sessions/${sessionId}/steps/${stepIndex}/complete`,
      {}
    );
  }
}
