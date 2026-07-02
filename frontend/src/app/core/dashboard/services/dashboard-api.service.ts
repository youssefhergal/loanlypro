import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { JustificatifGroupDto } from '../../documents/models/justificatif.model';
import { NotificationDto } from '../../notifications/models/notification.model';
import { LoanSummaryDto } from '../../loans/repayment/models/loan-summary.model';

export interface LoanApplicationSummaryDto {
  id: number;
  reference: string;
  status: string;
  title: string;
  requestedAmount: number;
  requestedDurationMonths: number;
  createdAt?: string | null;
  submittedAt?: string | null;
  decidedAt?: string | null;
  applicantName?: string | null;
}

export interface LoanDocumentResponseDto {
  id: number;
  loanApplicationId: number;
  documentType: string;
  originalFileName: string;
  displayName?: string | null;
  contentType?: string | null;
  fileSizeBytes: number;
  uploadedAt: string;
}

export interface PaymentTransactionDto {
  id: number;
  installmentId: number;
  sequenceNumber: number;
  attemptNumber: number;
  amount: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | string;
  failureReason?: string | null;
  externalReference?: string | null;
  attemptedAt: string;
  settledAt?: string | null;
}

export interface DashboardResponse {
  demandes: LoanApplicationSummaryDto[];
  loans: LoanSummaryDto[];
  justificatifs: JustificatifGroupDto[];
  notifications: NotificationDto[];
  unreadNotificationsCount: number;
  documents: LoanDocumentResponseDto[];
  transactions: PaymentTransactionDto[];
}

export interface AdvisorDashboardResponse {
  totalCount: number;
  loans: LoanSummaryDto[];
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
}
