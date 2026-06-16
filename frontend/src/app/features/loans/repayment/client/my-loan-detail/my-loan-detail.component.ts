import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RepaymentApiService } from '../../../../../core/loans/repayment/services/repayment-api.service';
import { LoanDetailDto } from '../../../../../core/loans/repayment/models/loan-detail.model';
import { getErrorMessage } from '../../../../../core/loans/utils/api-error.util';
import { ConfirmDialogService } from '../../../../../shared/confirm-dialog/confirm-dialog.service';
import { filter } from 'rxjs';
import { LoanStatusChipComponent } from '../../shared/loan-status-chip/loan-status-chip.component';
import { MandateStatusBannerComponent } from '../../shared/mandate-status-banner/mandate-status-banner.component';
import { RepaymentHistoryTimelineComponent } from '../../shared/repayment-history-timeline/repayment-history-timeline.component';
import { LoanHistoryEventResponseDto } from '../../../../../core/loans/models/loan-history.model';
import { needsMandateSetup } from '../../../../../core/loans/repayment/constants/repayment.constants';

@Component({
  selector: 'app-my-loan-detail',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    LoanStatusChipComponent,
    MandateStatusBannerComponent,
    RepaymentHistoryTimelineComponent,
  ],
  templateUrl: './my-loan-detail.component.html',
  styleUrl: './my-loan-detail.component.scss',
})
export class MyLoanDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loan = signal<LoanDetailDto | null>(null);
  readonly history = signal<LoanHistoryEventResponseDto[]>([]);
  readonly historyLoading = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly revokingMandate = signal(false);

  readonly needsMandate = needsMandateSetup;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.error.set('Identifiant de prêt invalide.');
      return;
    }
    this.load(id);
  }

  load(loanId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.repaymentApi.getLoan(loanId).subscribe({
      next: (loan) => {
        this.loan.set(loan);
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
    this.repaymentApi.getHistory(loanId).subscribe({
      next: (events) => {
        this.history.set(events);
        this.historyLoading.set(false);
      },
      error: () => {
        this.history.set([]);
        this.historyLoading.set(false);
      },
    });
  }

  revokeMandate(): void {
    const detail = this.loan();
    if (!detail || detail.mandateStatus !== 'ACTIVE' || this.revokingMandate()) {
      return;
    }

    this.confirmDialog
      .open({
        title: 'Révoquer le mandat SEPA ?',
        message:
          'Les prochains prélèvements seront suspendus tant qu’un nouveau mandat ne sera pas signé.',
        confirmLabel: 'Révoquer le mandat',
        confirmColor: 'warn',
        cancelLabel: 'Annuler',
      })
      .pipe(filter((ok) => ok))
      .subscribe(() => {
        this.revokingMandate.set(true);
        this.repaymentApi.revokeMandate(detail.id).subscribe({
          next: () => {
            this.revokingMandate.set(false);
            this.snackBar.open('Mandat révoqué.', 'OK', { duration: 4000 });
            this.load(detail.id);
          },
          error: (err) => {
            this.revokingMandate.set(false);
            this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
          },
        });
      });
  }
}
