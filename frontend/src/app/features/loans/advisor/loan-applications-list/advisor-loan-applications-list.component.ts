import { CurrencyPipe, DatePipe, NgIf } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';

@Component({
  selector: 'app-advisor-loan-applications-list',
  standalone: true,
  imports: [
    NgIf,
    CurrencyPipe,
    DatePipe,
    MatTableModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './advisor-loan-applications-list.component.html',
  styleUrl: './advisor-loan-applications-list.component.scss',
})
export class AdvisorLoanApplicationsListComponent implements OnInit {
  private readonly loanApi = inject(LoanApiService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly loans = signal<LoanResponseDto[]>([]);

  readonly displayedColumns = [
    'reference',
    'applicantName',
    'title',
    'requestedAmount',
    'status',
    'updatedAt',
  ];

  // UI helpers pour pastille de statut
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
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.loanApi.list({ page: 0, size: 200 }).subscribe({
      next: (page) => {
        this.loans.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        const message = err?.error?.message || err?.message || 'Erreur de chargement';
        this.error.set(message);
        this.loading.set(false);
      },
    });
  }

  open(id: number): void {
    if (id != null) {
      this.router.navigate(['/conseiller/dossiers', id]);
    }
  }
}
