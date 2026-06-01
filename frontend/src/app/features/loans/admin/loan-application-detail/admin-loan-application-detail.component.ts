import { CurrencyPipe, DatePipe, NgIf } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';

@Component({
  selector: 'app-admin-loan-application-detail',
  standalone: true,
  imports: [
    NgIf,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './admin-loan-application-detail.component.html',
  styleUrl: './admin-loan-application-detail.component.scss',
})
export class AdminLoanApplicationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly loanApi = inject(LoanApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly loan = signal<LoanResponseDto | null>(null);

  // UI helpers
  readonly statusIcon = (s: LoanResponseDto['status']) => {
    switch (s) {
      case 'SUBMITTED':
        return 'schedule';
      case 'UNDER_REVIEW':
        return 'find_in_page';
      case 'APPROVED':
        return 'check_circle';
      case 'REJECTED':
        return 'cancel';
      case 'CANCELLED':
        return 'do_not_disturb';
      default:
        return 'info';
    }
  };

  readonly statusClass = (s: LoanResponseDto['status']) => `status-chip status-${s.toLowerCase()}`;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!id || Number.isNaN(id)) {
      this.error.set("Identifiant invalide.");
      return;
    }
    this.fetch(id);
  }

  private fetch(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.loanApi.getById(id).subscribe({
      next: (data) => {
        this.loan.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        const message = err?.status === 404
          ? 'Demande introuvable (404).'
          : (err?.error?.message || err?.message || 'Erreur de chargement');
        this.error.set(message);
        this.loading.set(false);
      },
    });
  }
}
