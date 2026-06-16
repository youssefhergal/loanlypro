import { Component, input } from '@angular/core';
import { installmentStatusStyle } from '../../../../../core/loans/repayment/constants/repayment.constants';

@Component({
  selector: 'app-installment-status-chip',
  standalone: true,
  template: `
    @if (status()) {
      @let style = statusStyle();
      <span
        class="installment-chip"
        [style.background]="style.badgeBg"
        [style.color]="style.badgeColor"
      >
        {{ style.label }}
      </span>
    }
  `,
  styles: [
    `
      .installment-chip {
        display: inline-flex;
        align-items: center;
        padding: 0.15rem 0.55rem;
        border-radius: 6px;
        font-size: 0.6875rem;
        font-weight: 700;
        white-space: nowrap;
      }
    `,
  ],
})
export class InstallmentStatusChipComponent {
  readonly status = input<string | null>(null);

  statusStyle() {
    return installmentStatusStyle(this.status() ?? 'UPCOMING');
  }
}
