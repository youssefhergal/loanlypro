import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LoanSummaryDto } from '../../loans/repayment/models/loan-summary.model';
import { AdminLoanListSummary } from '../../loans/models/admin-loan-list-summary.model';

// Minimal models used by the client dashboard component
export interface LoanApplicationSummaryDto {
  id: number;
  reference: string;
  status: string;
  title: string;
  requestedAmount: number;
  requestedDurationMonths: number;
  createdAt?: string;
  submittedAt?: string;
  decidedAt?: string;
}

export interface LoanDocumentResponseDto {
  id: number;
  documentType: string;
  originalFileName: string;
  displayName?: string;
  fileSizeBytes: number;
  uploadedAt: string;
}

export interface PaymentTransactionDto {
  id: number;
  amount: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | string;
  sequenceNumber: number;
  attemptNumber: number;
  attemptedAt?: string;
  settledAt?: string;
}

export interface DashboardResponse {
  demandes: LoanApplicationSummaryDto[];
  documents: LoanDocumentResponseDto[];
  transactions: PaymentTransactionDto[];
}

export interface AdvisorDashboardResponse {
  totalCount: number;
  loans: LoanSummaryDto[];
  // Champs optionnels renvoyés par le backend pour les dossiers assignés au conseiller
  applicationsCount?: number;
  applications?: LoanApplicationSummaryDto[];
}

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  constructor(private readonly http: HttpClient) {}

  getMyDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${this.baseUrl}/me`);
  }

  getAdvisorDashboard(): Observable<AdvisorDashboardResponse> {
    return this.http.get<AdvisorDashboardResponse>(`${this.baseUrl}/advisor`);
  }

  getAdminDashboard(params?: { search?: string }): Observable<AdminLoanListSummary> {
    let httpParams = new HttpParams();
    if (params?.search?.trim()) {
      httpParams = httpParams.set('search', params.search.trim());
    }
    return this.http.get<AdminLoanListSummary>(`${this.baseUrl}/admin`, { params: httpParams });
  }
}
