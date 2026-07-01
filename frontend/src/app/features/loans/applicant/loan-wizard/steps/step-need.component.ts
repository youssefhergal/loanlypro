import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { LoanWizardStateService } from '../../../../../core/loans/services/loan-wizard-state.service';
import { LOAN_PURPOSE_OPTIONS } from '../../../../../core/loans/constants/loan.constants';
import {
  LOAN_AMOUNT_MAX,
  LOAN_AMOUNT_MIN,
  LOAN_COMMENT_MAX_LENGTH,
  LOAN_DURATION_MAX,
  LOAN_DURATION_MIN,
} from '../../../../../core/loans/constants/loan.constants';
import { LabeledSliderFieldComponent } from '../components/labeled-slider-field.component';
import { WizardEstimationPanelComponent } from '../components/wizard-estimation-panel.component';
import { WizardAdvicePanelComponent } from '../components/wizard-advice-panel.component';

function formatAmountCompact(value: number): string {
  if (value >= 1000) {
    const k = value / 1000;
    return Number.isInteger(k) ? `${k}k` : `${k.toFixed(1).replace(/\.0$/, '')}k`;
  }
  return String(value);
}

@Component({
  selector: 'app-step-need',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    LabeledSliderFieldComponent,
    WizardEstimationPanelComponent,
    WizardAdvicePanelComponent,
  ],
  templateUrl: './step-need.component.html',
  styleUrl: './step-need.component.scss',
})
export class StepNeedComponent {
  readonly state = inject(LoanWizardStateService);
  readonly form = this.state.form;
  readonly purposeOptions = LOAN_PURPOSE_OPTIONS;
  readonly commentMaxLength = LOAN_COMMENT_MAX_LENGTH;

  readonly amountMin = LOAN_AMOUNT_MIN;
  readonly amountMax = LOAN_AMOUNT_MAX;
  readonly durationMin = LOAN_DURATION_MIN;
  readonly durationMax = LOAN_DURATION_MAX;

  /** Bulles slider : format compact (10k, 45k). */
  formatAmountSlider = (v: number): string => formatAmountCompact(v);

  /** Min / max : format complet (1 000 €, 200 000 €). */
  formatAmountRange = (v: number): string =>
    new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(v) + ' €';

  /** Bulles slider : format compact (12m, 84m). */
  formatDurationSlider = (v: number): string => `${v}m`;

  /** Min / max : format complet (12 mois, 240 mois). */
  formatDurationRange = (v: number): string => `${v} mois`;

  get amount(): number {
    return Number(this.form.get('requestedAmount')?.value) || 0;
  }

  get durationMonths(): number {
    return Number(this.form.get('requestedDurationMonths')?.value) || 0;
  }

  get loanPurpose() {
    return this.form.get('loanPurpose')?.value ?? 'PERSONAL';
  }

  get selectedPurposeHint(): string | undefined {
    return this.purposeOptions.find((opt) => opt.value === this.loanPurpose)?.hint;
  }
}
