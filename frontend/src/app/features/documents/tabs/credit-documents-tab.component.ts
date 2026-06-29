import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { DocumentsApiService } from '../../../core/documents/services/documents-api.service';
import { CreditDocumentPreviewService } from '../../../core/documents/services/credit-document-preview.service';
import { CreditDocumentDto } from '../../../core/documents/models/credit-document.model';
import {
  creditDocumentFileName,
  creditDocumentIcon,
  formatIssuedAt,
} from '../../../core/documents/utils/credit-document-display.util';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';
import { RepaymentApiService } from '../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';
import {
  SelectedLoanPickerComponent,
  SelectedLoanPickerItem,
} from '../../../shared/selected-loan-picker/selected-loan-picker.component';

@Component({
  selector: 'app-credit-documents-tab',
  standalone: true,
  imports: [
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatCardModule,
    SelectedLoanPickerComponent,
  ],
  templateUrl: './credit-documents-tab.component.html',
  styleUrl: './credit-documents-tab.component.scss',
})
export class CreditDocumentsTabComponent implements OnInit {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly preview = inject(CreditDocumentPreviewService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly notImplemented = signal(false);
  readonly documents = signal<CreditDocumentDto[]>([]);
  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly selectedLoanId = signal<number | null>(null);

  readonly helpers = {
    creditDocumentIcon,
    formatIssuedAt,
  };

  readonly pickerLoans = computed((): SelectedLoanPickerItem[] => {
    const docs = this.documents();
    const loans = this.loans();
    const references = [...new Set(docs.map((doc) => doc.reference))].sort((a, b) =>
      b.localeCompare(a),
    );

    return references
      .map((reference) => {
        const loan = loans.find((item) => item.reference === reference);
        if (loan) {
          return { id: loan.id, reference: loan.reference, status: loan.status };
        }
        const doc = docs.find((item) => item.reference === reference);
        if (doc?.loanApplicationId == null) {
          return null;
        }
        return {
          id: doc.loanApplicationId,
          reference,
          status: 'PENDING_MANDATE',
        };
      })
      .filter((item): item is SelectedLoanPickerItem => item != null);
  });

  readonly selectedPickerItem = computed(() => {
    const id = this.selectedLoanId();
    return this.pickerLoans().find((loan) => loan.id === id) ?? null;
  });

  readonly filteredDocuments = computed(() => {
    const selected = this.selectedPickerItem();
    if (!selected) {
      return [];
    }
    return this.documents().filter((doc) => doc.reference === selected.reference);
  });

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading.set(true);
    this.notImplemented.set(false);
    forkJoin({
      documents: this.documentsApi.getCreditDocuments(),
      loans: this.repaymentApi.getMyLoans(),
    }).subscribe({
      next: ({ documents, loans }) => {
        this.documents.set(documents);
        this.loans.set(loans);
        this.initSelectedLoan(documents, loans);
        this.syncPreviewContext();
        this.loading.set(false);
      },
      error: (err) => this.handleLoadError(err, 'Impossible de charger vos documents crédit.'),
    });
  }

  onLoanChange(loanId: number): void {
    this.selectedLoanId.set(loanId);
    this.syncPreviewContext();
    if (this.preview.open()) {
      this.preview.close();
    }
  }

  openPreview(doc: CreditDocumentDto): void {
    this.syncPreviewContext();
    this.preview.openPreview(doc);
  }

  download(doc: CreditDocumentDto): void {
    const referenceId = doc.loanId ?? doc.loanApplicationId;
    if (!doc.available || referenceId == null) {
      return;
    }
    this.documentsApi.downloadCreditDocument(doc.documentType, referenceId).subscribe({
      next: (blob) => this.triggerDownload(blob, creditDocumentFileName(doc)),
      error: (err) => {
        this.snackBar.open(getErrorMessage(err, 'Téléchargement indisponible.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  trackDoc(index: number, doc: CreditDocumentDto): string {
    return `${doc.reference}:${doc.documentType}`;
  }

  private initSelectedLoan(documents: CreditDocumentDto[], loans: LoanSummaryDto[]): void {
    if (documents.length === 0) {
      this.selectedLoanId.set(null);
      return;
    }

    const references = [...new Set(documents.map((doc) => doc.reference))].sort((a, b) =>
      b.localeCompare(a),
    );
    const preferredReference = references[0];
    const matchingLoan = loans.find((loan) => loan.reference === preferredReference);
    if (matchingLoan) {
      this.selectedLoanId.set(matchingLoan.id);
      return;
    }

    const doc = documents.find((item) => item.reference === preferredReference);
    this.selectedLoanId.set(doc?.loanApplicationId ?? null);
  }

  private syncPreviewContext(): void {
    this.preview.setPreviewableDocuments(this.filteredDocuments());
  }

  private handleLoadError(err: unknown, fallback: string): void {
    if (err instanceof HttpErrorResponse && err.status === 501) {
      this.notImplemented.set(true);
      this.documents.set([]);
      this.loans.set([]);
      this.selectedLoanId.set(null);
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
