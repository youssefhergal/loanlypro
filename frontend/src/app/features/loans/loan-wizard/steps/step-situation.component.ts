import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoanWizardStateService } from '../../../../core/loans/services/loan-wizard-state.service';
import { EMPLOYMENT_OPTIONS, LOAN_DEBT_RATIO_MAX } from '../../../../core/loans/constants/loan.constants';
import { LoanSimulationCardComponent } from '../components/loan-simulation-card.component';
import {
  calculateDebtRatio,
  calculateMonthlyPayment,
} from '../../../../core/loans/utils/loan-calculator';

@Component({
  selector: 'app-step-situation',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    LoanSimulationCardComponent,
    DecimalPipe,
  ],
  templateUrl: './step-situation.component.html',
  styleUrl: './step-situation.component.scss',
})
export class StepSituationComponent {
  readonly Math = Math;
  readonly state = inject(LoanWizardStateService);
  readonly form = this.state.form;
  readonly employmentOptions = EMPLOYMENT_OPTIONS;
  readonly debtRatioMax = LOAN_DEBT_RATIO_MAX;

  readonly showEmployerFields = computed(() => {
    const status = this.form.get('employmentStatus')?.value;
    return EMPLOYMENT_OPTIONS.find((o) => o.value === status)?.requiresEmployer ?? false;
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
