import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { AdminApiService } from '../../../core/admin/services/admin-api.service';
import {
  applicationStatusLabel,
  buildPipelineConicGradient,
  buildPipelineSegments,
  buildExtremesAdvisorWorkloadRows,
  formatRelativeSubmittedAt,
  pipelineSegmentsTotal,
  statusCount,
} from '../../../core/dashboard/utils/admin-dashboard-display.util';
import { LoanApiService } from '../../../core/loans/services/loan-api.service';
import { AdminLoanListSummary } from '../../../core/loans/models/admin-loan-list-summary.model';
import { LoanResponseDto } from '../../../core/loans/models/loan-response.model';
import { loanCardStatusStyle } from '../../../core/loans/utils/loan-list.util';
import { RepaymentKpiDto } from '../../../core/loans/repayment/models/repayment-kpi.model';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';
import { RepaymentApiService } from '../../../core/loans/repayment/services/repayment-api.service';
import { CreateUserDialogComponent } from '../../users/admin-users/create-user-dialog/create-user-dialog.component';

interface UserPageResponse {
  content: { emailVerified: boolean }[];
  totalElements: number;
}

interface UsersSnapshot {
  total: number;
  clients: number;
  staff: number;
  unverified: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DecimalPipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly loanApi = inject(LoanApiService);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly adminApi = inject(AdminApiService);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly usersUrl = `${environment.apiUrl}/v1/admin/users`;

  readonly loading = signal(true);
  readonly assigning = signal(false);
  readonly error = signal<string | null>(null);
  readonly summary = signal<AdminLoanListSummary | null>(null);
  readonly kpi = signal<RepaymentKpiDto | null>(null);
  readonly unassignedApplications = signal<LoanResponseDto[]>([]);
  readonly watchLoans = signal<LoanSummaryDto[]>([]);
  readonly usersSnapshot = signal<UsersSnapshot | null>(null);
  readonly advisorCounts = signal<{ id: number; name: string; count: number }[]>([]);

  readonly helpers = {
    applicationStatusLabel,
    formatRelativeSubmittedAt,
    loanCardStatusStyle,
  };

  readonly pipelineSegments = computed(() =>
    buildPipelineSegments(this.summary()?.statusCounts ?? {}),
  );

  readonly pipelineConicGradient = computed(() =>
    buildPipelineConicGradient(this.pipelineSegments()),
  );

  readonly pipelineTotal = computed(() => pipelineSegmentsTotal(this.pipelineSegments()));

  readonly workloadRows = computed(() => buildExtremesAdvisorWorkloadRows(this.advisorCounts()));

  readonly totalApplications = computed(() => this.summary()?.totalCount ?? 0);
  readonly unassignedCount = computed(() => this.summary()?.unassignedCount ?? 0);
  readonly underReviewCount = computed(() =>
    statusCount(this.summary()?.statusCounts ?? {}, 'UNDER_REVIEW'),
  );
  readonly overdueCount = computed(() => this.kpi()?.overdueInstallmentsCount ?? 0);

  readonly hasUnassignedAlert = computed(() => this.unassignedCount() > 0);
  readonly hasOverdueAlert = computed(() => this.overdueCount() > 0);

  readonly isEmptyPlatform = computed(
    () => this.totalApplications() === 0 && (this.kpi()?.activeLoansCount ?? 0) === 0,
  );

  readonly overdueWatchLoans = computed(() =>
    [...this.watchLoans()]
      .sort((a, b) => b.overdueInstallmentsCount - a.overdueInstallmentsCount)
      .slice(0, 3),
  );

  ngOnInit(): void {
    this.loadDashboard();
  }

  runAdvisorAssignment(): void {
    this.assigning.set(true);
    this.adminApi.runAdvisorAssignment().subscribe({
      next: (result) => {
        this.assigning.set(false);
        this.snackBar.open(
          `${result.assignedCount} dossier(s) affecté(s). ${result.unassignedRemaining} restant(s).`,
          'Fermer',
          { duration: 5000 },
        );
        this.loadDashboard();
      },
      error: () => {
        this.assigning.set(false);
        this.snackBar.open("Échec de l'affectation automatique.", 'Fermer', { duration: 4000 });
      },
    });
  }

  openCreateUserDialog(): void {
    const ref = this.dialog.open(CreateUserDialogComponent, {
      width: '520px',
      panelClass: 'admin-users-dialog-panel',
    });
    ref.afterClosed().subscribe((created) => {
      if (created) {
        this.snackBar.open('Compte créé avec succès.', 'Fermer', { duration: 4000 });
        this.loadUsersSnapshot();
      }
    });
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      summary: this.loanApi.getAdminSummary(),
      kpi: this.repaymentApi.getAdminKpi().pipe(catchError(() => of(null))),
      unassigned: this.loanApi
        .listAdmin({ unassignedOnly: true, page: 0, size: 5, sort: 'UPDATED_DESC' })
        .pipe(map((page) => page.content ?? [])),
      loans: this.repaymentApi
        .getAdminLoans({ page: 0, size: 20, status: 'ACTIVE' })
        .pipe(
          map((page) => page.content ?? []),
          catchError(() => of([] as LoanSummaryDto[])),
        ),
    }).subscribe({
      next: ({ summary, kpi, unassigned, loans }) => {
        this.summary.set(summary);
        this.kpi.set(kpi);
        this.unassignedApplications.set(unassigned);
        this.watchLoans.set(loans);
        this.loadAdvisorCounts(summary);
        this.loadUsersSnapshot();
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le tableau de bord administrateur.');
        this.loading.set(false);
      },
    });
  }

  private loadAdvisorCounts(summary: AdminLoanListSummary): void {
    const advisors = summary.advisors ?? [];
    if (advisors.length === 0) {
      this.advisorCounts.set([]);
      return;
    }

    forkJoin(
      advisors.map((advisor) =>
        this.loanApi.listAdmin({ advisorId: advisor.id, page: 0, size: 1 }).pipe(
          map((page) => ({
            id: advisor.id,
            name: advisor.name,
            count: page.totalElements ?? 0,
          })),
        ),
      ),
    ).subscribe({
      next: (counts) => this.advisorCounts.set(counts),
      error: () => this.advisorCounts.set([]),
    });
  }

  private loadUsersSnapshot(): void {
    const fetchTotal = (params: HttpParams) =>
      this.http
        .get<UserPageResponse>(this.usersUrl, { params })
        .pipe(catchError(() => of({ content: [], totalElements: 0 })));

    forkJoin({
      total: fetchTotal(new HttpParams().set('page', '0').set('size', '1')),
      clients: fetchTotal(new HttpParams().set('page', '0').set('size', '1').set('role', 'CLIENT')),
      conseillers: fetchTotal(
        new HttpParams().set('page', '0').set('size', '1').set('role', 'CONSEILLER'),
      ),
      admins: fetchTotal(new HttpParams().set('page', '0').set('size', '1').set('role', 'ADMIN')),
      sample: fetchTotal(new HttpParams().set('page', '0').set('size', '200')),
    }).subscribe({
      next: ({ total, clients, conseillers, admins, sample }) => {
        const unverified = (sample.content ?? []).filter((user) => !user.emailVerified).length;
        this.usersSnapshot.set({
          total: total.totalElements ?? 0,
          clients: clients.totalElements ?? 0,
          staff: (conseillers.totalElements ?? 0) + (admins.totalElements ?? 0),
          unverified,
        });
      },
    });
  }
}
