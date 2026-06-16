import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { RepaymentApiService } from '../../../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../../../core/loans/repayment/models/loan-summary.model';
import { LoanDetailDto } from '../../../../../core/loans/repayment/models/loan-detail.model';
import { InstallmentDto } from '../../../../../core/loans/repayment/models/installment.model';
import { PaymentTransactionDto } from '../../../../../core/loans/repayment/models/payment-transaction.model';
import { getErrorMessage } from '../../../../../core/loans/utils/api-error.util';
import { InstallmentTableComponent } from '../../shared/installment-table/installment-table.component';
import { PaymentTransactionListComponent } from '../../shared/payment-transaction-list/payment-transaction-list.component';
import { MandateStatusBannerComponent } from '../../shared/mandate-status-banner/mandate-status-banner.component';
import { LoanStatusChipComponent } from '../../shared/loan-status-chip/loan-status-chip.component';

@Component({
  selector: 'app-payments-schedule',
  standalone: true,
  imports: [
    CurrencyPipe,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTabsModule,
    MatIconModule,
    InstallmentTableComponent,
    PaymentTransactionListComponent,
    MandateStatusBannerComponent,
    LoanStatusChipComponent,
  ],
  templateUrl: './payments-schedule.component.html',
  styleUrl: './payments-schedule.component.scss',
})
export class PaymentsScheduleComponent implements OnInit {
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly selectedLoanId = signal<number | null>(null);
  readonly loanDetail = signal<LoanDetailDto | null>(null);
  readonly installments = signal<InstallmentDto[]>([]);
  readonly transactions = signal<PaymentTransactionDto[]>([]);
  readonly loading = signal(false);

  readonly selectedLoan = computed(() => {
    const id = this.selectedLoanId();
    return this.loans().find((loan) => loan.id === id) ?? null;
  });

  readonly hasOverdueOrBlocked = computed(() =>
    this.installments().some(
      (installment) => installment.status === 'OVERDUE' || installment.status === 'BLOCKED',
    ),
  );

  readonly repaymentProgress = computed(() => {
    const installments = this.installments();
    const paid = installments.filter((item) => item.status === 'PAID');
    const total = installments.length;
    const paidCount = paid.length;
    const percent = total > 0 ? Math.round((paidCount / total) * 100) : 0;
    const capitalRepaid = paid.reduce((sum, item) => sum + item.principalPart, 0);
    const interestPaid = paid.reduce((sum, item) => sum + item.interestPart, 0);
    const totalPaid = paid.reduce((sum, item) => sum + item.amountDue, 0);
    return { paidCount, total, percent, capitalRepaid, interestPaid, totalPaid };
  });

  readonly showActiveMandateBanner = computed(() => {
    const loan = this.selectedLoan();
    const detail = this.loanDetail();
    return loan?.mandateStatus === 'ACTIVE' && !!detail?.ibanMasked;
  });

  ngOnInit(): void {
    const queryLoanId = Number(this.route.snapshot.queryParamMap.get('loanId'));
    this.loading.set(true);
    this.repaymentApi.getMyLoans().subscribe({
      next: (loans) => {
        this.loans.set(loans);
        if (!loans.length) {
          this.loading.set(false);
          return;
        }
        const initialId =
          Number.isFinite(queryLoanId) && loans.some((l) => l.id === queryLoanId)
            ? queryLoanId
            : loans[0].id;
        this.selectLoan(initialId);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }

  selectLoan(loanId: number): void {
    this.selectedLoanId.set(loanId);
    this.loading.set(true);
    forkJoin({
      installments: this.repaymentApi.getInstallments(loanId),
      transactions: this.repaymentApi.getTransactions(loanId),
      loan: this.repaymentApi.getLoan(loanId),
    }).subscribe({
      next: ({ installments, transactions, loan }) => {
        this.installments.set(installments);
        this.transactions.set(transactions);
        this.loanDetail.set(loan);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }

  onLoanChange(loanId: number): void {
    this.selectLoan(loanId);
  }
}
