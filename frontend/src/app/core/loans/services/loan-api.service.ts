import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LoanRequestDto } from '../models/loan-request.model';
import { LoanResponseDto } from '../models/loan-response.model';
import { LoanDocumentResponseDto } from '../models/loan-document.model';
import { LoanApplicationStatus } from '../models/loan.enums';
import { LoanDocumentType } from '../models/loan.enums';
import { LoanHistoryEventResponseDto } from '../models/loan-history.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class LoanApiService {
  private readonly baseUrl = `${environment.apiUrl}/v1/loan-applications`;

  constructor(private readonly http: HttpClient) {}

  create(body: LoanRequestDto): Observable<LoanResponseDto> {
    return this.http.post<LoanResponseDto>(this.baseUrl, body);
  }

  update(id: number, body: LoanRequestDto): Observable<LoanResponseDto> {
    return this.http.patch<LoanResponseDto>(`${this.baseUrl}/${id}`, body);
  }

  getById(id: number): Observable<LoanResponseDto> {
    return this.http.get<LoanResponseDto>(`${this.baseUrl}/${id}`);
  }

  list(params: {
    page?: number;
    size?: number;
    status?: LoanApplicationStatus;
  }): Observable<Page<LoanResponseDto>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 10));
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    return this.http.get<Page<LoanResponseDto>>(this.baseUrl, { params: httpParams });
  }

  submit(id: number): Observable<LoanResponseDto> {
    return this.http.post<LoanResponseDto>(`${this.baseUrl}/${id}/submit`, {});
  }

  cancel(id: number, comment?: string): Observable<LoanResponseDto> {
    const body = comment?.trim() ? { comment: comment.trim() } : {};
    return this.http.post<LoanResponseDto>(`${this.baseUrl}/${id}/cancel`, body);
  }

  deleteApplication(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadDocument(
    id: number,
    documentType: LoanDocumentType,
    file: File,
    displayName?: string
  ): Observable<LoanDocumentResponseDto> {
    const formData = this.buildDocumentFormData(documentType, file, displayName);
    return this.http.post<LoanDocumentResponseDto>(`${this.baseUrl}/${id}/documents`, formData);
  }

  uploadDocumentWithProgress(
    id: number,
    documentType: LoanDocumentType,
    file: File,
    displayName?: string
  ): Observable<HttpEvent<LoanDocumentResponseDto>> {
    const formData = this.buildDocumentFormData(documentType, file, displayName);
    return this.http.post<LoanDocumentResponseDto>(`${this.baseUrl}/${id}/documents`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  private buildDocumentFormData(
    documentType: LoanDocumentType,
    file: File,
    displayName?: string
  ): FormData {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    if (displayName?.trim()) {
      formData.append('displayName', displayName.trim());
    }
    return formData;
  }

  downloadDocument(id: number, documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/documents/${documentId}/download`, {
      responseType: 'blob',
    });
  }

  getDocuments(id: number): Observable<LoanDocumentResponseDto[]> {
    return this.http.get<LoanDocumentResponseDto[]>(`${this.baseUrl}/${id}/documents`);
  }

  getHistory(id: number): Observable<LoanHistoryEventResponseDto[]> {
    return this.http.get<LoanHistoryEventResponseDto[]>(`${this.baseUrl}/${id}/history`);
  }

  uploadComplementDocument(
    id: number,
    documentType: LoanDocumentType,
    file: File,
    displayName?: string
  ): Observable<LoanDocumentResponseDto> {
    const formData = this.buildDocumentFormData(documentType, file, displayName);
    return this.http.post<LoanDocumentResponseDto>(
      `${this.baseUrl}/${id}/documents/complement`,
      formData
    );
  }

  deleteDocument(id: number, documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/documents/${documentId}`);
  }
}
