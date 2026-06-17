import { CurrencyPipe, DatePipe, DecimalPipe, UpperCasePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RepaymentApiService } from '../../../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../../../core/loans/repayment/models/loan-summary.model';
import { RepaymentKpiDto } from '../../../../../core/loans/repayment/models/repayment-kpi.model';
import { RepaymentLoanStatus } from '../../../../../core/loans/repayment/models/repayment.enums';
import { advisorRepaymentLoanStatusStyle } from '../../../../../core/loans/repayment/constants/repayment.constants';
import { getErrorMessage } from '../../../../../core/loans/utils/api-error.util';

type RepaymentStatusFilter = 'ALL' | RepaymentLoanStatus;

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { value: RepaymentStatusFilter; label: string }[] = [
  { value: 'ALL', label: 'Tous les statuts' },
  { value: 'ACTIVE', label: 'Actif' },
  { value: 'DEFAULTED', label: 'En défaut' },
  { value: 'CLOSED', label: 'Soldé' },
  { value: 'PENDING_MANDATE', label: 'Mandat en attente' },
];

@Component({
  selector: 'app-admin-loan-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    UpperCasePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './admin-loan-dashboard.component.html',
  styleUrl: './admin-loan-dashboard.component.scss',
})
export class AdminLoanDashboardComponent implements OnInit {
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly statusOptions = STATUS_OPTIONS;
  readonly statusStyle = advisorRepaymentLoanStatusStyle;
  readonly PAGE_SIZE = PAGE_SIZE;

  readonly displayedColumns = [
    'reference',
    'client',
    'principalAmount',
    'remainingBalance',
    'nextInstallmentDate',
    'status',
    'overdue',
    'actions',
  ];

  readonly kpi = signal<RepaymentKpiDto | null>(null);
  readonly allLoans = signal<LoanSummaryDto[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly kpiLoading = signal(false);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<RepaymentStatusFilter>('ALL');
  readonly pageIndex = signal(0);

  private readonly searchResults = signal<LoanSummaryDto[]>([]);

  readonly rangeStart = computed(() => {
    const total = this.totalElements();
    if (!total) {
      return 0;
    }
    return this.pageIndex() * PAGE_SIZE + 1;
  });

  readonly rangeEnd = computed(() => {
    const total = this.totalElements();
    if (!total) {
      return 0;
    }
    return Math.min((this.pageIndex() + 1) * PAGE_SIZE, total);
  });

  ngOnInit(): void {
    this.loadKpi();
    this.load();
  }

  loadKpi(): void {
    this.kpiLoading.set(true);
    this.repaymentApi.getAdminKpi().subscribe({
      next: (kpi) => {
        this.kpi.set(kpi);
        this.kpiLoading.set(false);
      },
      error: (err) => {
        this.kpiLoading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }

  load(): void {
    this.loading.set(true);
    const status = this.statusFilter();
    const search = this.searchQuery().trim();
    const serverPage = search ? 0 : this.pageIndex();
    const serverSize = search ? 200 : PAGE_SIZE;

    this.repaymentApi
      .getAdminLoans({
        page: serverPage,
        size: serverSize,
        status: status === 'ALL' ? undefined : status,
      })
      .subscribe({
        next: (page) => {
          if (search) {
            const filtered = page.content.filter((loan) =>
              matchesSearch(loan, search.toLowerCase()),
            );
            this.searchResults.set(filtered);
            this.totalElements.set(filtered.length);
            this.applySearchPage();
          } else {
            this.searchResults.set([]);
            this.allLoans.set(page.content);
            this.totalElements.set(page.totalElements);
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
        },
      });
  }

  refresh(): void {
    this.loadKpi();
    this.load();
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.pageIndex.set(0);
    this.load();
  }

  onStatusChange(value: RepaymentStatusFilter): void {
    this.statusFilter.set(value);
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    if (this.searchQuery().trim()) {
      this.applySearchPage();
      return;
    }
    this.load();
  }

  private applySearchPage(): void {
    const filtered = this.searchResults();
    const start = this.pageIndex() * PAGE_SIZE;
    this.allLoans.set(filtered.slice(start, start + PAGE_SIZE));
  }

  open(loan: LoanSummaryDto): void {
    this.router.navigate(['/admin/prets', loan.id]);
  }

  overdueLabel(count: number): string {
    if (count <= 0) {
      return '—';
    }
    return count === 1 ? '1 éch. en retard' : `${count} éch. en retard`;
  }
}

function matchesSearch(loan: LoanSummaryDto, query: string): boolean {
  const haystack = [loan.reference, loan.borrowerName, loan.borrowerEmail]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return haystack.includes(query);
}
