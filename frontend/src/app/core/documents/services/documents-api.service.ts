import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { JustificatifGroupDto } from '../models/justificatif.model';
import { CreditDocumentDto } from '../models/credit-document.model';
import { IssuedDocumentType } from '../models/document.enums';

@Injectable({ providedIn: 'root' })
export class DocumentsApiService {
  private readonly baseUrl = `${environment.apiUrl}/v1/documents`;

  constructor(private readonly http: HttpClient) {}

  getJustificatifs(): Observable<JustificatifGroupDto[]> {
    return this.http.get<JustificatifGroupDto[]>(`${this.baseUrl}/me/justificatifs`);
  }

  downloadJustificatif(documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/justificatifs/${documentId}/download`, {
      responseType: 'blob',
    });
  }

  getCreditDocuments(): Observable<CreditDocumentDto[]> {
    return this.http.get<CreditDocumentDto[]>(`${this.baseUrl}/me/credit`);
  }

  downloadCreditDocument(type: IssuedDocumentType, referenceId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/credit/${type}/${referenceId}/download`, {
      responseType: 'blob',
    });
  }

  downloadSchedulePdf(loanId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/loans/${loanId}/schedule.pdf`, {
      responseType: 'blob',
    });
  }

  downloadPaymentsPdf(loanId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/loans/${loanId}/payments.pdf`, {
      responseType: 'blob',
    });
  }
}
