import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentsApiService } from '../../../core/documents/services/documents-api.service';
import { CreditDocumentDto } from '../../../core/documents/models/credit-document.model';
import { IssuedDocumentType } from '../../../core/documents/models/document.enums';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-credit-documents-tab',
  standalone: true,
  imports: [MatIconModule, MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './credit-documents-tab.component.html',
  styleUrl: './credit-documents-tab.component.scss',
})
export class CreditDocumentsTabComponent implements OnInit {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly notImplemented = signal(false);
  readonly documents = signal<CreditDocumentDto[]>([]);

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading.set(true);
    this.notImplemented.set(false);
    this.documentsApi.getCreditDocuments().subscribe({
      next: (items) => {
        this.documents.set(items);
        this.loading.set(false);
      },
      error: (err) => this.handleLoadError(err, 'Impossible de charger vos documents crédit.'),
    });
  }

  download(doc: CreditDocumentDto): void {
    const referenceId = doc.loanId ?? doc.loanApplicationId;
    if (!doc.available || referenceId == null) {
      return;
    }
    this.documentsApi.downloadCreditDocument(doc.documentType, referenceId).subscribe({
      next: (blob) => this.triggerDownload(blob, `${doc.documentType.toLowerCase()}.pdf`),
      error: (err) => {
        this.snackBar.open(getErrorMessage(err, 'Téléchargement indisponible.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  trackByType(index: number, doc: CreditDocumentDto): IssuedDocumentType {
    return doc.documentType;
  }

  private handleLoadError(err: unknown, fallback: string): void {
    if (err instanceof HttpErrorResponse && err.status === 501) {
      this.notImplemented.set(true);
      this.documents.set([]);
    } else {
      this.snackBar.open(getErrorMessage(err, fallback), 'Fermer', { duration: 5000 });
    }
    this.loading.set(false);
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
