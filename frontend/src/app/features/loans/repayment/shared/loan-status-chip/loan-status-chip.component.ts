import { Component, input } from '@angular/core';
import { repaymentLoanStatusStyle } from '../../../../../core/loans/repayment/constants/repayment.constants';

@Component({
  selector: 'app-loan-status-chip',
  standalone: true,
  template: `
    @let style = statusStyle();
    <span
      class="status-chip"
      [style.background]="style.badgeBg"
      [style.color]="style.badgeColor"
    >
      <span class="status-chip__dot" [style.background]="style.accent"></span>
      {{ style.label }}
    </span>
  `,
  styles: [
    `
      .status-chip {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        padding: 0.2rem 0.6rem;
        border-radius: 6px;
        font-size: 0.75rem;
        font-weight: 700;
        white-space: nowrap;
      }

      .status-chip__dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }
    `,
  ],
})
export class LoanStatusChipComponent {
  readonly status = input.required<string>();

  statusStyle() {
    return repaymentLoanStatusStyle(this.status());
  }
}
