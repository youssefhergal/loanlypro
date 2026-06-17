import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RepaymentApiService } from '../../../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../../../core/loans/repayment/models/loan-summary.model';
import {
  needsMandateSetup,
  repaymentLoanStatusStyle,
} from '../../../../../core/loans/repayment/constants/repayment.constants';
import { getErrorMessage } from '../../../../../core/loans/utils/api-error.util';
import { LoanStatusChipComponent } from '../../shared/loan-status-chip/loan-status-chip.component';
import { MandateStatusBannerComponent } from '../../shared/mandate-status-banner/mandate-status-banner.component';
import { InstallmentStatusChipComponent } from '../../shared/installment-status-chip/installment-status-chip.component';

@Component({
  selector: 'app-my-loans',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    LoanStatusChipComponent,
    MandateStatusBannerComponent,
    InstallmentStatusChipComponent,
  ],
  templateUrl: './my-loans.component.html',
  styleUrl: './my-loans.component.scss',
})
export class MyLoansComponent implements OnInit {
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly loading = signal(false);

  readonly needsMandate = needsMandateSetup;
  readonly statusStyle = repaymentLoanStatusStyle;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.repaymentApi.getMyLoans().subscribe({
      next: (loans) => {
        this.loans.set(loans);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }
}
