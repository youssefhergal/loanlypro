import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Page } from '../../models/page.model';
import { LoanSummaryDto } from '../models/loan-summary.model';
import { LoanDetailDto } from '../models/loan-detail.model';
import { InstallmentDto } from '../models/installment.model';
import { PaymentTransactionDto } from '../models/payment-transaction.model';
import {
  ActivateMandateRequestDto,
  MandateResponseDto,
} from '../models/mandate.model';
import { LoanHistoryEventResponseDto } from '../../models/loan-history.model';
import { RepaymentKpiDto } from '../models/repayment-kpi.model';

export interface AdvisorLoansQuery {
  page?: number;
  size?: number;
  status?: string;
  overdueOnly?: boolean;
}

export interface AdminLoansQuery {
  page?: number;
  size?: number;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class RepaymentApiService {
  private readonly baseUrl = `${environment.apiUrl}/v1/loans`;
  private readonly advisorBaseUrl = `${environment.apiUrl}/v1/advisor/loans`;
  private readonly adminBaseUrl = `${environment.apiUrl}/v1/admin/loans`;

  constructor(private readonly http: HttpClient) {}

  getMyLoans(): Observable<LoanSummaryDto[]> {
    return this.http.get<LoanSummaryDto[]>(`${this.baseUrl}/me`);
  }

  getLoan(loanId: number): Observable<LoanDetailDto> {
    return this.http.get<LoanDetailDto>(`${this.baseUrl}/${loanId}`);
  }

  getInstallments(loanId: number): Observable<InstallmentDto[]> {
    return this.http.get<InstallmentDto[]>(`${this.baseUrl}/${loanId}/installments`);
  }

  getTransactions(loanId: number): Observable<PaymentTransactionDto[]> {
    return this.http.get<PaymentTransactionDto[]>(`${this.baseUrl}/${loanId}/transactions`);
  }

  activateMandate(
    loanId: number,
    body: ActivateMandateRequestDto,
  ): Observable<MandateResponseDto> {
    return this.http.post<MandateResponseDto>(
      `${this.baseUrl}/${loanId}/mandate/activate`,
      body,
    );
  }

  revokeMandate(loanId: number): Observable<MandateResponseDto> {
    return this.http.post<MandateResponseDto>(`${this.baseUrl}/${loanId}/mandate/revoke`, {});
  }

  getHistory(loanId: number): Observable<LoanHistoryEventResponseDto[]> {
    return this.http.get<LoanHistoryEventResponseDto[]>(`${this.baseUrl}/${loanId}/history`);
  }

  getAdvisorLoans(query: AdvisorLoansQuery = {}): Observable<Page<LoanSummaryDto>> {
    let params = new HttpParams();
    if (query.page != null) {
      params = params.set('page', String(query.page));
    }
    if (query.size != null) {
      params = params.set('size', String(query.size));
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.overdueOnly) {
      params = params.set('overdueOnly', 'true');
    }
    return this.http.get<Page<LoanSummaryDto>>(this.advisorBaseUrl, { params });
  }

  getAdvisorLoan(loanId: number): Observable<LoanDetailDto> {
    return this.http.get<LoanDetailDto>(`${this.advisorBaseUrl}/${loanId}`);
  }

  getAdvisorHistory(loanId: number): Observable<LoanHistoryEventResponseDto[]> {
    return this.http.get<LoanHistoryEventResponseDto[]>(
      `${this.advisorBaseUrl}/${loanId}/history`,
    );
  }

  getAdminKpi(): Observable<RepaymentKpiDto> {
    return this.http.get<RepaymentKpiDto>(`${this.adminBaseUrl}/kpi`);
  }

  getAdminLoans(query: AdminLoansQuery = {}): Observable<Page<LoanSummaryDto>> {
    let params = new HttpParams();
    if (query.page != null) {
      params = params.set('page', String(query.page));
    }
    if (query.size != null) {
      params = params.set('size', String(query.size));
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    return this.http.get<Page<LoanSummaryDto>>(this.adminBaseUrl, { params });
  }

  getAdminLoan(loanId: number): Observable<LoanDetailDto> {
    return this.http.get<LoanDetailDto>(`${this.adminBaseUrl}/${loanId}`);
  }

  getAdminHistory(loanId: number): Observable<LoanHistoryEventResponseDto[]> {
    return this.http.get<LoanHistoryEventResponseDto[]>(
      `${this.adminBaseUrl}/${loanId}/history`,
    );
  }
}
