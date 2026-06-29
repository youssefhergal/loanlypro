import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/services/auth.service';
import { AdminLoanListSummary } from '../../../core/loans/models/admin-loan-list-summary.model';
import { DashboardApiService } from '../../../core/dashboard/services/dashboard-api.service';
import { LoanApplicationStatus } from '../../../core/loans/models/loan.enums';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatListModule, MatChipsModule, MatProgressSpinnerModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly dashboardApi = inject(DashboardApiService);

  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  private readonly data = signal<AdminLoanListSummary | null>(null);
  readonly summary = this.data.asReadonly();

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.dashboardApi.getAdminDashboard().subscribe({
      next: (res: AdminLoanListSummary) => {
        this.data.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le tableau de bord administrateur.');
        this.loading.set(false);
      }
    });
  }

  statusEntries = computed(() => {
    const map = this.data()?.statusCounts || {} as Partial<Record<LoanApplicationStatus, number>>;
    return Object.entries(map).map(([key, value]) => ({ key, value }))
      .filter(e => e.value != null) as { key: string, value: number }[];
  });

  labelForStatus(status?: string): string {
    switch (status) {
      case 'DRAFT': return 'Brouillon';
      case 'SUBMITTED': return 'Soumise';
      case 'UNDER_REVIEW': return 'En étude';
      case 'OFFER_PENDING': return 'Offre en attente';
      case 'APPROVED': return 'Approuvée';
      case 'REJECTED': return 'Refusée';
      case 'CANCELLED': return 'Annulée';
      default: return status || '';
    }
  }
}
