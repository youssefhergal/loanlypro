import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LoanApiService } from '../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../core/loans/models/loan-response.model';
import { LoanApplicationStatus } from '../../../core/loans/models/loan.enums';
import { STATUS_LABELS } from '../../../core/loans/constants/loan.constants';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';

type StatusFilter = 'ALL' | LoanApplicationStatus;

@Component({
  selector: 'app-loan-list',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatPaginatorModule,
    MatTabsModule,
    NgClass,
  ],
  templateUrl: './loan-list.component.html',
  styleUrl: './loan-list.component.scss',
})
export class LoanListComponent implements OnInit {
  private readonly loanApi = inject(LoanApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  readonly statusLabels = STATUS_LABELS;
  readonly displayedColumns = ['reference', 'title', 'amount', 'status', 'createdAt', 'actions'];

  readonly loans = signal<LoanResponseDto[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly statusFilter = signal<StatusFilter>('ALL');

  ngOnInit(): void {
    const submitted = this.route.snapshot.queryParamMap.get('submitted');
    if (submitted) {
      this.snackBar.open(`Demande ${submitted} soumise avec succès.`, 'OK', { duration: 5000 });
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const status = this.statusFilter();
    this.loanApi
      .list({
        page: this.pageIndex(),
        size: this.pageSize(),
        status: status === 'ALL' ? undefined : status,
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

  onTabChange(index: number): void {
    const filters: StatusFilter[] = ['ALL', 'DRAFT', 'SUBMITTED', 'APPROVED'];
    this.onFilterChange(filters[index] ?? 'ALL');
  }

  onFilterChange(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  statusLabel(status: LoanApplicationStatus): string {
    return STATUS_LABELS[status]?.label ?? status;
  }

  statusClass(status: LoanApplicationStatus): string {
    return STATUS_LABELS[status]?.chipClass ?? '';
  }
}
