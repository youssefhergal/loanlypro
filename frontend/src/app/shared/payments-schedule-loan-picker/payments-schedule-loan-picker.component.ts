import { Component, inject } from '@angular/core';
import { PaymentsSchedulePageStateService } from '../../core/loans/repayment/services/payments-schedule-page-state.service';
import { SelectedLoanPickerComponent } from '../selected-loan-picker/selected-loan-picker.component';

@Component({
  selector: 'app-payments-schedule-loan-picker',
  standalone: true,
  imports: [SelectedLoanPickerComponent],
  template: `
    @if (pageState.pickerVisible()) {
      <app-selected-loan-picker
        class="payments-schedule-loan-picker"
        [loans]="pageState.loans()"
        [selectedLoanId]="pageState.selectedLoanId()"
        (loanChange)="onLoanChange($event)"
      />
    }
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .payments-schedule-loan-picker {
        align-self: center;
      }
    `,
  ],
})
export class PaymentsScheduleLoanPickerComponent {
  readonly pageState = inject(PaymentsSchedulePageStateService);

  onLoanChange(loanId: number): void {
    this.pageState.selectLoan(loanId);
  }
}
