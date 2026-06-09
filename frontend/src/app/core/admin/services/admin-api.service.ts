import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AdvisorAssignmentResult } from '../models/advisor-assignment.model';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly baseUrl = `${environment.apiUrl}/v1/admin`;

  constructor(private readonly http: HttpClient) {}

  runAdvisorAssignment(): Observable<AdvisorAssignmentResult> {
    return this.http.post<AdvisorAssignmentResult>(`${this.baseUrl}/advisor-assignment/run`, {});
  }
}
