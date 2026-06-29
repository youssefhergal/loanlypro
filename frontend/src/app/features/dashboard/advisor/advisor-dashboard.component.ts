import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/services/auth.service';
import { DashboardApiService, AdvisorDashboardResponse, LoanApplicationSummaryDto } from '../../../core/dashboard/services/dashboard-api.service';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';

@Component({
  selector: 'app-advisor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, MatCardModule, MatListModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './advisor-dashboard.component.html',
  styleUrl: './advisor-dashboard.component.scss',
})
export class AdvisorDashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(DashboardApiService);

  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  private readonly pageContent = signal<LoanSummaryDto[]>([]);
  readonly loans = computed(() => this.pageContent());
  readonly total = signal<number>(0);
  // Nouvelles données: demandes (LoanApplication) assignées au conseiller
  private readonly assignedApps = signal<LoanApplicationSummaryDto[]>([]);
  readonly applications = computed(() => this.assignedApps());
  readonly applicationsTotal = signal<number>(0);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getAdvisorDashboard().subscribe({
      next: (res: AdvisorDashboardResponse) => {
        this.pageContent.set(res.loans ?? []);
        this.total.set(res.totalCount ?? res.loans?.length ?? 0);
        this.assignedApps.set(res.applications ?? []);
        this.applicationsTotal.set(res.applicationsCount ?? (res.applications?.length ?? 0));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le tableau de bord conseiller.');
        this.loading.set(false);
      },
    });
  }

  labelForStatus(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'Brouillon';
      case 'SUBMITTED':
        return 'Soumise';
      case 'UNDER_REVIEW':
        return 'En étude';
      case 'OFFER_PENDING':
        return 'Offre en attente';
      case 'APPROVED':
        return 'Approuvée';
      case 'REJECTED':
        return 'Refusée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }

  iconForStatus(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'edit_note';
      case 'SUBMITTED':
      case 'UNDER_REVIEW':
        return 'hourglass_top';
      case 'OFFER_PENDING':
        return 'assignment_turned_in';
      case 'APPROVED':
        return 'verified';
      case 'REJECTED':
        return 'block';
      default:
        return 'description';
    }
  }
}
