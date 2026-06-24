import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentsApiService } from '../../../core/documents/services/documents-api.service';
import { RepaymentApiService } from '../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-repayment-exports-tab',
  standalone: true,
  imports: [
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  templateUrl: './repayment-exports-tab.component.html',
  styleUrl: './repayment-exports-tab.component.scss',
})
export class RepaymentExportsTabComponent implements OnInit {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loadingLoans = signal(true);
  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly selectedLoanId = signal<number | null>(null);
  readonly exporting = signal<'schedule' | 'payments' | null>(null);

  ngOnInit(): void {
    this.repaymentApi.getMyLoans().subscribe({
      next: (loans) => {
        this.loans.set(loans);
        if (loans.length > 0) {
          this.selectedLoanId.set(loans[0].id);
        }
        this.loadingLoans.set(false);
      },
      error: (err) => {
        this.loadingLoans.set(false);
        this.snackBar.open(getErrorMessage(err, 'Impossible de charger vos crédits.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  exportSchedule(): void {
    const loanId = this.selectedLoanId();
    if (loanId == null) {
      return;
    }
    this.exporting.set('schedule');
    this.documentsApi.downloadSchedulePdf(loanId).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, `echeancier-${loanId}.pdf`);
        this.exporting.set(null);
      },
      error: (err) => this.handleExportError(err),
    });
  }

  exportPayments(): void {
    const loanId = this.selectedLoanId();
    if (loanId == null) {
      return;
    }
    this.exporting.set('payments');
    this.documentsApi.downloadPaymentsPdf(loanId).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, `prelevements-${loanId}.pdf`);
        this.exporting.set(null);
      },
      error: (err) => this.handleExportError(err),
    });
  }

  private handleExportError(err: unknown): void {
    this.exporting.set(null);
    const message =
      err instanceof HttpErrorResponse && err.status === 501
        ? 'Export PDF non encore implémenté (US-6.7 / US-6.8).'
        : getErrorMessage(err, 'Export indisponible.');
    this.snackBar.open(message, 'Fermer', { duration: 5000 });
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
