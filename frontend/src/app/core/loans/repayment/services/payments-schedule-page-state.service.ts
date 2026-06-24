import { Injectable, computed, signal } from '@angular/core';
import { LoanSummaryDto } from '../models/loan-summary.model';

@Injectable({ providedIn: 'root' })
export class PaymentsSchedulePageStateService {
  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly selectedLoanId = signal<number | null>(null);
  readonly pickerVisible = signal(false);

  readonly selectedLoan = computed(() => {
    const id = this.selectedLoanId();
    return this.loans().find((loan) => loan.id === id) ?? null;
  });

  private changeHandler: ((loanId: number) => void) | null = null;

  bind(onChange: (loanId: number) => void): void {
    this.changeHandler = onChange;
  }

  unbind(): void {
    this.changeHandler = null;
    this.loans.set([]);
    this.selectedLoanId.set(null);
    this.pickerVisible.set(false);
  }

  setLoans(loans: LoanSummaryDto[]): void {
    this.loans.set(loans);
    this.pickerVisible.set(loans.length > 0);
  }

  setSelectedLoanId(loanId: number | null): void {
    this.selectedLoanId.set(loanId);
  }

  selectLoan(loanId: number): void {
    this.selectedLoanId.set(loanId);
    this.changeHandler?.(loanId);
  }
}
