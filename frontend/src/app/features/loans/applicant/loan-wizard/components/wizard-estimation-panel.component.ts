import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { LoanPurpose } from '../../../../../core/loans/models/loan.enums';
import {
  calculateLoanSimulation,
  LoanSimulation,
} from '../../../../../core/loans/utils/loan-calculator';
import {
  computeIndicativeRatePercent,
  isGreenLoanPurpose,
} from '../../../../../core/loans/utils/loan-interest-rate.util';

@Component({
  selector: 'app-wizard-estimation-panel',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, MatIconModule],
  template: `
    <section class="loan-detail-card estimation-panel">
      <h3 class="loan-detail-card__title estimation-panel__title">
        <mat-icon>calculate</mat-icon>
        Estimation
      </h3>

      <div class="estimation-panel__highlight">
        <span class="estimation-panel__highlight-label">Mensualité estimée</span>
        <p class="estimation-panel__highlight-value">
          {{ simulation.monthlyPayment | currency: 'EUR' : 'symbol' : '1.0-0' }}
        </p>
        <p class="estimation-panel__highlight-sub">
          / mois pendant {{ durationMonths }} mois
        </p>
      </div>

      <dl class="estimation-panel__details">
        <div>
          <dt>Montant emprunté</dt>
          <dd>{{ amount | currency: 'EUR' : 'symbol' : '1.0-0' }}</dd>
        </div>
        <div>
          <dt>Taux indicatif</dt>
          <dd>
            {{ interestPercent | number: '1.2-2' }} %
            @if (isGreen) {
              <span class="estimation-panel__green-badge">Crédit vert</span>
            }
          </dd>
        </div>
        <div>
          <dt>Coût total</dt>
          <dd>{{ simulation.totalCost | currency: 'EUR' : 'symbol' : '1.0-0' }}</dd>
        </div>
      </dl>

      <p class="estimation-panel__disclaimer">
        * Taux calculé selon montant, durée et type de projet — simulation non contractuelle
      </p>
    </section>
  `,
  styles: `
    .estimation-panel {
      margin-bottom: 0;
    }

    .estimation-panel__title {
      margin-bottom: 0.5rem;
    }

    .estimation-panel__highlight {
      background: var(--color-primary-light, #e8f5ee);
      border-radius: var(--radius-sm, 8px);
      padding: 0.5rem 0.65rem;
      margin-bottom: 0.5rem;
    }

    .estimation-panel__highlight-label {
      font-size: 0.8125rem;
      color: var(--color-text-muted, #6b7c72);
    }

    .estimation-panel__highlight-value {
      margin: 0.1rem 0 0;
      font-size: 1.125rem;
      font-weight: 700;
      color: var(--color-primary, #1a7a4a);
      line-height: 1.2;
    }

    .estimation-panel__highlight-sub {
      margin: 0.2rem 0 0;
      font-size: 0.8125rem;
      color: var(--color-text-muted, #6b7c72);
    }

    .estimation-panel__details {
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .estimation-panel__details div {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 0.75rem;
    }

    .estimation-panel__details dt {
      font-size: 0.875rem;
      color: var(--color-text-muted, #6b7c72);
      font-weight: 500;
    }

    .estimation-panel__details dd {
      margin: 0;
      font-size: 0.9375rem;
      font-weight: 600;
      color: var(--color-text, #1a2e22);
      display: flex;
      align-items: center;
      gap: 0.35rem;
      flex-wrap: wrap;
      justify-content: flex-end;
    }

    .estimation-panel__green-badge {
      font-size: 0.6875rem;
      font-weight: 650;
      color: var(--color-primary, #1a7a4a);
      background: color-mix(in srgb, var(--color-primary, #1a7a4a) 12%, #fff);
      border-radius: 999px;
      padding: 0.1rem 0.45rem;
    }

    .estimation-panel__disclaimer {
      margin: 0.5rem 0 0;
      font-size: 0.75rem;
      color: var(--color-text-muted, #6b7c72);
      font-style: italic;
    }
  `,
})
export class WizardEstimationPanelComponent implements OnChanges {
  @Input() amount = 0;
  @Input() durationMonths = 0;
  @Input() loanPurpose: LoanPurpose = 'PERSONAL';

  interestPercent = 0;
  isGreen = false;
  simulation: LoanSimulation = { monthlyPayment: 0, totalCost: 0, totalInterest: 0 };

  ngOnChanges(): void {
    this.interestPercent = computeIndicativeRatePercent(
      this.amount,
      this.durationMonths,
      this.loanPurpose
    );
    this.isGreen = isGreenLoanPurpose(this.loanPurpose);
    this.simulation = calculateLoanSimulation(this.amount, this.durationMonths, this.loanPurpose);
  }
}
