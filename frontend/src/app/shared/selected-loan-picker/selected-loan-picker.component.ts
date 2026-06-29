import { Component, input, output } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { LoanStatusChipComponent } from '../../features/loans/repayment/shared/loan-status-chip/loan-status-chip.component';

export interface SelectedLoanPickerItem {
  id: number;
  reference: string;
  status: string;
}

@Component({
  selector: 'app-selected-loan-picker',
  standalone: true,
  imports: [MatFormFieldModule, MatSelectModule, LoanStatusChipComponent],
  templateUrl: './selected-loan-picker.component.html',
  styleUrl: './selected-loan-picker.component.scss',
})
export class SelectedLoanPickerComponent {
  readonly loans = input.required<SelectedLoanPickerItem[]>();
  readonly selectedLoanId = input<number | null>(null);
  readonly label = input('Prêt sélectionné');

  readonly loanChange = output<number>();

  selectedLoan(): SelectedLoanPickerItem | null {
    const id = this.selectedLoanId();
    return this.loans().find((loan) => loan.id === id) ?? null;
  }

  onLoanChange(loanId: number): void {
    this.loanChange.emit(loanId);
  }
}
