import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  DashboardApiService,
  LoanApplicationSummaryDto,
  PaymentTransactionDto,
} from '../../../core/dashboard/services/dashboard-api.service';
import {
  applicationProgressSteps,
  applicationRouterLink,
  applicationStatusClass,
  applicationStatusIcon,
  buildClosedLoanHeroCarouselSlides,
  buildHeroCarouselSlides,
  computeClosedLoanRecap,
  computeRepaymentProgress,
  countArchivedApplications,
  countClosedLoans,
  countDocumentsToProcess,
  countInProgressApplications,
  dashboardApplicationStatusLabel,
  daysUntilDate,
  dossierVerticalTimelineSteps,
  findDocumentAlert,
  findPaymentFailureAlert,
  isActiveBorrowerProfile,
  isClosedLoanProfile,
  isDossierEnCoursProfile,
  isNewClientProfile,
  NEW_CLIENT_ONBOARDING_STEPS,
  pickPrimaryActiveLoan,
  pickPrimaryClosedLoan,
  pickPrimaryTrackedDemande,
  recentActionDocuments,
  showApplicationStepper,
} from '../../../core/dashboard/utils/client-dashboard-display.util';
import { JustificatifGroupDto } from '../../../core/documents/models/justificatif.model';
import {
  documentTypeIcon,
  validationStatusClass,
  validationStatusLabel,
} from '../../../core/documents/utils/justificatif-display.util';
import { NotificationDto } from '../../../core/notifications/models/notification.model';
import { NotificationApiService } from '../../../core/notifications/services/notification-api.service';
import {
  formatRelativeNotificationDate,
  getNotificationVisual,
} from '../../../core/notifications/utils/notification-display.util';
import { LoanSummaryDto } from '../../../core/loans/repayment/models/loan-summary.model';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';
import { LoanStatusChipComponent } from '../../loans/repayment/shared/loan-status-chip/loan-status-chip.component';
import { DashboardHeroCarouselComponent } from './hero-carousel/dashboard-hero-carousel.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    NgClass,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    LoanStatusChipComponent,
    DashboardHeroCarouselComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly notificationApi = inject(NotificationApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly heroSlideIndex = signal(0);
  readonly demandes = signal<LoanApplicationSummaryDto[]>([]);
  readonly loans = signal<LoanSummaryDto[]>([]);
  readonly transactions = signal<PaymentTransactionDto[]>([]);
  readonly justificatifGroups = signal<JustificatifGroupDto[]>([]);
  readonly notifications = signal<NotificationDto[]>([]);
  readonly unreadNotificationsCount = signal(0);

  readonly recentDemandes = computed(() => this.demandes().slice(0, 3));
  readonly inProgressCount = computed(() => countInProgressApplications(this.demandes()));
  readonly activeLoan = computed(() => pickPrimaryActiveLoan(this.loans()));
  readonly documentsToProcessCount = computed(() =>
    countDocumentsToProcess(this.justificatifGroups()),
  );
  readonly documentAlert = computed(() => findDocumentAlert(this.justificatifGroups()));
  readonly recentDocuments = computed(() =>
    recentActionDocuments(this.justificatifGroups(), 3),
  );
  readonly recentNotifications = computed(() => this.notifications().slice(0, 3));

  readonly isNewClientProfile = computed(() =>
    isNewClientProfile(this.demandes(), this.loans()),
  );

  readonly isDossierEnCoursProfile = computed(() =>
    isDossierEnCoursProfile(this.demandes(), this.loans()),
  );

  readonly isActiveBorrowerProfile = computed(() =>
    isActiveBorrowerProfile(this.demandes(), this.loans()),
  );

  readonly isClosedLoanProfile = computed(() =>
    isClosedLoanProfile(this.demandes(), this.loans()),
  );

  readonly paymentFailureAlert = computed(() => {
    if (this.isClosedLoanProfile()) {
      return null;
    }
    return findPaymentFailureAlert(this.loans(), this.transactions(), this.notifications());
  });

  readonly primaryTrackedDemande = computed(() =>
    pickPrimaryTrackedDemande(this.demandes()),
  );

  readonly activeRepaymentLoan = computed(() =>
    this.loans().find((loan) => loan.status === 'ACTIVE') ?? null,
  );

  readonly primaryClosedLoan = computed(() => pickPrimaryClosedLoan(this.loans()));

  readonly closedLoansCount = computed(() => countClosedLoans(this.loans()));

  readonly archivedDemandesCount = computed(() =>
    countArchivedApplications(this.demandes()),
  );

  readonly closedLoanRecap = computed(() =>
    computeClosedLoanRecap(this.primaryClosedLoan()),
  );

  readonly newClientOnboardingSteps = NEW_CLIENT_ONBOARDING_STEPS;

  readonly nextPaymentDays = computed(() =>
    daysUntilDate(this.activeRepaymentLoan()?.nextInstallmentDate),
  );

  readonly repaymentProgress = computed(() =>
    computeRepaymentProgress(this.activeRepaymentLoan()),
  );

  readonly heroSlides = computed(() => {
    if (this.isClosedLoanProfile()) {
      return buildClosedLoanHeroCarouselSlides();
    }
    return buildHeroCarouselSlides({
      loan: this.activeRepaymentLoan(),
      primaryDemande: this.primaryTrackedDemande(),
      paymentDays: this.nextPaymentDays(),
      documentAlert: this.documentAlert(),
      progress: this.repaymentProgress(),
      dossierEnCoursMode: this.isDossierEnCoursProfile(),
      activeBorrowerMode: this.isActiveBorrowerProfile(),
    });
  });

  readonly helpers = {
    applicationStatusLabel: dashboardApplicationStatusLabel,
    applicationStatusClass,
    applicationStatusIcon,
    applicationRouterLink,
    applicationProgressSteps,
    dossierVerticalTimelineSteps,
    showApplicationStepper,
    documentTypeIcon,
    validationStatusLabel,
    validationStatusClass,
    formatRelativeNotificationDate,
    getNotificationVisual,
  };

  ngOnInit(): void {
    this.loadDashboard();
  }

  onHeroSlideChange(index: number): void {
    this.heroSlideIndex.set(index);
  }

  openNotification(notification: NotificationDto): void {
    if (!notification.read) {
      this.notificationApi.markAsRead(notification.id).subscribe({
        next: () => {
          this.notifications.update((items) =>
            items.map((item) =>
              item.id === notification.id ? { ...item, read: true } : item,
            ),
          );
          this.unreadNotificationsCount.update((count) => Math.max(0, count - 1));
        },
      });
    }
    if (notification.referenceType === 'LOAN_APPLICATION' && notification.referenceId != null) {
      void this.router.navigate(['/mes-demandes', notification.referenceId]);
    } else {
      void this.router.navigate(['/notifications']);
    }
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.dashboardApi.getMyDashboard().subscribe({
      next: (dashboard) => {
        this.demandes.set(dashboard.demandes ?? []);
        this.loans.set(dashboard.loans ?? []);
        this.transactions.set(dashboard.transactions ?? []);
        this.justificatifGroups.set(dashboard.justificatifs ?? []);
        this.notifications.set(dashboard.notifications ?? []);
        this.unreadNotificationsCount.set(dashboard.unreadNotificationsCount ?? 0);
        this.heroSlideIndex.set(0);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(
          getErrorMessage(err, 'Impossible de charger le tableau de bord.'),
          'Fermer',
          { duration: 5000 },
        );
      },
    });
  }
}
