import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RepaymentApiService } from '../../../../../core/loans/repayment/services/repayment-api.service';
import { LoanDetailDto } from '../../../../../core/loans/repayment/models/loan-detail.model';
import { LoanHistoryEventResponseDto } from '../../../../../core/loans/models/loan-history.model';
import { getErrorMessage } from '../../../../../core/loans/utils/api-error.util';
import { LoanStatusChipComponent } from '../../shared/loan-status-chip/loan-status-chip.component';
import { InstallmentTableComponent } from '../../shared/installment-table/installment-table.component';
import { PaymentTransactionListComponent } from '../../shared/payment-transaction-list/payment-transaction-list.component';
import { RepaymentHistoryTimelineComponent } from '../../shared/repayment-history-timeline/repayment-history-timeline.component';

@Component({
  selector: 'app-admin-loan-detail',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    LoanStatusChipComponent,
    InstallmentTableComponent,
    PaymentTransactionListComponent,
    RepaymentHistoryTimelineComponent,
  ],
  templateUrl: './admin-loan-detail.component.html',
  styleUrl: './admin-loan-detail.component.scss',
})
export class AdminLoanDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loan = signal<LoanDetailDto | null>(null);
  readonly history = signal<LoanHistoryEventResponseDto[]>([]);
  readonly historyLoading = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Prêt introuvable.');
      return;
    }
    this.load(id);
  }

  private load(loanId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.repaymentApi.getAdminLoan(loanId).subscribe({
      next: (detail) => {
        this.loan.set(detail);
        this.loading.set(false);
        this.loadHistory(loanId);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(getErrorMessage(err));
      },
    });
  }

  private loadHistory(loanId: number): void {
    this.historyLoading.set(true);
    this.repaymentApi.getAdminHistory(loanId).subscribe({
      next: (events) => {
        this.history.set(events);
        this.historyLoading.set(false);
      },
      error: (err) => {
        this.historyLoading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }
}
