import { Component, Input, OnChanges } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { LoanPurpose } from '../../../../../core/loans/models/loan.enums';
import {
  calculateLoanSimulation,
  LoanSimulation,
} from '../../../../../core/loans/utils/loan-calculator';
import { LOAN_DEBT_RATIO_MAX } from '../../../../../core/loans/constants/loan.constants';
import { computeIndicativeRatePercent } from '../../../../../core/loans/utils/loan-interest-rate.util';

@Component({
  selector: 'app-loan-simulation-card',
  standalone: true,
  imports: [MatCardModule, CurrencyPipe, DecimalPipe],
  template: `
    <mat-card class="simulation-card">
      <mat-card-header>
        <mat-card-title>Simulation indicative</mat-card-title>
        <mat-card-subtitle>Taux {{ interestPercent | number: '1.2-2' }} % — hors frais</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="simulation-metrics">
          <div class="row">
            <span class="label">Mensualité estimée</span>
            <span class="value">{{ simulation.monthlyPayment | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
          </div>
          <div class="row">
            <span class="label">Coût total du crédit</span>
            <span class="value">{{ simulation.totalCost | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
          </div>
          <div class="row">
            <span class="label">Intérêts estimés</span>
            <span class="value">{{ simulation.totalInterest | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
          </div>
          @if (debtRatio != null) {
            <div class="row">
              <span class="label">Taux d'endettement</span>
              <span class="value" [class.warn]="debtRatio > debtRatioMax">
                {{ debtRatio * 100 | number: '1.0-1' }} %
              </span>
            </div>
          }
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .simulation-card { background: #f8faf8; }
    .simulation-metrics { display: grid; gap: 0.75rem; }
    .row { display: flex; justify-content: space-between; gap: 1rem; }
    .label { font-size: 0.875rem; color: rgba(0,0,0,0.6); }
    .value { font-weight: 600; }
    .value.warn { color: #c62828; }
  `,
})
export class LoanSimulationCardComponent implements OnChanges {
  @Input() amount = 0;
  @Input() durationMonths = 0;
  @Input() loanPurpose: LoanPurpose = 'PERSONAL';
  @Input() debtRatio: number | null = null;

  readonly debtRatioMax = LOAN_DEBT_RATIO_MAX;
  interestPercent = 0;
  simulation: LoanSimulation = { monthlyPayment: 0, totalCost: 0, totalInterest: 0 };

  ngOnChanges(): void {
    this.interestPercent = computeIndicativeRatePercent(
      this.amount,
      this.durationMonths,
      this.loanPurpose
    );
    this.simulation = calculateLoanSimulation(this.amount, this.durationMonths, this.loanPurpose);
  }
}
