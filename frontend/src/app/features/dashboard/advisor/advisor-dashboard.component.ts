import { CurrencyPipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  DashboardApiService,
  LoanApplicationSummaryDto,
} from '../../../core/dashboard/services/dashboard-api.service';
import {
  advisorApplicationCtaLabel,
  advisorApplicationHint,
  advisorApplicationHintTone,
  advisorApplicationRouterLink,
  applicationProgressSteps,
  applicationStatusClass,
  applicationStatusIcon,
  applicationStatusLabel,
  buildAdvisorHeroSlides,
  buildAdvisorPriorityAlerts,
  countActionRequiredApplications,
  countActiveLoans,
  displayApplicantName,
  formatNextInstallment,
  isEmptyAdvisorProfile,
  pickPriorityApplications,
  recentLoans,
  showApplicationStepper,
} from '../../../core/dashboard/utils/advisor-dashboard-display.util';
import { MessagingApiService } from '../../../core/messaging/services/messaging-api.service';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';
import { LoanStatusChipComponent } from '../../loans/repayment/shared/loan-status-chip/loan-status-chip.component';
import { DashboardHeroCarouselComponent } from '../client/hero-carousel/dashboard-hero-carousel.component';

@Component({
  selector: 'app-advisor-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    NgClass,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    LoanStatusChipComponent,
    DashboardHeroCarouselComponent,
  ],
  templateUrl: './advisor-dashboard.component.html',
  styleUrl: './advisor-dashboard.component.scss',
})
export class AdvisorDashboardComponent implements OnInit {
  private readonly api = inject(DashboardApiService);
  private readonly messagingApi = inject(MessagingApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly applications = signal<LoanApplicationSummaryDto[]>([]);
  readonly applicationsTotal = signal(0);
  readonly unreadMessages = signal(0);
  readonly heroSlideIndex = signal(0);

  readonly helpers = {
    applicationProgressSteps,
    applicationStatusClass,
    applicationStatusIcon,
    applicationStatusLabel,
    showApplicationStepper,
    advisorApplicationHint,
    advisorApplicationHintTone,
    advisorApplicationRouterLink,
    advisorApplicationCtaLabel,
    displayApplicantName,
    formatNextInstallment,
  };

  readonly isEmpty = computed(() => isEmptyAdvisorProfile(this.applications(), this.loans()));
  readonly priorityAlerts = computed(() => buildAdvisorPriorityAlerts(this.applications()));
  readonly heroSlides = computed(() => buildAdvisorHeroSlides(this.applications(), this.loans()));
  readonly priorityApplications = computed(() => pickPriorityApplications(this.applications()));
  readonly trackedLoans = computed(() => recentLoans(this.loans()));
  readonly actionRequiredCount = computed(() => countActionRequiredApplications(this.applications()));
  readonly activeLoansCount = computed(() => countActiveLoans(this.loans()));

  readonly priorityBannerText = computed(() => {
    const count = this.priorityAlerts().length;
    if (count === 0) {
      return null;
    }
    return `${count} dossier${count > 1 ? 's' : ''} nécessite${count > 1 ? 'nt' : ''} votre attention`;
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      dashboard: this.api.getAdvisorDashboard(),
      unread: this.messagingApi.getUnreadCount().pipe(catchError(() => of({ count: 0 }))),
    }).subscribe({
      next: ({ dashboard, unread }) => {
        this.loans.set(dashboard.loans ?? []);
        this.applications.set(dashboard.applications ?? []);
        this.applicationsTotal.set(
          dashboard.applicationsCount ?? dashboard.applications?.length ?? 0,
        );
        this.unreadMessages.set(unread.count ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le tableau de bord conseiller.');
        this.loading.set(false);
      },
    });
  }

  onHeroSlideChange(index: number): void {
    this.heroSlideIndex.set(index);
  }
}
