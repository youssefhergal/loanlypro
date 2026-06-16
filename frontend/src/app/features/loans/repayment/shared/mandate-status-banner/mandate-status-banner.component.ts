import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { needsMandateSetup } from '../../../../../core/loans/repayment/constants/repayment.constants';

@Component({
  selector: 'app-mandate-status-banner',
  standalone: true,
  imports: [MatIconModule, RouterLink, MatButtonModule],
  template: `
    @if (showBanner()) {
      <div class="mandate-banner" [class.mandate-banner--danger]="loanStatus() === 'DEFAULTED'">
        <mat-icon>{{ icon() }}</mat-icon>
        <div class="mandate-banner__body">
          <strong>{{ title() }}</strong>
          <p>{{ message() }}</p>
        </div>
        @if (showCta()) {
          <a
            mat-flat-button
            color="primary"
            class="mandate-banner__cta"
            [routerLink]="['/mes-prets', loanId(), 'mandat']"
          >
            Configurer le prélèvement
          </a>
        }
      </div>
    }
  `,
  styles: [
    `
      .mandate-banner {
        display: flex;
        flex-wrap: wrap;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 0.9rem 1rem;
        margin-bottom: 1rem;
        border-radius: 10px;
        background: #fff3e0;
        border: 1px solid #ffcc80;
        color: #e65100;

        mat-icon {
          margin-top: 0.1rem;
        }
      }

      .mandate-banner--danger {
        background: #ffebee;
        border-color: #ef9a9a;
        color: #c62828;
      }

      .mandate-banner__body {
        flex: 1;
        min-width: 200px;

        p {
          margin: 0.25rem 0 0;
          font-size: 0.875rem;
          line-height: 1.45;
        }
      }

      .mandate-banner__cta {
        margin-left: auto;
      }
    `,
  ],
})
export class MandateStatusBannerComponent {
  readonly loanId = input.required<number>();
  readonly loanStatus = input.required<string>();
  readonly mandateStatus = input<string | null>(null);
  readonly overdueCount = input(0);

  showBanner(): boolean {
    if (this.loanStatus() === 'DEFAULTED' || this.overdueCount() > 0) {
      return true;
    }
    return needsMandateSetup(this.loanStatus(), this.mandateStatus());
  }

  showCta(): boolean {
    return needsMandateSetup(this.loanStatus(), this.mandateStatus());
  }

  icon(): string {
    if (this.loanStatus() === 'DEFAULTED' || this.overdueCount() > 0) {
      return 'warning';
    }
    return 'account_balance';
  }

  title(): string {
    if (this.loanStatus() === 'DEFAULTED') {
      return 'Prêt en défaut de paiement';
    }
    if (this.overdueCount() > 0) {
      return 'Échéance(s) en retard';
    }
    return 'Mandat de prélèvement requis';
  }

  message(): string {
    if (this.loanStatus() === 'DEFAULTED') {
      return 'Des échéances n’ont pas pu être prélevées. Contactez votre conseiller pour régulariser la situation.';
    }
    if (this.overdueCount() > 0) {
      return `${this.overdueCount()} échéance(s) en retard. Les tentatives de prélèvement automatique continuent selon le calendrier prévu.`;
    }
    return 'Configurez votre IBAN et activez le mandat SEPA pour lancer les prélèvements automatiques.';
  }
}
