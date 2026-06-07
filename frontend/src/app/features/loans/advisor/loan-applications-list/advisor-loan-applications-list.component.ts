import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import {
  ADVISOR_LOAN_LIST_STATUS_TABS,
  LoanListSort,
  LoanListStatusFilter,
  countByStatus,
  formatDateFrLong,
  loanCardStatusStyle,
  loanPurposeLabel,
  matchesAdvisorLoanSearch,
  sortLoans,
} from '../../../../core/loans/utils/loan-list.util';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-advisor-loan-applications-list',
  standalone: true,
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './advisor-loan-applications-list.component.html',
  styleUrl: './advisor-loan-applications-list.component.scss',
})
export class AdvisorLoanApplicationsListComponent implements OnInit {
  private readonly loanApi = inject(LoanApiService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly statusTabs = ADVISOR_LOAN_LIST_STATUS_TABS;
  readonly statusStyle = loanCardStatusStyle;
  readonly purposeLabel = loanPurposeLabel;
  readonly formatSubmittedAt = formatDateFrLong;

  readonly displayedColumns = [
    'reference',
    'applicantName',
    'title',
    'loanPurpose',
    'requestedAmount',
    'requestedDurationMonths',
    'status',
    'submittedAt',
    'actions',
  ];

  readonly allLoans = signal<LoanResponseDto[]>([]);
  readonly loading = signal(false);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<LoanListStatusFilter>('ALL');
  readonly sortBy = signal<LoanListSort>('updatedDesc');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);

  readonly filteredLoans = computed(() => {
    const q = this.searchQuery();
    const status = this.statusFilter();
    let list = this.allLoans().filter((loan) => matchesAdvisorLoanSearch(loan, q));
    if (status !== 'ALL') {
      list = list.filter((loan) => loan.status === status);
    }
    return sortLoans(list, this.sortBy());
  });

  readonly paginatedLoans = computed(() => {
    const list = this.filteredLoans();
    const start = this.pageIndex() * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  readonly totalFiltered = computed(() => this.filteredLoans().length);

  readonly statusCounts = computed(() => {
    const searched = this.allLoans().filter((loan) =>
      matchesAdvisorLoanSearch(loan, this.searchQuery())
    );
    const counts: Record<LoanListStatusFilter, number> = {
      ALL: searched.length,
      DRAFT: 0,
      SUBMITTED: 0,
      UNDER_REVIEW: 0,
      OFFER_PENDING: 0,
      APPROVED: 0,
      REJECTED: 0,
      CANCELLED: 0,
    };
    for (const tab of this.statusTabs) {
      if (tab.key !== 'ALL') {
        counts[tab.key] = countByStatus(searched, tab.key);
      }
    }
    return counts;
  });

  readonly sortLabel = computed(() => {
    switch (this.sortBy()) {
      case 'updatedAsc':
        return 'Plus anciennes';
      case 'amountDesc':
        return 'Montant décroissant';
      case 'amountAsc':
        return 'Montant croissant';
      default:
        return 'Plus récentes';
    }
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loanApi.list({ page: 0, size: 200 }).subscribe({
      next: (page) => {
        this.allLoans.set(page.content.filter((loan) => loan.status !== 'DRAFT'));
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.pageIndex.set(0);
  }

  selectStatus(filter: LoanListStatusFilter): void {
    this.statusFilter.set(filter);
    this.pageIndex.set(0);
  }

  setSort(sort: LoanListSort): void {
    this.sortBy.set(sort);
    this.pageIndex.set(0);
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  tabCount(key: LoanListStatusFilter): number {
    return this.statusCounts()[key] ?? 0;
  }

  open(loan: LoanResponseDto): void {
    this.router.navigate(['/conseiller/dossiers', loan.id]);
  }

  submittedDate(loan: LoanResponseDto): string {
    const iso = loan.submittedAt ?? loan.updatedAt ?? loan.createdAt;
    return formatDateFrLong(iso) || '—';
  }
}
