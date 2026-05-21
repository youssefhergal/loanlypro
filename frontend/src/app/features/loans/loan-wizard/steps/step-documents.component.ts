import { Component, inject } from '@angular/core';
import { LoanWizardStateService } from '../../../../core/loans/services/loan-wizard-state.service';
import { DOCUMENT_SLOTS } from '../../../../core/loans/constants/loan.constants';
import { DocumentUploadSlotComponent } from '../components/document-upload-slot.component';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanDocumentType } from '../../../../core/loans/models/loan.enums';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-step-documents',
  standalone: true,
  imports: [DocumentUploadSlotComponent],
  templateUrl: './step-documents.component.html',
  styles: `
    .intro { margin: 0 0 1rem; color: rgba(0,0,0,0.65); }
    .error-banner { color: #c62828; background: #ffebee; padding: 0.75rem; border-radius: 4px; margin-bottom: 1rem; }
  `,
})
export class StepDocumentsComponent {
  private readonly state = inject(LoanWizardStateService);
  private readonly loanApi = inject(LoanApiService);

  readonly slots = DOCUMENT_SLOTS;
  readonly documents = this.state.documents;
  readonly errorMessage = this.state.lastError;

  getFiles(type: LoanDocumentType) {
    return this.documents().filter((d) => d.documentType === type);
  }

  onUpload(type: LoanDocumentType, file: File): void {
    const id = this.state.applicationId();
    if (!id) {
      this.state.lastError.set('Enregistrez d\u2019abord les étapes précédentes.');
      return;
    }
    this.loanApi.uploadDocument(id, type, file).subscribe({
      next: () => this.state.refreshDocuments(id).subscribe(),
      error: (err) => this.state.lastError.set(getErrorMessage(err)),
    });
  }

  onRemove(documentId: number): void {
    const id = this.state.applicationId();
    if (!id) return;
    this.loanApi.deleteDocument(id, documentId).subscribe({
      next: () => this.state.refreshDocuments(id).subscribe(),
      error: (err) => this.state.lastError.set(getErrorMessage(err)),
    });
  }
}
