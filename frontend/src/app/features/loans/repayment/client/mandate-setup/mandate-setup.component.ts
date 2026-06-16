import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../../../core/auth/services/auth.service';
import {
  MANDATE_CONSENT_FINE_PRINT,
} from '../../../../../core/loans/repayment/constants/repayment.constants';
import { LoanDetailDto } from '../../../../../core/loans/repayment/models/loan-detail.model';
import { RepaymentApiService } from '../../../../../core/loans/repayment/services/repayment-api.service';
import {
  applyApiErrorsToForm,
  getErrorMessage,
} from '../../../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-mandate-setup',
  standalone: true,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './mandate-setup.component.html',
  styleUrl: './mandate-setup.component.scss',
})
export class MandateSetupComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly consentFinePrint = MANDATE_CONSENT_FINE_PRINT;
  readonly submitting = signal(false);
  readonly loading = signal(true);
  readonly loanId = signal<number | null>(null);
  readonly loan = signal<LoanDetailDto | null>(null);

  readonly form = this.fb.nonNullable.group({
    holderName: ['', [Validators.required, Validators.maxLength(120)]],
    iban: ['', [Validators.required, Validators.minLength(15), Validators.maxLength(34)]],
    consent: [false, Validators.requiredTrue],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.snackBar.open('Identifiant de prêt invalide.', 'Fermer', { duration: 4000 });
      void this.router.navigate(['/mes-prets']);
      return;
    }

    this.loanId.set(id);
    this.repaymentApi.getLoan(id).subscribe({
      next: (detail) => {
        if (detail.mandateStatus === 'ACTIVE') {
          this.snackBar.open('Un mandat actif est déjà configuré pour ce prêt.', 'OK', {
            duration: 4000,
          });
          void this.router.navigate(['/mes-prets', id]);
          return;
        }
        this.loan.set(detail);
        this.prefillHolderName(detail);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
        void this.router.navigate(['/mes-prets']);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.loanId() == null) {
      this.form.markAllAsTouched();
      return;
    }

    const { holderName, iban } = this.form.getRawValue();
    this.submitting.set(true);
    this.repaymentApi
      .activateMandate(this.loanId()!, {
        holderName: holderName.trim(),
        iban: iban.replace(/\s/g, '').toUpperCase(),
      })
      .subscribe({
        next: (response) => {
          this.submitting.set(false);
          this.snackBar.open('Mandat activé avec succès.', 'OK', { duration: 4000 });
          void this.router.navigate(['/mes-prets', response.loanId]);
        },
        error: (err) => {
          this.submitting.set(false);
          applyApiErrorsToForm(this.form, err);
          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
        },
      });
  }

  private prefillHolderName(detail: LoanDetailDto): void {
    if (detail.holderName?.trim()) {
      this.form.controls.holderName.setValue(detail.holderName.trim());
      return;
    }

    if (detail.borrowerName?.trim()) {
      this.form.controls.holderName.setValue(detail.borrowerName.trim());
      return;
    }

    const user = this.auth.currentUser();
    if (user) {
      const fullName = `${user.firstName} ${user.lastName}`.trim();
      if (fullName) {
        this.form.controls.holderName.setValue(fullName);
      }
    }
  }
}
