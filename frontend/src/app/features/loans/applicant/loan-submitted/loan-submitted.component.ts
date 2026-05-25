import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import { LOAN_PURPOSE_OPTIONS } from '../../../../core/loans/constants/loan.constants';
import { calculateMonthlyPayment } from '../../../../core/loans/utils/loan-calculator';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-loan-submitted',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, CurrencyPipe],
  templateUrl: './loan-submitted.component.html',
  styleUrl: './loan-submitted.component.scss',
})
export class LoanSubmittedComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly loanApi = inject(LoanApiService);
  readonly auth = inject(AuthService);

  readonly loan = signal<LoanResponseDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly purposeLabel = computed(() => {
    const loan = this.loan();
    if (!loan) return '—';
    return LOAN_PURPOSE_OPTIONS.find((p) => p.value === loan.loanPurpose)?.label ?? loan.loanPurpose;
  });

  readonly monthlyPayment = computed(() => {
    const loan = this.loan();
    if (!loan) return 0;
    return calculateMonthlyPayment(
      Number(loan.requestedAmount),
      loan.requestedDurationMonths
    );
  });

  readonly durationLabel = computed(() => {
    const months = this.loan()?.requestedDurationMonths ?? 0;
    if (months % 12 === 0 && months > 0) {
      return `${months} mois`;
    }
    return `${months} mois`;
  });

  readonly referenceDisplay = computed(() => {
    const ref = this.loan()?.reference;
    return ref ? `#${ref}` : '—';
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Dossier introuvable.');
      this.loading.set(false);
      return;
    }
    this.loanApi.getById(id).subscribe({
      next: (loan) => {
        this.loan.set(loan);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(getErrorMessage(err, 'Impossible de charger le dossier.'));
        this.loading.set(false);
      },
    });
  }
}
