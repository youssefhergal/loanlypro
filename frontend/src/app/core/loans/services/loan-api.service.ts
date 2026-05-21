import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LoanRequestDto } from '../models/loan-request.model';
import { LoanResponseDto } from '../models/loan-response.model';
import { LoanDocumentResponseDto } from '../models/loan-document.model';
import { LoanApplicationStatus } from '../models/loan.enums';
import { LoanDocumentType } from '../models/loan.enums';
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

  uploadDocument(
    id: number,
    documentType: LoanDocumentType,
    file: File
  ): Observable<LoanDocumentResponseDto> {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    return this.http.post<LoanDocumentResponseDto>(`${this.baseUrl}/${id}/documents`, formData);
  }

  getDocuments(id: number): Observable<LoanDocumentResponseDto[]> {
    return this.http.get<LoanDocumentResponseDto[]>(`${this.baseUrl}/${id}/documents`);
  }

  deleteDocument(id: number, documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/documents/${documentId}`);
  }
}
