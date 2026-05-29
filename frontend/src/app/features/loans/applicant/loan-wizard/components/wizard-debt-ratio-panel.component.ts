import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { LOAN_DEBT_RATIO_DISPLAY_MAX } from '../../../../../core/loans/constants/loan.constants';

export type DebtRatioStatus = 'ok' | 'warn' | 'danger';

@Component({
  selector: 'app-wizard-debt-ratio-panel',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, MatIconModule],
  template: `
    <section
      class="loan-detail-card debt-panel"
      [class.debt-panel--warn]="status === 'warn'"
      [class.debt-panel--danger]="status === 'danger'"
    >
      <h3 class="loan-detail-card__title debt-panel__title">
        <mat-icon>speed</mat-icon>
        Taux d'endettement
      </h3>
      <p class="loan-detail-card__desc debt-panel__subtitle">
        Calculé en temps réel selon vos revenus et charges.
      </p>

      <div class="debt-panel__gauge-wrap">
        <svg class="debt-panel__gauge" viewBox="0 0 200 118" aria-hidden="true">
          <path class="debt-panel__gauge-track" [attr.d]="trackPath" />
          <path
            class="debt-panel__gauge-value"
            [class.debt-panel__gauge-value--warn]="status === 'warn'"
            [class.debt-panel__gauge-value--danger]="status === 'danger'"
            [attr.d]="trackPath"
            [attr.stroke-dasharray]="gaugeDash"
            stroke-dashoffset="0"
          />
        </svg>
        <div class="debt-panel__gauge-center">
          <span class="debt-panel__percent">{{ percent | number: '1.1-1' }}%</span>
          <span class="debt-panel__percent-label">de taux d'endettement</span>
        </div>
        <div class="debt-panel__scale">
          <span>0%</span>
          <span class="debt-panel__scale-warn">⚠ {{ displayMaxPercent }}%</span>
          <span>50%+</span>
        </div>
      </div>

      <p
        class="debt-panel__status"
        [class.debt-panel__status--warn]="status === 'warn'"
        [class.debt-panel__status--danger]="status === 'danger'"
      >
        <mat-icon>{{ statusIcon }}</mat-icon>
        {{ statusMessage }}
      </p>

      <dl class="debt-panel__stats">
        <div>
          <dt>Charges mensuelles</dt>
          <dd>{{ totalCharges | currency: 'EUR' : 'symbol' : '1.0-0' }}</dd>
        </div>
        <div>
          <dt>Revenus nets totaux</dt>
          <dd>{{ totalIncome | currency: 'EUR' : 'symbol' : '1.0-0' }}</dd>
        </div>
        <div>
          <dt>Taux calculé</dt>
          <dd>{{ percent | number: '1.1-1' }} %</dd>
        </div>
      </dl>
    </section>
  `,
  styles: `
    .debt-panel {
      margin-bottom: 0;
    }

    .debt-panel__title {
      margin-bottom: 0.35rem;
    }

    .debt-panel__subtitle {
      margin-top: -0.25rem;
      margin-bottom: 0.5rem;
    }

    .debt-panel__gauge-wrap {
      position: relative;
      margin: 0.25rem 0 0.5rem;
    }

    .debt-panel__gauge {
      width: 100%;
      height: auto;
      display: block;
    }

    .debt-panel__gauge-track {
      fill: none;
      stroke: var(--color-border, #d8e8de);
      stroke-width: 10;
      stroke-linecap: round;
    }

    .debt-panel__gauge-value {
      fill: none;
      stroke: var(--color-primary, #1a7a4a);
      stroke-width: 10;
      stroke-linecap: round;
      transition: stroke-dasharray 0.35s ease;
    }

    .debt-panel__gauge-value--warn {
      stroke: #d97706;
    }

    .debt-panel__gauge-value--danger {
      stroke: #c62828;
    }

    .debt-panel__gauge-center {
      position: absolute;
      left: 50%;
      bottom: 1.35rem;
      transform: translateX(-50%);
      text-align: center;
      pointer-events: none;
    }

    .debt-panel__percent {
      display: block;
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--color-primary, #1a7a4a);
      line-height: 1.1;
    }

    .debt-panel--warn .debt-panel__percent {
      color: #d97706;
    }

    .debt-panel--danger .debt-panel__percent {
      color: #c62828;
    }

    .debt-panel__percent-label {
      font-size: 0.6875rem;
      color: var(--color-text-muted, #6b7c72);
    }

    .debt-panel__scale {
      display: flex;
      justify-content: space-between;
      font-size: 0.6875rem;
      color: var(--color-text-muted, #6b7c72);
      padding: 0 0.35rem;
    }

    .debt-panel__scale-warn {
      color: #d97706;
      font-weight: 600;
    }

    .debt-panel__status {
      display: flex;
      align-items: flex-start;
      gap: 0.35rem;
      margin: 0 0 0.65rem;
      padding: 0.5rem 0.6rem;
      border-radius: var(--radius-sm, 8px);
      background: var(--color-primary-light, #e8f5ee);
      font-size: 0.8125rem;
      line-height: 1.4;
      color: var(--color-primary, #1a7a4a);
    }

    .debt-panel__status mat-icon {
      font-size: 1.1rem;
      width: 1.1rem;
      height: 1.1rem;
      flex-shrink: 0;
    }

    .debt-panel__status--warn {
      background: #fffbeb;
      color: #92400e;
    }

    .debt-panel__status--danger {
      background: #ffebee;
      color: #c62828;
    }

    .debt-panel__stats {
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    .debt-panel__stats div {
      display: flex;
      justify-content: space-between;
      gap: 0.5rem;
      font-size: 0.8125rem;
    }

    .debt-panel__stats dt {
      color: var(--color-text-muted, #6b7c72);
      font-weight: 500;
    }

    .debt-panel__stats dd {
      margin: 0;
      font-weight: 600;
      color: var(--color-text, #1a2e22);
    }
  `,
})
export class WizardDebtRatioPanelComponent {
  @Input() debtRatio = 0;
  @Input() totalIncome = 0;
  @Input() totalCharges = 0;

  readonly displayMaxPercent = LOAN_DEBT_RATIO_DISPLAY_MAX * 100;
  readonly trackPath = 'M 24 100 A 76 76 0 0 1 176 100';
  private readonly arcLength = 238.76;

  get percent(): number {
    return Math.min(this.debtRatio * 100, 55);
  }

  get gaugeDash(): string {
    const ratio = Math.min(this.debtRatio / 0.5, 1);
    const filled = ratio * this.arcLength;
    return `${filled} ${this.arcLength}`;
  }

  get status(): DebtRatioStatus {
    if (this.debtRatio >= 0.5) return 'danger';
    if (this.debtRatio > LOAN_DEBT_RATIO_DISPLAY_MAX) return 'warn';
    return 'ok';
  }

  get statusIcon(): string {
    if (this.status === 'danger') return 'error';
    if (this.status === 'warn') return 'warning';
    return 'check_circle';
  }

  get statusMessage(): string {
    if (this.status === 'danger') {
      return 'Taux très élevé : vos charges et la mensualité dépassent largement le seuil habituel.';
    }
    if (this.status === 'warn') {
      return 'Au-dessus du seuil de 35 % : ajustez vos charges ou le montant emprunté.';
    }
    return 'En dessous du seuil de 35 % — votre dossier est dans la zone favorable.';
  }
}
