import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface PaymentTransactionDto {
  id: number;
  installmentId: number;
  sequenceNumber: number;
  attemptNumber: number;
  amount: number;
  status: string;
  failureReason?: string | null;
  externalReference?: string | null;
  attemptedAt: string; // ISO
  settledAt?: string | null;
}

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
}

export interface LoanDocumentResponseDto {
  id: number;
  loanApplicationId: number;
  documentType: string;
  originalFileName: string;
  displayName?: string | null;
  contentType?: string | null;
  fileSizeBytes: number;
  uploadedAt: string; // ISO
}

export interface DashboardResponse {
  transactions: PaymentTransactionDto[];
  demandes: LoanApplicationSummaryDto[];
  documents: LoanDocumentResponseDto[];
}

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  constructor(private readonly http: HttpClient) {}

  getMyDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${this.baseUrl}/me`);
  }
}
