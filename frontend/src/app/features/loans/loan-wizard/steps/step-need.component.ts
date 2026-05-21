import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { LoanWizardStateService } from '../../../../core/loans/services/loan-wizard-state.service';
import { LOAN_PURPOSE_OPTIONS } from '../../../../core/loans/constants/loan.constants';
import { LoanSimulationCardComponent } from '../components/loan-simulation-card.component';
import {
  LOAN_AMOUNT_MAX,
  LOAN_AMOUNT_MIN,
  LOAN_DURATION_MAX,
  LOAN_DURATION_MIN,
} from '../../../../core/loans/constants/loan.constants';

@Component({
  selector: 'app-step-need',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    LoanSimulationCardComponent,
    DecimalPipe,
  ],
  templateUrl: './step-need.component.html',
  styleUrl: './step-need.component.scss',
})
export class StepNeedComponent {
  readonly state = inject(LoanWizardStateService);
  readonly form = this.state.form;
  readonly purposeOptions = LOAN_PURPOSE_OPTIONS;
  readonly amountMin = LOAN_AMOUNT_MIN;
  readonly amountMax = LOAN_AMOUNT_MAX;
  readonly durationMin = LOAN_DURATION_MIN;
  readonly durationMax = LOAN_DURATION_MAX;
}
