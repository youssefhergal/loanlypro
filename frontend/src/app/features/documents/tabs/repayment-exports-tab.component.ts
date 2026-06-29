import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { DocumentsApiService } from '../../../core/documents/services/documents-api.service';
import { RepaymentApiService } from '../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';
import { LoanDetailDto } from '../../../core/loans/repayment/models/loan-detail.model';
import { InstallmentDto } from '../../../core/loans/repayment/models/installment.model';
import { PaymentTransactionDto } from '../../../core/loans/repayment/models/payment-transaction.model';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';
import { InstallmentTableComponent } from '../../loans/repayment/shared/installment-table/installment-table.component';
import { SelectedLoanPickerComponent } from '../../../shared/selected-loan-picker/selected-loan-picker.component';

const PREVIEW_INSTALLMENTS = 5;
const PREVIEW_TRANSACTIONS = 4;

@Component({
  selector: 'app-repayment-exports-tab',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    InstallmentTableComponent,
    SelectedLoanPickerComponent,
  ],
  templateUrl: './repayment-exports-tab.component.html',
  styleUrl: './repayment-exports-tab.component.scss',
})
export class RepaymentExportsTabComponent implements OnInit {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loadingLoans = signal(true);
  readonly loadingDetails = signal(false);
  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly selectedLoanId = signal<number | null>(null);
  readonly loanDetail = signal<LoanDetailDto | null>(null);
  readonly installments = signal<InstallmentDto[]>([]);
  readonly transactions = signal<PaymentTransactionDto[]>([]);
  readonly exporting = signal<'schedule' | 'payments' | null>(null);

  readonly selectedLoan = computed(() => {
    const id = this.selectedLoanId();
    return this.loans().find((loan) => loan.id === id) ?? null;
  });

  readonly previewInstallments = computed(() =>
    this.installments().slice(0, PREVIEW_INSTALLMENTS),
  );

  readonly previewTransactions = computed(() =>
    this.transactions().slice(0, PREVIEW_TRANSACTIONS),
  );

  readonly showMandateCard = computed(() => {
    const loan = this.selectedLoan();
    return loan?.mandateStatus === 'ACTIVE';
  });

  readonly paymentsLink = computed(() => {
    const loanId = this.selectedLoanId();
    return loanId != null ? ['/paiements'] : ['/paiements'];
  });

  readonly paymentsQueryParams = computed(() => {
    const loanId = this.selectedLoanId();
    return loanId != null ? { loanId } : {};
  });

  readonly mandateLink = computed(() => {
    const loanId = this.selectedLoanId();
    return loanId != null ? ['/mes-prets', loanId, 'mandat'] : ['/mes-prets'];
  });

  ngOnInit(): void {
    this.repaymentApi.getMyLoans().subscribe({
      next: (loans) => {
        this.loans.set(loans);
        if (loans.length > 0) {
          this.selectLoan(loans[0].id);
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

  selectLoan(loanId: number): void {
    this.selectedLoanId.set(loanId);
    this.loadingDetails.set(true);
    forkJoin({
      installments: this.repaymentApi.getInstallments(loanId),
      transactions: this.repaymentApi.getTransactions(loanId),
      loan: this.repaymentApi.getLoan(loanId),
    }).subscribe({
      next: ({ installments, transactions, loan }) => {
        this.installments.set(installments);
        this.transactions.set(transactions);
        this.loanDetail.set(loan);
        this.loadingDetails.set(false);
      },
      error: (err) => {
        this.loadingDetails.set(false);
        this.snackBar.open(getErrorMessage(err, 'Impossible de charger les données du prêt.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  onLoanChange(loanId: number): void {
    this.selectLoan(loanId);
  }

  exportSchedule(): void {
    const loan = this.selectedLoan();
    if (!loan) {
      return;
    }
    this.exporting.set('schedule');
    this.documentsApi.downloadSchedulePdf(loan.id).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, `echeancier-${loan.reference}.pdf`);
        this.exporting.set(null);
      },
      error: (err) => this.handleExportError(err),
    });
  }

  exportPayments(): void {
    const loan = this.selectedLoan();
    if (!loan) {
      return;
    }
    this.exporting.set('payments');
    this.documentsApi.downloadPaymentsPdf(loan.id).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, `releve-prelevements-${loan.reference}.pdf`);
        this.exporting.set(null);
      },
      error: (err) => this.handleExportError(err),
    });
  }

  transactionStatusLabel(status: string): string {
    if (status === 'SUCCESS') {
      return 'Réussi';
    }
    if (status === 'FAILED') {
      return 'Échec';
    }
    return status;
  }

  private handleExportError(err: unknown): void {
    this.exporting.set(null);
    const message =
      err instanceof HttpErrorResponse && err.status === 501
        ? "L'export PDF n'est pas disponible pour ce prêt."
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
