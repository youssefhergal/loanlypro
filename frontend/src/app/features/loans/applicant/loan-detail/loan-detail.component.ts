import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { RepaymentApiService } from '../../../../core/loans/repayment/services/repayment-api.service';
import { LoanSummaryDto } from '../../../../core/loans/repayment/models/loan-summary.model';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import { LoanDocumentResponseDto } from '../../../../core/loans/models/loan-document.model';
import { LoanDocumentReviewResponseDto } from '../../../../core/loans/models/loan-document-review.model';
import { LoanHistoryEventResponseDto } from '../../../../core/loans/models/loan-history.model';
import {
  EMPLOYER_SECTOR_OPTIONS,
  EMPLOYMENT_OPTIONS,
  LOAN_DEBT_RATIO_DISPLAY_MAX,
} from '../../../../core/loans/constants/loan.constants';
import {
  calculateDebtRatio,
  calculateLoanSimulation,
  calculateMonthlyPayment,
} from '../../../../core/loans/utils/loan-calculator';
import { LoanDocumentType } from '../../../../core/loans/models/loan.enums';
import {
  LoanDetailDocumentRow,
  advisorReviewStatusIcon,
  advisorReviewStatusLabel,
  canClientCancelLoan,
  countDocumentsNeedingClientAction,
  docDisplayName,
  formatDocSize,
  isAdvisorDocumentReviewVisible,
  historyEventsToTimeline,
  isDocumentComplementAllowed,
  loanDetailDocuments,
  loanDetailProgress,
  loanDetailTimeline,
  loanStatusDisplayLabel,
} from '../../../../core/loans/utils/loan-detail.util';
import { ConfirmDialogService } from '../../../../shared/confirm-dialog/confirm-dialog.service';
import { AcceptCounterOfferDialogService } from '../../../../shared/accept-counter-offer-dialog/accept-counter-offer-dialog.service';
import { RejectCounterOfferDialogService } from '../../../../shared/reject-counter-offer-dialog/reject-counter-offer-dialog.service';
import { filter } from 'rxjs';
import {
  formatDateFrLong,
  loanCardStatusStyle,
  loanPurposeLabel,
} from '../../../../core/loans/utils/loan-list.util';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-loan-detail',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './loan-detail.component.html',
  styleUrl: './loan-detail.component.scss',
})
export class LoanDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly loanApi = inject(LoanApiService);
  private readonly repaymentApi = inject(RepaymentApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly acceptCounterOfferDialog = inject(AcceptCounterOfferDialogService);
  private readonly rejectCounterOfferDialog = inject(RejectCounterOfferDialogService);
  readonly auth = inject(AuthService);
  readonly debtRatioMaxPercent = LOAN_DEBT_RATIO_DISPLAY_MAX * 100;

  readonly loan = signal<LoanResponseDto | null>(null);
  readonly documents = signal<LoanDocumentResponseDto[]>([]);
  readonly documentReviews = signal<LoanDocumentReviewResponseDto[]>([]);
  readonly historyEvents = signal<LoanHistoryEventResponseDto[]>([]);
  readonly historyStatus = signal<'pending' | 'loaded' | 'failed'>('pending');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly complementUploading = signal<LoanDocumentType | null>(null);
  readonly offerActionLoading = signal(false);
  readonly linkedRepaymentLoan = signal<LoanSummaryDto | null>(null);

  readonly showMandateSetupAlert = computed(() => {
    const l = this.loan();
    if (!l || l.status !== 'APPROVED') {
      return false;
    }
    const repayment = this.linkedRepaymentLoan();
    if (!repayment) {
      return true;
    }
    return (
      repayment.status === 'PENDING_MANDATE' ||
      repayment.mandateStatus === 'NONE' ||
      repayment.mandateStatus === 'REVOKED'
    );
  });

  readonly mandateSetupRoute = computed(() => {
    const repayment = this.linkedRepaymentLoan();
    return repayment ? ['/mes-prets', repayment.id, 'mandat'] : ['/mes-prets'];
  });

  readonly statusStyle = computed(() => {
    const l = this.loan();
    return l ? loanCardStatusStyle(l.status) : null;
  });

  readonly progress = computed(() => {
    const l = this.loan();
    return l ? loanDetailProgress(l) : null;
  });

  readonly documentRows = computed(() => {
    const l = this.loan();
    if (!l) return [];
    return loanDetailDocuments(l, this.documents(), this.documentReviews(), this.historyEvents());
  });

  readonly showAdvisorDocumentReview = computed(() => {
    const l = this.loan();
    return l ? isAdvisorDocumentReviewVisible(l.status) : false;
  });

  readonly documentsNeedingAction = computed(() =>
    countDocumentsNeedingClientAction(this.documentRows())
  );

  readonly requiredProvided = computed(
    () => this.documentRows().filter((r) => r.required && r.status === 'provided').length
  );

  readonly requiredCount = computed(
    () => this.documentRows().filter((r) => r.required).length
  );

  readonly missingRequired = computed(
    () => this.documentRows().filter((r) => r.required && r.status === 'missing').length
  );

  readonly canUploadComplement = computed(() => {
    const l = this.loan();
    return l ? isDocumentComplementAllowed(l.status) : false;
  });

  readonly showComplementAlert = computed(() => {
    if (!this.canUploadComplement()) {
      return false;
    }
    return this.documentsNeedingAction() > 0;
  });

  readonly offerPending = computed(() => this.loan()?.status === 'OFFER_PENDING');

  readonly counterOfferAmount = computed(() => {
    const l = this.loan();
    return l?.approvedAmount != null ? Number(l.approvedAmount) : null;
  });

  readonly counterOfferDurationMonths = computed(() => this.loan()?.approvedDurationMonths ?? null);

  readonly counterOfferRate = computed(() => {
    const l = this.loan();
    return l?.interestRate != null ? Number(l.interestRate) : null;
  });

  readonly counterOfferMonthlyPayment = computed(() => {
    const amount = this.counterOfferAmount();
    const duration = this.counterOfferDurationMonths();
    const rate = this.counterOfferRate();
    if (amount == null || duration == null) return 0;
    return calculateMonthlyPayment(amount, duration, rate != null ? rate / 100 : undefined);
  });

  readonly canCancelLoan = computed(() => {
    const l = this.loan();
    return l ? canClientCancelLoan(l.status) : false;
  });

  readonly timeline = computed(() => {
    const l = this.loan();
    if (!l || this.historyStatus() === 'failed') return [];
    if (this.historyStatus() === 'pending') return [];
    const fromApi = this.historyEvents();
    if (fromApi.length > 0) {
      return historyEventsToTimeline(fromApi);
    }
    return loanDetailTimeline(l, this.requiredProvided(), this.requiredCount());
  });

  readonly historyUnavailable = computed(() => this.historyStatus() === 'failed');

  readonly monthlyPayment = computed(() => {
    const l = this.loan();
    if (!l) return 0;
    const rate = l.interestRate != null ? Number(l.interestRate) / 100 : undefined;
    return calculateMonthlyPayment(
      Number(l.requestedAmount),
      l.requestedDurationMonths,
      rate
    );
  });

  readonly totalCost = computed(() => {
    const l = this.loan();
    if (!l) return 0;
    return calculateLoanSimulation(Number(l.requestedAmount), l.requestedDurationMonths)
      .totalCost;
  });

  readonly debtRatioPercent = computed(() => {
    const l = this.loan();
    if (!l) return 0;
    const charges =
      Number(l.monthlyRent ?? 0) +
      Number(l.monthlyLoanPayments ?? 0) +
      Number(l.monthlyAlimony ?? 0) +
      Number(l.monthlyOtherCharges ?? 0);
    const income = Number(l.monthlyIncome) + Number(l.additionalIncome ?? 0);
    return calculateDebtRatio(charges, this.monthlyPayment(), income) * 100;
  });

  readonly employmentLabel = computed(() => {
    const l = this.loan();
    if (!l) return '—';
    const emp = EMPLOYMENT_OPTIONS.find((e) => e.value === l.employmentStatus)?.label ?? l.employmentStatus;
    const job = l.jobTitle?.trim();
    if (job) return `${emp} – ${job}`;
    return emp;
  });

  readonly sectorLabel = computed(() => {
    const l = this.loan();
    if (!l?.employerSector) return null;
    return EMPLOYER_SECTOR_OPTIONS.find((s) => s.value === l.employerSector)?.label ?? l.employerSector;
  });

  readonly hireDateLabel = computed(() => {
    const d = this.loan()?.hireDate;
    if (!d) return null;
    return formatDateFrLong(d);
  });

  readonly purposeLabel = computed(() => {
    const l = this.loan();
    return l ? loanPurposeLabel(l.loanPurpose) : '—';
  });

  readonly applicantName = computed(() => {
    const l = this.loan();
    if (l?.applicantName?.trim()) return l.applicantName;
    const u = this.auth.currentUser();
    if (!u) return '—';
    return `${u.firstName} ${u.lastName}`.trim();
  });

  readonly durationYears = computed(() => {
    const m = this.loan()?.requestedDurationMonths ?? 0;
    if (m % 12 === 0 && m > 0) return `${m / 12} an${m / 12 > 1 ? 's' : ''}`;
    return `${m} mois`;
  });

  readonly proposedRate = computed(() => {
    const l = this.loan();
    if (l?.interestRate != null) return Number(l.interestRate);
    return 3.85;
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Dossier introuvable.');
      this.loading.set(false);
      return;
    }

    forkJoin({
      loan: this.loanApi.getById(id),
      documents: this.loanApi.getDocuments(id),
      documentReviews: this.loanApi.getDocumentReviews(id),
    }).subscribe({
      next: ({ loan, documents, documentReviews }) => {
        if (loan.status === 'DRAFT') {
          this.router.navigate(['/nouvelle-demande', loan.id]);
          return;
        }
        this.loan.set(loan);
        this.documents.set(documents);
        this.documentReviews.set(documentReviews);
        this.loading.set(false);
        this.fetchHistory(id);
        if (loan.status === 'APPROVED') {
          this.loadLinkedRepaymentLoan(loan.id);
        }
      },
      error: (err) => {
        this.error.set(getErrorMessage(err, 'Impossible de charger le dossier.'));
        this.loading.set(false);
      },
    });
  }

  private fetchHistory(loanId: number): void {
    this.historyStatus.set('pending');
    this.loanApi.getHistory(loanId).subscribe({
      next: (history) => {
        this.historyEvents.set(history);
        this.historyStatus.set('loaded');
      },
      error: () => {
        this.historyEvents.set([]);
        this.historyStatus.set('failed');
      },
    });
  }

  private loadLinkedRepaymentLoan(applicationId: number): void {
    this.repaymentApi.getMyLoans().subscribe({
      next: (loans) => {
        const match = loans.find((loan) => loan.loanApplicationId === applicationId) ?? null;
        this.linkedRepaymentLoan.set(match);
      },
      error: () => {
        this.linkedRepaymentLoan.set(null);
      },
    });
  }

  statusLabel(): string {
    const l = this.loan();
    return l ? loanStatusDisplayLabel(l.status) : '';
  }

  formatDate(iso: string | null | undefined): string {
    return formatDateFrLong(iso) || '—';
  }

  docName = docDisplayName;
  docSize = formatDocSize;

  readonly advisorReviewLabel = advisorReviewStatusLabel;
  readonly advisorReviewIcon = advisorReviewStatusIcon;

  canReplaceDocument(row: LoanDetailDocumentRow): boolean {
    if (!this.canUploadComplement()) {
      return false;
    }
    const s = row.advisorReview?.status;
    return s === 'rejected' || s === 'missing_upload';
  }

  replaceDocumentLabel(row: LoanDetailDocumentRow): string {
    return row.advisorReview?.status === 'missing_upload' ? 'Ajouter' : 'Remplacer';
  }

  displayFileName(row: LoanDetailDocumentRow): string | null {
    if (row.files.length > 0) {
      return this.docName(row.files[0]);
    }
    return null;
  }

  onComplementDocumentSelected(row: LoanDetailDocumentRow, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    const allowed = ['application/pdf', 'image/jpeg', 'image/png'];
    if (!allowed.includes(file.type)) {
      this.snackBar.open('Format accepté : PDF, JPG ou PNG.', 'Fermer', { duration: 4000 });
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.snackBar.open('Fichier trop volumineux (max. 10 Mo).', 'Fermer', { duration: 4000 });
      return;
    }

    const loan = this.loan();
    if (!loan) return;
    if (!isDocumentComplementAllowed(loan.status)) {
      this.snackBar.open(
        'Les compléments documentaires ne sont possibles que lorsque votre dossier est en analyse.',
        'Fermer',
        { duration: 5000 }
      );
      return;
    }

    const loanId = loan.id;
    this.complementUploading.set(row.type);
    this.loanApi.uploadComplementDocument(loanId, row.type, file).subscribe({
      next: () => {
        this.complementUploading.set(null);
        this.reloadDocumentsAndHistory(loanId);
        this.snackBar.open(
          'Document envoyé. En attente de validation par votre conseiller.',
          'OK',
          { duration: 5000 }
        );
      },
      error: (err) => {
        this.complementUploading.set(null);
        this.snackBar.open(getErrorMessage(err, 'Envoi impossible.'), 'Fermer', { duration: 5000 });
      },
    });
  }

  private reloadDocumentsAndHistory(loanId: number): void {
    forkJoin({
      documents: this.loanApi.getDocuments(loanId),
      documentReviews: this.loanApi.getDocumentReviews(loanId),
    }).subscribe({
      next: ({ documents, documentReviews }) => {
        this.documents.set(documents);
        this.documentReviews.set(documentReviews);
        this.fetchHistory(loanId);
      },
      error: (err) => {
        this.snackBar.open(getErrorMessage(err, 'Actualisation impossible.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  acceptCounterOffer(): void {
    const loan = this.loan();
    const amount = this.counterOfferAmount();
    const durationMonths = this.counterOfferDurationMonths();
    if (
      !loan ||
      !this.offerPending() ||
      this.offerActionLoading() ||
      amount == null ||
      durationMonths == null
    ) {
      return;
    }

    this.acceptCounterOfferDialog
      .open({
        amount,
        durationMonths,
        rate: this.counterOfferRate(),
        monthlyPayment: this.counterOfferMonthlyPayment(),
        offerMessage: loan.offerMessage,
      })
      .pipe(filter((ok) => ok))
      .subscribe(() => {
        this.offerActionLoading.set(true);
        this.loanApi.acceptOffer(loan.id).subscribe({
          next: (updated) => {
            this.loan.set(updated);
            this.reloadDocumentsAndHistory(loan.id);
            this.offerActionLoading.set(false);
            this.snackBar.open('Contre-offre acceptée. Votre dossier reprend son analyse.', 'OK', {
              duration: 5000,
            });
          },
          error: (err) => {
            this.offerActionLoading.set(false);
            this.snackBar.open(getErrorMessage(err, 'Acceptation impossible.'), 'Fermer', {
              duration: 5000,
            });
          },
        });
      });
  }

  rejectCounterOffer(): void {
    const loan = this.loan();
    const amount = this.counterOfferAmount();
    const durationMonths = this.counterOfferDurationMonths();
    if (
      !loan ||
      !this.offerPending() ||
      this.offerActionLoading() ||
      amount == null ||
      durationMonths == null
    ) {
      return;
    }

    this.rejectCounterOfferDialog
      .open({
        amount,
        durationMonths,
        rate: this.counterOfferRate(),
        monthlyPayment: this.counterOfferMonthlyPayment(),
        offerMessage: loan.offerMessage,
      })
      .pipe(filter((comment): comment is string => comment !== null))
      .subscribe((comment) => {
        this.offerActionLoading.set(true);
        this.loanApi.rejectOffer(loan.id, comment || undefined).subscribe({
          next: (updated) => {
            this.loan.set(updated);
            this.reloadDocumentsAndHistory(loan.id);
            this.offerActionLoading.set(false);
            this.snackBar.open('Contre-offre refusée. Votre conseiller a été informé.', 'OK', {
              duration: 5000,
            });
          },
          error: (err) => {
            this.offerActionLoading.set(false);
            this.snackBar.open(getErrorMessage(err, 'Refus impossible.'), 'Fermer', {
              duration: 5000,
            });
          },
        });
      });
  }

  cancelLoan(): void {
    const loan = this.loan();
    if (!loan || !this.canCancelLoan()) {
      return;
    }

    this.confirmDialog
      .open({
        title: 'Annuler cette demande ?',
        message:
          'Votre dossier ne sera plus étudié. Cette action est définitive.\n\n' +
          'Vous pourrez créer une nouvelle demande plus tard si besoin.',
        confirmLabel: 'Annuler la demande',
        confirmColor: 'warn',
        cancelLabel: 'Conserver',
      })
      .pipe(filter((ok) => ok))
      .subscribe(() => {
        this.loanApi.cancel(loan.id).subscribe({
          next: () => {
            this.snackBar.open('Votre demande a été annulée.', 'OK', { duration: 4000 });
            this.router.navigate(['/mes-demandes']);
          },
          error: (err) => {
            this.snackBar.open(getErrorMessage(err, 'Annulation impossible.'), 'Fermer', {
              duration: 5000,
            });
          },
        });
      });
  }

  downloadRecap(): void {
    this.snackBar.open('Téléchargement du récapitulatif — bientôt disponible.', 'OK', {
      duration: 3000,
    });
  }

  downloadDocument(doc: LoanDocumentResponseDto): void {
    const loanId = this.loan()?.id;
    if (!loanId) return;
    this.loanApi.downloadDocument(loanId, doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.displayName?.trim() || doc.originalFileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }
}
