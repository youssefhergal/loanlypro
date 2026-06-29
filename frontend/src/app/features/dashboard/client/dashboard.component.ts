import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/auth/services/auth.service';
import { DashboardApiService, DashboardResponse } from '../../../core/dashboard/services/dashboard-api.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatDividerModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(DashboardApiService);

  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  private readonly data = signal<DashboardResponse | null>(null);

  readonly demandes = computed(() => this.data()?.demandes ?? []);
  readonly documents = computed(() => this.data()?.documents ?? []);
  readonly transactions = computed(() => this.data()?.transactions ?? []);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getMyDashboard().subscribe({
      next: (res: DashboardResponse) => {
        this.data.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger le tableau de bord.");
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

  statusClass(status: string): string {
    return `chip ${status}`;
  }
}
