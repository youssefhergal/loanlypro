import { Component, inject } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { PaymentsSchedulePageStateService } from '../../core/loans/repayment/services/payments-schedule-page-state.service';
import { LoanStatusChipComponent } from '../../features/loans/repayment/shared/loan-status-chip/loan-status-chip.component';

@Component({
  selector: 'app-payments-schedule-loan-picker',
  standalone: true,
  imports: [MatFormFieldModule, MatSelectModule, LoanStatusChipComponent],
  templateUrl: './payments-schedule-loan-picker.component.html',
  styleUrl: './payments-schedule-loan-picker.component.scss',
})
export class PaymentsScheduleLoanPickerComponent {
  readonly pageState = inject(PaymentsSchedulePageStateService);

  onLoanChange(loanId: number): void {
    this.pageState.selectLoan(loanId);
  }
}
