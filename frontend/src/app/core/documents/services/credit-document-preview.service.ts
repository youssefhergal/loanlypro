import { Injectable, computed, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocumentsApiService } from './documents-api.service';
import { CreditDocumentDto } from '../models/credit-document.model';
import {
  creditDocumentFileName,
  creditDocumentStatusClass,
  creditDocumentStatusLabel,
  formatIssuedAt,
} from '../utils/credit-document-display.util';
import { getErrorMessage } from '../../loans/utils/api-error.util';
import { LoanDetailDocumentRow } from '../../loans/utils/loan-detail.util';

@Injectable()
export class CreditDocumentPreviewService {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly open = signal(false);
  readonly doc = signal<CreditDocumentDto | null>(null);
  readonly url = signal<string | null>(null);
  readonly loading = signal(false);
  readonly zoom = signal(100);
  readonly previewableDocuments = signal<CreditDocumentDto[]>([]);

  private objectUrl: string | null = null;

  readonly row = computed((): LoanDetailDocumentRow | null => {
    const current = this.doc();
    if (!current) {
      return null;
    }
    return {
      type: 'OTHER',
      label: current.title,
      required: false,
      files: [],
      status: 'provided',
    };
  });

  readonly fileMeta = computed(() => {
    const current = this.doc();
    if (!current) {
      return '';
    }
    return `${current.reference} · Émis le ${formatIssuedAt(current.issuedAt)}`;
  });

  readonly index = computed(() => {
    const current = this.doc();
    if (!current) {
      return -1;
    }
    return this.previewableDocuments().findIndex(
      (item) =>
        item.documentType === current.documentType && item.reference === current.reference,
    );
  });

  readonly statusLabel = computed(() =>
    creditDocumentStatusLabel(this.doc()?.available ?? false),
  );

  readonly statusClass = computed(() =>
    creditDocumentStatusClass(this.doc()?.available ?? false),
  );

  setPreviewableDocuments(documents: CreditDocumentDto[]): void {
    this.previewableDocuments.set(documents.filter((doc) => doc.available));
  }

  openPreview(doc: CreditDocumentDto): void {
    const referenceId = doc.loanId ?? doc.loanApplicationId;
    if (!doc.available || referenceId == null) {
      return;
    }

    this.doc.set(doc);
    this.open.set(true);
    this.zoom.set(100);
    this.loadBlob(doc, referenceId);
  }

  close(): void {
    this.open.set(false);
    this.revokeUrl();
    this.doc.set(null);
  }

  zoomIn(): void {
    this.zoom.update((value) => Math.min(value + 10, 200));
  }

  zoomOut(): void {
    this.zoom.update((value) => Math.max(value - 10, 50));
  }

  prev(): void {
    const idx = this.index();
    const rows = this.previewableDocuments();
    if (idx > 0) {
      this.openPreview(rows[idx - 1]);
    }
  }

  next(): void {
    const idx = this.index();
    const rows = this.previewableDocuments();
    if (idx >= 0 && idx < rows.length - 1) {
      this.openPreview(rows[idx + 1]);
    }
  }

  downloadCurrent(): void {
    const current = this.doc();
    const previewUrl = this.url();
    if (!current || !previewUrl) {
      return;
    }
    this.triggerDownload(previewUrl, creditDocumentFileName(current));
  }

  destroy(): void {
    this.close();
  }

  private loadBlob(doc: CreditDocumentDto, referenceId: number): void {
    this.revokeUrl();
    this.loading.set(true);
    this.documentsApi.downloadCreditDocument(doc.documentType, referenceId).subscribe({
      next: (blob) => {
        this.objectUrl = URL.createObjectURL(blob);
        this.url.set(this.objectUrl);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err, 'Impossible de charger le document.'), 'Fermer', {
          duration: 5000,
        });
        this.close();
      },
    });
  }

  private revokeUrl(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
    this.url.set(null);
  }

  private triggerDownload(url: string, fileName: string): void {
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
  }
}
