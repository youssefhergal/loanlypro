import { CurrencyPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Subscription } from 'rxjs';
import { LoanWizardStateService } from '../../../../../core/loans/services/loan-wizard-state.service';
import {
  EMPLOYER_SECTOR_OPTIONS,
  EMPLOYMENT_OPTIONS,
  LOAN_DEBT_RATIO_DISPLAY_MAX,
  SENIORITY_RANGE_OPTIONS,
  seniorityMonthsFromRange,
} from '../../../../../core/loans/constants/loan.constants';
import { WizardEstimationPanelComponent } from '../components/wizard-estimation-panel.component';
import { WizardDebtRatioPanelComponent } from '../components/wizard-debt-ratio-panel.component';
import { WizardAdvicePanelComponent } from '../components/wizard-advice-panel.component';
import {
  calculateDebtRatio,
  calculateMonthlyPayment,
} from '../../../../../core/loans/utils/loan-calculator';

@Component({
  selector: 'app-step-situation',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonToggleModule,
    MatIconModule,
    WizardEstimationPanelComponent,
    WizardDebtRatioPanelComponent,
    WizardAdvicePanelComponent,
    CurrencyPipe,
  ],
  templateUrl: './step-situation.component.html',
  styleUrl: './step-situation.component.scss',
})
export class StepSituationComponent implements OnInit, OnDestroy {
  readonly state = inject(LoanWizardStateService);
  readonly form = this.state.form;
  readonly employmentOptions = EMPLOYMENT_OPTIONS;
  readonly sectorOptions = EMPLOYER_SECTOR_OPTIONS;
  readonly seniorityOptions = SENIORITY_RANGE_OPTIONS;
  readonly debtRatioDisplayMax = LOAN_DEBT_RATIO_DISPLAY_MAX;

  private sub = new Subscription();

  get showEmployerFields(): boolean {
    const status = this.form.get('employmentStatus')?.value;
    return EMPLOYMENT_OPTIONS.find((o) => o.value === status)?.requiresEmployer ?? false;
  }

  get totalIncome(): number {
    const v = this.form.getRawValue();
    return Number(v.monthlyIncome ?? 0) + Number(v.additionalIncome ?? 0);
  }

  get totalCharges(): number {
    const v = this.form.getRawValue();
    return (
      Number(v.monthlyRent ?? 0) +
      Number(v.monthlyLoanPayments ?? 0) +
      Number(v.monthlyAlimony ?? 0) +
      Number(v.monthlyOtherCharges ?? 0)
    );
  }

  get estimatedPayment(): number {
    const v = this.form.getRawValue();
    return calculateMonthlyPayment(
      Number(v.requestedAmount ?? 0),
      Number(v.requestedDurationMonths ?? 0)
    );
  }

  get debtRatio(): number {
    return calculateDebtRatio(this.totalCharges, this.estimatedPayment, this.totalIncome);
  }

  get amount(): number {
    return Number(this.form.get('requestedAmount')?.value) || 0;
  }

  get durationMonths(): number {
    return Number(this.form.get('requestedDurationMonths')?.value) || 0;
  }

  get adviceTitle(): string {
    return 'Bon à savoir';
  }

  get adviceMessage(): string {
    const ratio = this.debtRatio;
    if (ratio >= 0.5) {
      return 'Votre taux d\'endettement est élevé. Réduisez vos charges ou augmentez votre apport pour améliorer votre dossier.';
    }
    if (ratio > LOAN_DEBT_RATIO_DISPLAY_MAX) {
      return 'Vous dépassez le seuil indicatif de 35 %. Les banques peuvent demander des garanties supplémentaires ou un montant réduit.';
    }
    return 'Les banques acceptent généralement un taux d\'endettement inférieur à 35 %. Votre dossier est dans la zone favorable.';
  }

  ngOnInit(): void {
    this.updateEmployerValidators();
    this.sub.add(
      this.form.get('employmentStatus')?.valueChanges.subscribe(() => {
        this.updateEmployerValidators();
      }) ?? new Subscription()
    );
    this.sub.add(
      this.form.get('seniorityRange')?.valueChanges.subscribe((range) => {
        const months = seniorityMonthsFromRange(range as string);
        this.form.get('seniorityMonths')?.setValue(months, { emitEvent: false });
      }) ?? new Subscription()
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private updateEmployerValidators(): void {
    const required = this.showEmployerFields;
    const employerName = this.form.get('employerName');
    const seniorityRange = this.form.get('seniorityRange');
    if (required) {
      employerName?.setValidators([Validators.required, Validators.maxLength(120)]);
      seniorityRange?.setValidators([Validators.required]);
    } else {
      employerName?.clearValidators();
      seniorityRange?.clearValidators();
    }
    employerName?.updateValueAndValidity({ emitEvent: false });
    seniorityRange?.updateValueAndValidity({ emitEvent: false });
  }
}
