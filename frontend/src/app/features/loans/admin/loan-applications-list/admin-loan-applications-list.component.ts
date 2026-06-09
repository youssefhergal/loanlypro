import { CurrencyPipe } from '@angular/common';

import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';

import { ActivatedRoute, ParamMap, Router } from '@angular/router';

import { Subject, Subscription } from 'rxjs';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { MatButtonModule } from '@angular/material/button';

import { MatFormFieldModule } from '@angular/material/form-field';

import { MatIconModule } from '@angular/material/icon';

import { MatInputModule } from '@angular/material/input';

import { MatMenuModule } from '@angular/material/menu';

import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MatSelectModule } from '@angular/material/select';

import { MatSnackBar } from '@angular/material/snack-bar';

import { MatTableModule } from '@angular/material/table';

import { MatTooltipModule } from '@angular/material/tooltip';

import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { AdminApiService } from '../../../../core/admin/services/admin-api.service';

import { LoanApplicationStatus } from '../../../../core/loans/models/loan.enums';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';

import {
  AdminLoanListSortApi,
  AdminLoanListSummary,
} from '../../../../core/loans/models/admin-loan-list-summary.model';

import {

  ADVISOR_LOAN_LIST_STATUS_TABS,

  LoanListSort,

  LoanListStatusFilter,

  advisorInitials,

  formatAdminListSubmittedAt,

  formatAdminListUpdatedAt,

  loanCardStatusStyle,

} from '../../../../core/loans/utils/loan-list.util';

import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';
import { ConfirmDialogService } from '../../../../shared/confirm-dialog/confirm-dialog.service';



type AdminListMode = 'STATUS' | 'UNASSIGNED';



const DEFAULT_PAGE_SIZE = 10;

const PAGE_SIZE_OPTIONS = [10, 25, 50] as const;



function toAdminApiSort(sort: LoanListSort): AdminLoanListSortApi {

  const map: Record<LoanListSort, AdminLoanListSortApi> = {

    updatedDesc: 'UPDATED_DESC',

    updatedAsc: 'UPDATED_ASC',

    amountDesc: 'AMOUNT_DESC',

    amountAsc: 'AMOUNT_ASC',

  };

  return map[sort];

}



@Component({

  selector: 'app-admin-loan-applications-list',

  standalone: true,

  imports: [

    CurrencyPipe,

    MatButtonModule,

    MatFormFieldModule,

    MatIconModule,

    MatInputModule,

    MatMenuModule,

    MatPaginatorModule,

    MatProgressSpinnerModule,

    MatSelectModule,

    MatTableModule,

    MatTooltipModule,

  ],

  templateUrl: './admin-loan-applications-list.component.html',

  styleUrl: './admin-loan-applications-list.component.scss',

})

export class AdminLoanApplicationsListComponent implements OnInit, OnDestroy {

  private readonly loanApi = inject(LoanApiService);
  private readonly adminApi = inject(AdminApiService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  private readonly router = inject(Router);

  private readonly route = inject(ActivatedRoute);

  private readonly snackBar = inject(MatSnackBar);

  private queryParamsSub?: Subscription;

  private searchDebounceSub?: Subscription;

  private readonly searchDebounce$ = new Subject<string>();

  private syncingFromUrl = false;



  readonly statusTabs = ADVISOR_LOAN_LIST_STATUS_TABS;

  readonly statusStyle = loanCardStatusStyle;

  readonly advisorInitials = advisorInitials;

  readonly formatSubmittedAt = formatAdminListSubmittedAt;

  readonly formatUpdatedAt = formatAdminListUpdatedAt;



  readonly displayedColumns = [

    'reference',

    'client',

    'advisor',

    'title',

    'requestedAmount',

    'status',

    'submittedAt',

    'updatedAt',

  ];



  readonly loans = signal<LoanResponseDto[]>([]);

  readonly totalElements = signal(0);

  readonly loading = signal(false);
  readonly assigning = signal(false);

  readonly searchQuery = signal('');

  readonly listMode = signal<AdminListMode>('STATUS');

  readonly statusFilter = signal<LoanListStatusFilter>('ALL');

  readonly advisorFilter = signal<'ALL' | number>('ALL');

  readonly sortBy = signal<LoanListSort>('updatedDesc');

  readonly pageIndex = signal(0);

  readonly pageSize = signal(DEFAULT_PAGE_SIZE);

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;



  private readonly summary = signal<AdminLoanListSummary | null>(null);

  readonly advisorOptions = computed(() => this.summary()?.advisors ?? []);



  readonly pageRangeLabel = computed(() => {

    const total = this.totalElements();

    if (!total) return 'Aucune demande';

    const items = this.loans().length;

    const start = this.pageIndex() * this.pageSize() + 1;

    const end = start + items - 1;

    return `Affichage de ${start} à ${end} sur ${total} demande${total > 1 ? 's' : ''}`;

  });



  readonly unassignedCount = computed(() => this.summary()?.unassignedCount ?? 0);



  readonly sortLabel = computed(() => {

    switch (this.sortBy()) {

      case 'updatedAsc':

        return 'Date MAJ (ancien)';

      case 'amountDesc':

        return 'Montant décroissant';

      case 'amountAsc':

        return 'Montant croissant';

      default:

        return 'Date MAJ (récent)';

    }

  });



  ngOnInit(): void {

    this.applyQueryParams(this.route.snapshot.queryParamMap);

    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {

      if (this.syncingFromUrl) return;

      this.applyQueryParams(params);

      this.load();

    });

    this.searchDebounceSub = this.searchDebounce$

      .pipe(debounceTime(300), distinctUntilChanged())

      .subscribe(() => {

        this.pageIndex.set(0);

        this.syncPaginationToUrl();

        this.load();

        this.loadSummary();

      });

    if (

      !this.route.snapshot.queryParamMap.has('page') ||

      !this.route.snapshot.queryParamMap.has('size')

    ) {

      this.syncPaginationToUrl();

    }

    this.load();

    this.loadSummary();

  }



  ngOnDestroy(): void {

    this.queryParamsSub?.unsubscribe();

    this.searchDebounceSub?.unsubscribe();

    this.searchDebounce$.complete();

  }



  private applyQueryParams(params: ParamMap): void {

    const rawPage = Number(params.get('page') ?? '1');

    const page = Number.isFinite(rawPage) && rawPage >= 1 ? Math.floor(rawPage) : 1;



    const rawSize = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));

    const size = PAGE_SIZE_OPTIONS.includes(rawSize as (typeof PAGE_SIZE_OPTIONS)[number])

      ? rawSize

      : DEFAULT_PAGE_SIZE;



    this.pageIndex.set(page - 1);

    this.pageSize.set(size);

  }



  private syncPaginationToUrl(): void {

    this.syncingFromUrl = true;

    this.router

      .navigate([], {

        relativeTo: this.route,

        queryParams: {

          page: this.pageIndex() + 1,

          size: this.pageSize(),

        },

        replaceUrl: true,

      })

      .finally(() => {

        this.syncingFromUrl = false;

      });

  }



  private resetPageAndSyncUrl(): void {

    this.pageIndex.set(0);

    this.syncPaginationToUrl();

  }



  load(): void {
    const unassignedOnly = this.listMode() === 'UNASSIGNED';
    const advisor = this.advisorFilter();
    const statusFilter = this.statusFilter();
    let status: LoanApplicationStatus | undefined;
    if (this.listMode() === 'STATUS' && statusFilter !== 'ALL') {
      status = statusFilter;
    }

    this.loading.set(true);

    this.loanApi

      .listAdmin({

        page: this.pageIndex(),

        size: this.pageSize(),

        search: this.searchQuery().trim() || undefined,

        advisorId: unassignedOnly || advisor === 'ALL' ? undefined : advisor,

        unassignedOnly,

        status,

        sort: toAdminApiSort(this.sortBy()),

      })

      .subscribe({

        next: (page) => {

          this.loans.set(page.content);

          this.totalElements.set(page.totalElements);

          this.loading.set(false);

        },

        error: (err) => {

          this.loading.set(false);

          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });

        },

      });

  }



  loadSummary(): void {

    this.loanApi

      .getAdminSummary({ search: this.searchQuery().trim() || undefined })

      .subscribe({

        next: (summary) => this.summary.set(summary),

        error: (err) => {

          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });

        },

      });

  }



  refresh(): void {
    this.load();
    this.loadSummary();
  }

  runAdvisorAssignment(): void {
    this.confirmDialog
      .open({
        title: 'Affecter les dossiers non assignés',
        message:
          'Répartir les demandes soumises sans conseiller selon la charge de travail de chaque conseiller ?',
        confirmLabel: 'Lancer l\'affectation',
        cancelLabel: 'Annuler',
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.assigning.set(true);
        this.adminApi.runAdvisorAssignment().subscribe({
          next: (result) => {
            this.assigning.set(false);
            this.snackBar.open(result.message, 'Fermer', { duration: 6000 });
            this.refresh();
          },
          error: (err) => {
            this.assigning.set(false);
            this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
          },
        });
      });
  }



  onSearchInput(value: string): void {

    this.searchQuery.set(value);

    this.searchDebounce$.next(value);

  }



  selectStatus(filter: LoanListStatusFilter): void {

    this.listMode.set('STATUS');

    this.statusFilter.set(filter);

    this.resetPageAndSyncUrl();

    this.load();

  }



  selectUnassigned(): void {
    this.listMode.set('UNASSIGNED');
    this.advisorFilter.set('ALL');
    this.resetPageAndSyncUrl();
    this.load();
  }



  onAdvisorFilterChange(value: string): void {

    this.advisorFilter.set(value === 'ALL' ? 'ALL' : Number(value));

    this.resetPageAndSyncUrl();

    this.load();

  }



  setSort(sort: LoanListSort): void {

    this.sortBy.set(sort);

    this.resetPageAndSyncUrl();

    this.load();

  }



  onPage(event: PageEvent): void {

    this.pageIndex.set(event.pageIndex);

    this.pageSize.set(event.pageSize);

    this.syncPaginationToUrl();

    this.load();

  }



  tabCount(key: LoanListStatusFilter): number {

    const data = this.summary();

    if (!data) return 0;

    if (key === 'ALL') return data.totalCount;

    return data.statusCounts[key] ?? 0;

  }



  isStatusTabActive(key: LoanListStatusFilter): boolean {

    return this.listMode() === 'STATUS' && this.statusFilter() === key;

  }



  open(loan: LoanResponseDto): void {

    this.router.navigate(['/admin/demandes', loan.id]);

  }

}


