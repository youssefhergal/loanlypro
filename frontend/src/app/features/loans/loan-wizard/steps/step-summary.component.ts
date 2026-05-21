import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { LoanWizardStateService } from '../../../../core/loans/services/loan-wizard-state.service';
import {
  EMPLOYMENT_OPTIONS,
  LOAN_PURPOSE_OPTIONS,
} from '../../../../core/loans/constants/loan.constants';
import { LoanSimulationCardComponent } from '../components/loan-simulation-card.component';
import { calculateDebtRatio, calculateMonthlyPayment } from '../../../../core/loans/utils/loan-calculator';
import { LOAN_DEBT_RATIO_MAX } from '../../../../core/loans/constants/loan.constants';

@Component({
  selector: 'app-step-summary',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCheckboxModule,
    MatExpansionModule,
    CurrencyPipe,
    LoanSimulationCardComponent,
  ],
  templateUrl: './step-summary.component.html',
  styleUrl: './step-summary.component.scss',
})
export class StepSummaryComponent {
  readonly state = inject(LoanWizardStateService);
  readonly form = this.state.form;
  readonly documents = this.state.documents;
  readonly reference = this.state.reference;

  readonly purposeLabel = computed(() => {
    const v = this.form.get('loanPurpose')?.value;
    return LOAN_PURPOSE_OPTIONS.find((p) => p.value === v)?.label ?? v;
  });

  readonly employmentLabel = computed(() => {
    const v = this.form.get('employmentStatus')?.value;
    return EMPLOYMENT_OPTIONS.find((e) => e.value === v)?.label ?? v;
  });

  readonly debtRatio = computed(() => {
    const v = this.form.getRawValue();
    const charges =
      Number(v.monthlyRent ?? 0) +
      Number(v.monthlyLoanPayments ?? 0) +
      Number(v.monthlyAlimony ?? 0) +
      Number(v.monthlyOtherCharges ?? 0);
    const income = Number(v.monthlyIncome ?? 0) + Number(v.additionalIncome ?? 0);
    const payment = calculateMonthlyPayment(
      Number(v.requestedAmount ?? 0),
      Number(v.requestedDurationMonths ?? 0)
    );
    return calculateDebtRatio(charges, payment, income);
  });
}
