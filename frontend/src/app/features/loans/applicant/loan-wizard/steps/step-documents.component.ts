import { Component, OnDestroy, inject, signal } from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { Subscription } from 'rxjs';
import { LoanWizardStateService } from '../../../../../core/loans/services/loan-wizard-state.service';
import { DOCUMENT_SLOTS } from '../../../../../core/loans/constants/loan.constants';
import {
  DocumentSlotUploadState,
  DocumentUploadPayload,
  DocumentUploadSlotComponent,
} from '../components/document-upload-slot.component';
import { OTHER_DOCUMENT_MAX_COUNT } from '../../../../../core/loans/constants/loan.constants';
import { WizardEstimationPanelComponent } from '../components/wizard-estimation-panel.component';
import { WizardAdvicePanelComponent } from '../components/wizard-advice-panel.component';
import { LoanApiService } from '../../../../../core/loans/services/loan-api.service';
import { LoanDocumentType } from '../../../../../core/loans/models/loan.enums';
import { getErrorMessage } from '../../../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-step-documents',
  standalone: true,
  imports: [
    MatIconModule,
    DocumentUploadSlotComponent,
    WizardEstimationPanelComponent,
    WizardAdvicePanelComponent,
  ],
  templateUrl: './step-documents.component.html',
  styleUrl: './step-documents.component.scss',
})
export class StepDocumentsComponent implements OnDestroy {
  private readonly state = inject(LoanWizardStateService);
  private readonly loanApi = inject(LoanApiService);

  readonly slots = DOCUMENT_SLOTS;
  readonly documents = this.state.documents;
  readonly errorMessage = this.state.lastError;
  readonly uploadStateByType = signal<
    Partial<Record<LoanDocumentType, DocumentSlotUploadState>>
  >({});

  private readonly uploadSubs = new Map<LoanDocumentType, Subscription>();

  readonly adviceTitle = 'Bon à savoir';
  readonly adviceMessage =
    'Vos documents sont chiffrés et stockés de manière sécurisée. Ils ne seront utilisés que dans le cadre de votre demande de prêt.';

  get amount(): number {
    return Number(this.state.form.get('requestedAmount')?.value) || 0;
  }

  get durationMonths(): number {
    return Number(this.state.form.get('requestedDurationMonths')?.value) || 0;
  }

  get loanPurpose() {
    return this.state.form.get('loanPurpose')?.value ?? 'PERSONAL';
  }

  getFiles(type: LoanDocumentType) {
    return this.documents().filter((d) => d.documentType === type);
  }

  getUploadState(type: LoanDocumentType): DocumentSlotUploadState | null {
    return this.uploadStateByType()[type] ?? null;
  }

  maxFilesFor(type: LoanDocumentType): number {
    return type === 'OTHER' ? OTHER_DOCUMENT_MAX_COUNT : 1;
  }

  requireDisplayNameFor(type: LoanDocumentType): boolean {
    return type === 'OTHER';
  }

  onUpload(type: LoanDocumentType, payload: DocumentUploadPayload): void {
    const { file, displayName } = payload;
    const id = this.state.applicationId();
    if (!id) {
      this.state.lastError.set('Enregistrez d\u2019abord les étapes précédentes.');
      return;
    }

    this.state.lastError.set(null);
    this.cancelUpload(type);

    this.uploadStateByType.update((m) => ({
      ...m,
      [type]: {
        fileName: file.name,
        fileSize: file.size,
        mimeType: file.type,
        progress: 0,
        displayName: displayName?.trim(),
      },
    }));

    const sub = this.loanApi
      .uploadDocumentWithProgress(id, type, file, displayName)
      .subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const total = event.total ?? file.size;
          const progress = total > 0 ? Math.round((100 * event.loaded) / total) : 0;
          this.uploadStateByType.update((m) => {
            const current = m[type];
            if (!current) return m;
            return { ...m, [type]: { ...current, progress: Math.min(progress, 99) } };
          });
        }
        if (event.type === HttpEventType.Response && event.body) {
          this.uploadStateByType.update((m) => {
            const current = m[type];
            if (!current) return m;
            return { ...m, [type]: { ...current, progress: 100 } };
          });
          this.state.refreshDocuments(id).subscribe({
            next: () => this.clearUploadState(type),
          });
        }
      },
      error: (err) => {
        this.clearUploadState(type);
        this.state.lastError.set(getErrorMessage(err));
      },
    });

    sub.add(() => this.uploadSubs.delete(type));

    this.uploadSubs.set(type, sub);
  }

  onCancelUpload(type: LoanDocumentType): void {
    this.cancelUpload(type);
  }

  onRemove(documentId: number): void {
    const id = this.state.applicationId();
    if (!id) return;
    this.loanApi.deleteDocument(id, documentId).subscribe({
      next: () => this.state.refreshDocuments(id).subscribe(),
      error: (err) => this.state.lastError.set(getErrorMessage(err)),
    });
  }

  onDownload(documentId: number): void {
    const id = this.state.applicationId();
    if (!id) return;
    const doc = this.documents().find((d) => d.id === documentId);
    if (!doc) return;

    this.loanApi.downloadDocument(id, documentId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.originalFileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.state.lastError.set(getErrorMessage(err)),
    });
  }

  ngOnDestroy(): void {
    this.uploadSubs.forEach((s) => s.unsubscribe());
    this.uploadSubs.clear();
  }

  private cancelUpload(type: LoanDocumentType): void {
    const sub = this.uploadSubs.get(type);
    sub?.unsubscribe();
    this.uploadSubs.delete(type);
    this.clearUploadState(type);
  }

  private clearUploadState(type: LoanDocumentType): void {
    this.uploadStateByType.update((m) => {
      const next = { ...m };
      delete next[type];
      return next;
    });
  }
}
