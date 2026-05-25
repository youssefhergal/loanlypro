import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import {
  LOAN_LIST_STATUS_TABS,
  LoanListSort,
  LoanListStatusFilter,
  countByStatus,
  loanCardMetaLine,
  loanCardRelativeTime,
  loanCardStatusStyle,
  loanPurposeIcon,
  loanPurposeLabel,
  matchesLoanSearch,
  sortLoans,
} from '../../../../core/loans/utils/loan-list.util';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';
import { ConfirmDialogService } from '../../../../shared/confirm-dialog/confirm-dialog.service';
import { filter } from 'rxjs';

@Component({
  selector: 'app-loan-list',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
  ],
  templateUrl: './loan-list.component.html',
  styleUrl: './loan-list.component.scss',
})
export class LoanListComponent implements OnInit {
  private readonly loanApi = inject(LoanApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly statusTabs = LOAN_LIST_STATUS_TABS;
  readonly cardHelpers = {
    statusStyle: loanCardStatusStyle,
    purposeLabel: loanPurposeLabel,
    purposeIcon: loanPurposeIcon,
    metaLine: loanCardMetaLine,
    relativeTime: loanCardRelativeTime,
  };

  readonly allLoans = signal<LoanResponseDto[]>([]);
  readonly loading = signal(false);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<LoanListStatusFilter>('ALL');
  readonly sortBy = signal<LoanListSort>('updatedDesc');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(7);

  readonly filteredLoans = computed(() => {
    const q = this.searchQuery();
    const status = this.statusFilter();
    let list = this.allLoans().filter((loan) => matchesLoanSearch(loan, q));
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
    const all = this.allLoans();
    const q = this.searchQuery();
    const searched = all.filter((loan) => matchesLoanSearch(loan, q));
    const counts: Record<LoanListStatusFilter, number> = {
      ALL: searched.length,
      DRAFT: 0,
      SUBMITTED: 0,
      UNDER_REVIEW: 0,
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
        this.allLoans.set(page.content);
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

  cardLink(loan: LoanResponseDto): string[] {
    if (loan.status === 'DRAFT') {
      return ['/nouvelle-demande', String(loan.id)];
    }
    return ['/mes-demandes', String(loan.id)];
  }

  deleteDraft(loan: LoanResponseDto, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.confirmDialog
      .open({
        title: 'Supprimer ce brouillon ?',
        message: `« ${loan.title} » sera définitivement supprimé.\n\nCette action est irréversible.`,
        confirmLabel: 'Supprimer',
        confirmColor: 'warn',
      })
      .pipe(filter((ok) => ok))
      .subscribe(() => {
        this.loanApi.deleteApplication(loan.id).subscribe({
          next: () => {
            this.allLoans.update((list) => list.filter((l) => l.id !== loan.id));
            this.snackBar.open('Brouillon supprimé.', 'OK', { duration: 3000 });
          },
          error: (err) => {
            this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
          },
        });
      });
  }
}
