import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { filter } from 'rxjs/operators';
import { EMPLOYMENT_OPTIONS, LOAN_INTEREST_RATE } from '../../../../core/loans/constants/loan.constants';
import { LoanDocumentResponseDto } from '../../../../core/loans/models/loan-document.model';
import { LoanDocumentType } from '../../../../core/loans/models/loan.enums';
import { LoanDocumentReviewResponseDto } from '../../../../core/loans/models/loan-document-review.model';
import { LoanHistoryEventResponseDto } from '../../../../core/loans/models/loan-history.model';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import {
  calculateDebtRatio,
  calculateMonthlyPayment,
} from '../../../../core/loans/utils/loan-calculator';
import {
  LoanDetailDocumentRow,
  docDisplayName,
  formatDocSize,
  historyEventsToTimeline,
  loanDetailDocuments,
} from '../../../../core/loans/utils/loan-detail.util';
import {
  LoanDocumentAdvisorReviewStatus,
} from '../../../../core/loans/utils/loan-document-advisor-review';
import { loanCardStatusStyle, loanPurposeLabel, formatDateFrLong } from '../../../../core/loans/utils/loan-list.util';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';
import { ConfirmDialogService } from '../../../../shared/confirm-dialog/confirm-dialog.service';
import { RejectDocumentDialogService } from '../../../../shared/reject-document-dialog/reject-document-dialog.service';
import { ValidateDocumentDialogService } from '../../../../shared/validate-document-dialog/validate-document-dialog.service';
import { SendProposalDialogService } from '../../../../shared/send-proposal-dialog/send-proposal-dialog.service';
import { ApproveLoanDialogService } from '../../../../shared/approve-loan-dialog/approve-loan-dialog.service';
import { RejectLoanDialogService } from '../../../../shared/reject-loan-dialog/reject-loan-dialog.service';
import { AdvisorDocumentPreviewPanelComponent } from './advisor-document-preview-panel/advisor-document-preview-panel.component';

type OfferMode = 'system' | 'custom';

@Component({
  selector: 'app-advisor-loan-application-detail',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    CurrencyPipe,
    DecimalPipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    AdvisorDocumentPreviewPanelComponent,
  ],
  templateUrl: './advisor-loan-application-detail.component.html',
  styleUrl: './advisor-loan-application-detail.component.scss',
})
export class AdvisorLoanApplicationDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly loanApi = inject(LoanApiService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly rejectDocumentDialog = inject(RejectDocumentDialogService);
  private readonly validateDocumentDialog = inject(ValidateDocumentDialogService);
  private readonly sendProposalDialog = inject(SendProposalDialogService);
  private readonly approveLoanDialog = inject(ApproveLoanDialogService);
  private readonly rejectLoanDialog = inject(RejectLoanDialogService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /** Réactivité des computed qui lisent offerForm (valueChanges ne déclenche pas les signals seuls). */
  private readonly offerFormRevision = signal(0);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly loan = signal<LoanResponseDto | null>(null);
  readonly documents = signal<LoanDocumentResponseDto[]>([]);
  readonly documentReviews = signal<LoanDocumentReviewResponseDto[]>([]);
  readonly historyEvents = signal<LoanHistoryEventResponseDto[]>([]);
  readonly actionLoading = signal(false);
  readonly offerMode = signal<OfferMode>('system');

  readonly previewOpen = signal(false);
  readonly previewRow = signal<LoanDetailDocumentRow | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly previewZoom = signal(100);
  private previewObjectUrl: string | null = null;

  readonly counterOfferPending = computed(() => this.loan()?.status === 'OFFER_PENDING');

  readonly counterOfferAccepted = computed(() => this.loan()?.offerClientAccepted === true);

  /** Contre-offre (formulaire ou dossier) : approbation réservée après acceptation client. */
  readonly needsClientOfferAcceptance = computed(() => {
    const l = this.loan();
    if (this.counterOfferPending()) return true;
    if (l?.offerClientAccepted === true) return true;
    if (this.isCounterOfferFromLoan(l)) return true;
    return this.offerMode() === 'custom' && this.isCounterOffer();
  });

  readonly canSendProposal = computed(() => {
    this.offerFormRevision();
    const l = this.loan();
    if (!l || this.isReadOnly() || this.actionLoading()) return false;
    if (this.offerMode() !== 'custom') return false;
    if (this.counterOfferPending()) return false;
    if (this.counterOfferAccepted()) return false;
    if (this.offerForm.invalid) return false;
    const c = this.checklist();
    if (!c.allValidated || !c.noRejected) return false;
    return this.isCounterOffer();
  });

  readonly sendProposalBlockedHint = computed(() => {
    if (this.counterOfferAccepted()) return null;
    const checklistHint = this.checklistBlockedHint('d\'envoyer une contre-offre');
    if (checklistHint) return checklistHint;
    if (this.offerMode() === 'custom' && !this.isCounterOffer()) {
      return 'Modifiez au moins le montant, la durée ou le taux par rapport à la demande client pour activer l\'envoi.';
    }
    return null;
  });

  readonly approveBlockedHint = computed(() => {
    const l = this.loan();
    if (!l || this.isReadOnly() || !this.showApproveButton() || this.canApprove()) {
      return null;
    }
    if (l.status !== 'UNDER_REVIEW') {
      return 'Le dossier doit être en analyse pour pouvoir l\'approuver.';
    }
    const checklistHint = this.checklistBlockedHint('d\'approuver le dossier');
    if (checklistHint) return checklistHint;
    if (!this.checklist().offerReady) {
      return 'Complétez les conditions de l\'offre avant d\'approuver.';
    }
    if (this.needsClientOfferAcceptance() && l.offerClientAccepted !== true) {
      return 'En attente de l\'acceptation du client sur la contre-offre.';
    }
    return 'Conditions d\'approbation non remplies.';
  });

  readonly showSendProposalButton = computed(() => {
    const l = this.loan();
    if (!l || this.isReadOnly()) return false;
    if (this.offerMode() !== 'custom') return false;
    return !this.counterOfferPending() && !this.counterOfferAccepted();
  });

  readonly offerFormLocked = computed(
    () => this.counterOfferPending() || this.counterOfferAccepted()
  );

  readonly offerForm = this.fb.group({
    approvedAmount: [0, [Validators.required, Validators.min(1000)]],
    approvedDurationMonths: [12, [Validators.required, Validators.min(12)]],
    interestRate: [3.85, [Validators.required, Validators.min(0)]],
    clientMessage: ['', Validators.maxLength(500)],
  });

  readonly statusStyle = computed(() => {
    const l = this.loan();
    return l ? loanCardStatusStyle(l.status) : null;
  });

  readonly purposeLabel = computed(() => {
    const l = this.loan();
    return l ? `${loanPurposeLabel(l.loanPurpose)}${l.purpose ? ` (${l.purpose})` : ''}` : '—';
  });

  readonly employmentLine = computed(() => {
    const l = this.loan();
    if (!l) return '—';
    const emp =
      EMPLOYMENT_OPTIONS.find((e) => e.value === l.employmentStatus)?.label ??
      l.employmentStatus;
    const parts = [emp];
    if (l.jobTitle?.trim()) parts.push(l.jobTitle.trim());
    parts.push(`${Number(l.monthlyIncome).toLocaleString('fr-FR')} € net / mois`);
    return parts.join(' • ');
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
    const payment = calculateMonthlyPayment(
      Number(l.requestedAmount),
      l.requestedDurationMonths,
      LOAN_INTEREST_RATE
    );
    return Math.round(calculateDebtRatio(charges, payment, income) * 100);
  });

  readonly documentRows = computed((): LoanDetailDocumentRow[] => {
    const l = this.loan();
    if (!l) return [];
    return loanDetailDocuments(l, this.documents(), this.documentReviews(), this.historyEvents()).filter(
      (row) => row.required || row.files.length > 0
    );
  });

  readonly previewableRows = computed(() =>
    this.documentRows().filter((row) => row.files.length > 0)
  );

  readonly previewIndex = computed(() => {
    const row = this.previewRow();
    if (!row) return -1;
    return this.previewableRows().findIndex((item) => item.type === row.type);
  });

  readonly previewPageLabel = computed(() => {
    const total = this.previewableRows().length;
    const index = this.previewIndex();
    if (total <= 0 || index < 0) return 'Document 1 / 1';
    return `Document ${index + 1} / ${total}`;
  });

  readonly previewContentType = computed(() => {
    const file = this.previewRow()?.files[0];
    return file?.contentType ?? '';
  });

  readonly validatedRequiredCount = computed(() => {
    return this.documentRows().filter(
      (row) => row.required && row.advisorReview?.status === 'validated'
    ).length;
  });

  readonly requiredDocumentCount = computed(() => {
    return this.documentRows().filter((row) => row.required).length;
  });

  readonly instructionProgressPercent = computed(() => {
    const total = this.requiredDocumentCount();
    if (!total) return 0;
    return Math.round((this.validatedRequiredCount() / total) * 100);
  });

  readonly systemMonthlyPayment = computed(() => {
    const l = this.loan();
    if (!l) return 0;
    return calculateMonthlyPayment(
      Number(l.requestedAmount),
      l.requestedDurationMonths,
      LOAN_INTEREST_RATE
    );
  });

  readonly systemRatePercent = computed(() => LOAN_INTEREST_RATE * 100);

  readonly proposedMonthlyPayment = computed(() => {
    this.offerFormRevision();
    const v = this.offerForm.getRawValue();
    return calculateMonthlyPayment(
      Number(v.approvedAmount),
      Number(v.approvedDurationMonths),
      Number(v.interestRate) / 100
    );
  });

  readonly requestedMonthlyPayment = computed(() => {
    const l = this.loan();
    if (!l) return 0;
    return calculateMonthlyPayment(Number(l.requestedAmount), l.requestedDurationMonths);
  });

  readonly isCounterOffer = computed(() => {
    this.offerFormRevision();
    if (this.offerMode() === 'system') return false;
    const l = this.loan();
    if (!l) return false;
    const v = this.offerForm.getRawValue();
    return (
      Number(v.approvedAmount) !== Number(l.requestedAmount) ||
      Number(v.approvedDurationMonths) !== Number(l.requestedDurationMonths) ||
      Number(v.interestRate) !== this.systemRatePercent()
    );
  });

  readonly isCounterOfferDisplay = computed(
    () => this.offerMode() === 'custom' && this.isCounterOffer()
  );

  readonly timeline = computed(() => historyEventsToTimeline(this.historyEvents()));

  readonly isReadOnly = computed(() => {
    const s = this.loan()?.status;
    return s === 'APPROVED' || s === 'REJECTED' || s === 'CANCELLED';
  });

  readonly canReviewDocuments = computed(() => this.loan()?.status === 'UNDER_REVIEW');

  readonly checklist = computed(() => {
    this.offerFormRevision();
    const l = this.loan();
    const rows = this.documentRows().filter((r) => r.required);
    const allValidated = rows.every((r) => r.advisorReview?.status === 'validated');
    const noRejected = !rows.some((r) => r.advisorReview?.status === 'rejected');
    const offerReady =
      this.offerMode() === 'system' ||
      (this.offerForm.valid && Number(this.offerForm.get('approvedAmount')?.value) > 0);
    const clientAccepted =
      (this.offerMode() === 'system' && !this.isCounterOffer()) ||
      l?.offerClientAccepted === true;
    return {
      allValidated,
      noRejected,
      offerReady,
      clientAccepted,
    };
  });

  readonly canApprove = computed(() => {
    const l = this.loan();
    if (!l || this.isReadOnly() || this.actionLoading()) return false;
    if (l.status === 'OFFER_PENDING') return false;
    if (l.status !== 'UNDER_REVIEW') return false;
    const c = this.checklist();
    if (!c.allValidated || !c.noRejected || !c.offerReady) return false;

    if (this.needsClientOfferAcceptance()) {
      return l.offerClientAccepted === true && l.status === 'UNDER_REVIEW';
    }

    return this.offerMode() === 'system' && !this.isCounterOffer();
  });

  readonly showApproveButton = computed(() => {
    const l = this.loan();
    if (!l || this.isReadOnly()) return false;
    if (l.status === 'OFFER_PENDING') return false;
    if (l.status !== 'UNDER_REVIEW') return false;

    if (this.needsClientOfferAcceptance()) {
      return l.offerClientAccepted === true;
    }

    return this.offerMode() === 'system' && !this.isCounterOffer();
  });

  readonly showDecisionPanel = computed(() => {
    const s = this.loan()?.status;
    return s === 'UNDER_REVIEW' || s === 'OFFER_PENDING';
  });

  isCounterOfferFromLoan(l: LoanResponseDto | null | undefined): boolean {
    if (!l?.approvedAmount || !l.approvedDurationMonths || l.interestRate == null) return false;
    return (
      Number(l.approvedAmount) !== Number(l.requestedAmount) ||
      l.approvedDurationMonths !== l.requestedDurationMonths ||
      Number(l.interestRate) !== this.systemRatePercent()
    );
  }

  formatDate(iso: string | null | undefined): string {
    return formatDateFrLong(iso) || '—';
  }

  ngOnInit(): void {
    this.offerForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.offerFormRevision.update((n) => n + 1);
    });

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Identifiant invalide.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      loan: this.loanApi.getById(id),
      documents: this.loanApi.getDocuments(id),
      documentReviews: this.loanApi.getDocumentReviews(id),
      history: this.loanApi.getHistory(id),
    }).subscribe({
      next: ({ loan, documents, documentReviews, history }) => {
        this.loan.set(loan);
        this.documents.set(documents);
        this.documentReviews.set(documentReviews);
        this.historyEvents.set(history);
        this.syncOfferForm(loan);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(getErrorMessage(err, 'Erreur de chargement du dossier.'));
        this.loading.set(false);
      },
    });
  }

  private syncOfferForm(loan: LoanResponseDto): void {
    const hasCustom = this.isCounterOfferFromLoan(loan) || loan.status === 'OFFER_PENDING';

    if (hasCustom) {
      this.offerMode.set('custom');
    } else {
      this.offerMode.set('system');
    }

    this.offerForm.patchValue({
      approvedAmount: Number(loan.approvedAmount ?? loan.requestedAmount),
      approvedDurationMonths: loan.approvedDurationMonths ?? loan.requestedDurationMonths,
      interestRate: Number(loan.interestRate ?? this.systemRatePercent()),
      clientMessage: loan.offerMessage ?? '',
    });
    this.offerFormRevision.update((n) => n + 1);
  }

  docFileLabel(row: LoanDetailDocumentRow): string {
    const file = row.files[0];
    if (!file) return '—';
    const name = docDisplayName(file);
    const size = formatDocSize(file.fileSizeBytes);
    const date = file.uploadedAt
      ? new Date(file.uploadedAt).toLocaleDateString('fr-FR')
      : '—';
    return `${name} • ${size} • ${date}`;
  }

  reviewBadgeClass(status: LoanDocumentAdvisorReviewStatus | undefined): string {
    switch (status) {
      case 'validated':
        return 'doc-badge doc-badge--ok';
      case 'rejected':
        return 'doc-badge doc-badge--ko';
      case 'pending_review':
        return 'doc-badge doc-badge--pending';
      default:
        return 'doc-badge';
    }
  }

  reviewDisplayLabel(status: LoanDocumentAdvisorReviewStatus | undefined): string {
    if (status === 'validated') return 'VALIDÉ ✓';
    if (status === 'rejected') return 'REJETÉ';
    if (status === 'pending_review') return 'EN ATTENTE';
    if (status === 'missing_upload') return 'MANQUANT';
    return '—';
  }

  previewPanelStatusLabel(status: LoanDocumentAdvisorReviewStatus | undefined): string {
    if (status === 'pending_review') return 'À VÉRIFIER';
    return this.reviewDisplayLabel(status);
  }

  previewFileMeta(row: LoanDetailDocumentRow): string {
    const file = row.files[0];
    if (!file) return '—';
    return `${docDisplayName(file)} • ${formatDocSize(file.fileSizeBytes)}`;
  }

  previewCanReviewActions(row: LoanDetailDocumentRow | null): boolean {
    return !!row && this.canReviewDocuments() && row.advisorReview?.status === 'pending_review';
  }

  ngOnDestroy(): void {
    this.revokePreviewUrl();
  }

  openDocumentPreview(row: LoanDetailDocumentRow): void {
    if (!this.canReviewDocuments()) {
      this.snackBar.open('Mettez le dossier en analyse pour consulter les pièces.', 'OK', {
        duration: 4000,
      });
      return;
    }
    if (!row.files[0]) return;

    this.previewRow.set(row);
    this.previewOpen.set(true);
    this.previewZoom.set(100);
    this.loadPreviewBlob(row.files[0]);
  }

  closeDocumentPreview(): void {
    this.previewOpen.set(false);
    this.revokePreviewUrl();
    this.previewRow.set(null);
  }

  private revokePreviewUrl(): void {
    if (this.previewObjectUrl) {
      URL.revokeObjectURL(this.previewObjectUrl);
      this.previewObjectUrl = null;
    }
    this.previewUrl.set(null);
  }

  private loadPreviewBlob(file: LoanDocumentResponseDto): void {
    const loanId = this.loan()?.id;
    if (!loanId) return;

    this.revokePreviewUrl();
    this.previewLoading.set(true);
    this.loanApi.downloadDocument(loanId, file.id).subscribe({
      next: (blob) => {
        this.previewObjectUrl = URL.createObjectURL(blob);
        this.previewUrl.set(this.previewObjectUrl);
        this.previewLoading.set(false);
      },
      error: (err) => {
        this.previewLoading.set(false);
        this.snackBar.open(getErrorMessage(err, 'Impossible de charger le document.'), 'Fermer', {
          duration: 5000,
        });
        this.closeDocumentPreview();
      },
    });
  }

  previewZoomIn(): void {
    this.previewZoom.update((value) => Math.min(value + 10, 200));
  }

  previewZoomOut(): void {
    this.previewZoom.update((value) => Math.max(value - 10, 50));
  }

  previewPrevDocument(): void {
    const rows = this.previewableRows();
    const index = this.previewIndex();
    if (index > 0) {
      this.openDocumentPreview(rows[index - 1]);
    }
  }

  previewNextDocument(): void {
    const rows = this.previewableRows();
    const index = this.previewIndex();
    if (index >= 0 && index < rows.length - 1) {
      this.openDocumentPreview(rows[index + 1]);
    }
  }

  downloadPreviewDocument(): void {
    const row = this.previewRow();
    const url = this.previewUrl();
    if (!row?.files[0] || !url) return;

    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = docDisplayName(row.files[0]);
    anchor.click();
  }

  validatePreviewDocument(): void {
    const row = this.previewRow();
    if (!row) return;
    this.validateDocument(row);
  }

  rejectPreviewDocument(): void {
    const row = this.previewRow();
    if (!row) return;
    this.rejectDocument(row);
  }

  docIcon(type: LoanDocumentType): string {
    switch (type) {
      case 'IDENTITY':
        return 'badge';
      case 'PAYSLIPS':
        return 'payments';
      case 'TAX_NOTICE':
        return 'description';
      case 'BANK_STATEMENTS':
        return 'account_balance';
      case 'PROOF_OF_ADDRESS':
        return 'home';
      default:
        return 'attach_file';
    }
  }

  validateDocument(row: LoanDetailDocumentRow): void {
    const l = this.loan();
    if (!l || !this.canReviewDocuments()) return;

    const file = row.files[0];
    this.validateDocumentDialog
      .open({
        documentLabel: row.label,
        fileName: file ? docDisplayName(file) : null,
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.actionLoading.set(true);
        this.loanApi.validateDocument(l.id, row.type).subscribe({
          next: () => {
            this.actionLoading.set(false);
            this.snackBar.open('Pièce validée.', 'OK', { duration: 3000 });
            this.reloadHistoryAndDocs(l.id);
          },
          error: (err) => {
            this.actionLoading.set(false);
            this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
          },
        });
      });
  }

  rejectDocument(row: LoanDetailDocumentRow): void {
    const l = this.loan();
    if (!l || !this.canReviewDocuments()) return;

    const file = row.files[0];
    this.rejectDocumentDialog
      .open({
        documentLabel: row.label,
        fileName: file ? docDisplayName(file) : null,
      })
      .subscribe((comment) => {
        if (!comment?.trim()) return;

        this.actionLoading.set(true);
        this.loanApi.rejectDocument(l.id, { documentType: row.type, comment: comment.trim() }).subscribe({
          next: () => {
            this.actionLoading.set(false);
            this.snackBar.open('Pièce rejetée. Le client pourra déposer un complément.', 'OK', {
              duration: 4000,
            });
            this.reloadHistoryAndDocs(l.id);
          },
          error: (err) => {
            this.actionLoading.set(false);
            this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
          },
        });
      });
  }

  private reloadHistoryAndDocs(loanId: number): void {
    forkJoin({
      documents: this.loanApi.getDocuments(loanId),
      documentReviews: this.loanApi.getDocumentReviews(loanId),
      history: this.loanApi.getHistory(loanId),
    }).subscribe({
      next: ({ documents, documentReviews, history }) => {
        this.documents.set(documents);
        this.documentReviews.set(documentReviews);
        this.historyEvents.set(history);
        this.syncPreviewRowAfterReload();
      },
      error: (err) => {
        this.snackBar.open(getErrorMessage(err, 'Actualisation impossible.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  private syncPreviewRowAfterReload(): void {
    const current = this.previewRow();
    const loan = this.loan();
    if (!current || !loan || !this.previewOpen()) return;

    const updated = loanDetailDocuments(
      loan,
      this.documents(),
      this.documentReviews(),
      this.historyEvents()
    ).find(
      (row) => row.type === current.type
    );
    if (updated) {
      this.previewRow.set(updated);
    }
  }

  startReview(): void {
    const l = this.loan();
    if (!l || l.status !== 'SUBMITTED') return;
    this.confirmDialog
      .open({
        title: 'Mettre en analyse ?',
        message: 'Le dossier passera en statut « En analyse ».',
        confirmLabel: 'Mettre en analyse',
      })
      .pipe(filter((ok) => ok))
      .subscribe(() => this.runAction(() => this.loanApi.startReview(l.id)));
  }

  sendProposal(): void {
    const l = this.loan();
    if (!l) return;
    if (!this.canSendProposal()) {
      this.openBlockedActionHint(
        this.sendProposalBlockedHint(),
        'Impossible d\'envoyer la proposition pour le moment.'
      );
      return;
    }

    const v = this.offerForm.getRawValue();
    const approvedAmount = Number(v.approvedAmount);
    const approvedDurationMonths = Number(v.approvedDurationMonths);
    const approvedRate = Number(v.interestRate);

    this.sendProposalDialog
      .open({
        requestedAmount: Number(l.requestedAmount),
        requestedDurationMonths: l.requestedDurationMonths,
        requestedMonthlyPayment: this.requestedMonthlyPayment(),
        approvedAmount,
        approvedDurationMonths,
        approvedRate,
        approvedMonthlyPayment: calculateMonthlyPayment(
          approvedAmount,
          approvedDurationMonths,
          approvedRate / 100
        ),
        clientMessage: v.clientMessage,
      })
      .subscribe((result) => {
        if (!result) return;

        this.runAction(
          () =>
            this.loanApi.proposeOffer(l.id, {
              approvedAmount,
              approvedDurationMonths,
              interestRate: approvedRate,
              clientMessage: result.clientMessage,
            }),
          (updated) => {
            this.loan.set(updated);
            this.syncOfferForm(updated);
            this.reloadHistory(updated.id);
            this.snackBar.open(
              'Contre-offre envoyée. En attente de la réponse du client.',
              'OK',
              { duration: 5000 }
            );
          }
        );
      });
  }

  private buildApproveOfferSummary(l: LoanResponseDto): {
    amount: number;
    durationMonths: number;
    rate: number;
    monthlyPayment: number;
  } {
    const useSystemOffer = this.offerMode() === 'system' && !this.isCounterOffer();
    if (useSystemOffer) {
      return {
        amount: Number(l.requestedAmount),
        durationMonths: l.requestedDurationMonths,
        rate: this.systemRatePercent(),
        monthlyPayment: this.systemMonthlyPayment(),
      };
    }

    const v = this.offerForm.getRawValue();
    const amount = Number(l.approvedAmount ?? v.approvedAmount);
    const durationMonths = Number(l.approvedDurationMonths ?? v.approvedDurationMonths);
    const rate = Number(l.interestRate ?? v.interestRate);
    return {
      amount,
      durationMonths,
      rate,
      monthlyPayment: calculateMonthlyPayment(amount, durationMonths, rate / 100),
    };
  }

  approve(): void {
    const l = this.loan();
    if (!l) return;
    if (!this.canApprove()) {
      this.openBlockedActionHint(
        this.approveBlockedHint(),
        'Impossible d\'approuver le dossier pour le moment.'
      );
      return;
    }

    this.approveLoanDialog.open(this.buildApproveOfferSummary(l)).subscribe((confirmed) => {
      if (!confirmed) return;

      this.actionLoading.set(true);
      this.loanApi.approve(l.id).subscribe({
        next: (approved) => {
          this.loan.set(approved);
          this.actionLoading.set(false);
          this.reloadHistory(l.id);
          this.snackBar.open('Dossier approuvé.', 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
        },
      });
    });
  }

  reject(): void {
    const l = this.loan();
    if (!l || this.isReadOnly()) return;
    if (l.status !== 'UNDER_REVIEW' && l.status !== 'OFFER_PENDING') {
      this.snackBar.open('Ce dossier ne peut plus être refusé dans son état actuel.', 'Fermer', {
        duration: 5000,
      });
      return;
    }

    this.rejectLoanDialog.open().subscribe((comment) => {
      if (!comment?.trim()) return;

      this.runAction(() => this.loanApi.reject(l.id, { comment: comment.trim() }), (updated) => {
        this.reloadHistory(updated.id);
        this.snackBar.open('Dossier refusé. Le client a été informé.', 'OK', { duration: 4000 });
      });
    });
  }

  private runAction(
    call: () => ReturnType<LoanApiService['startReview']>,
    onSuccess?: (loan: LoanResponseDto) => void
  ): void {
    this.actionLoading.set(true);
    call().subscribe({
      next: (updated) => {
        this.loan.set(updated);
        this.actionLoading.set(false);
        onSuccess?.(updated);
        if (!onSuccess) {
          this.reloadHistory(updated.id);
          this.snackBar.open('Action enregistrée.', 'OK', { duration: 3000 });
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }

  private reloadHistory(loanId: number): void {
    this.loanApi.getHistory(loanId).subscribe({
      next: (history) => this.historyEvents.set(history),
      error: (err) => {
        this.snackBar.open(getErrorMessage(err, 'Historique indisponible.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  private openBlockedActionHint(hint: string | null, fallback: string): void {
    this.snackBar.open(hint ?? fallback, 'Fermer', { duration: 5000 });
  }

  private checklistBlockedHint(action: string): string | null {
    const c = this.checklist();
    if (!c.allValidated) {
      return `Validez toutes les pièces obligatoires avant ${action}.`;
    }
    if (!c.noRejected) {
      return `Corrigez les pièces refusées avant ${action}.`;
    }
    return null;
  }

  onOfferModeChange(mode: OfferMode): void {
    if (this.offerFormLocked()) return;
    this.offerMode.set(mode);
    const l = this.loan();
    if (!l) return;
    if (mode === 'system') {
      this.offerForm.patchValue({
        approvedAmount: Number(l.requestedAmount),
        approvedDurationMonths: l.requestedDurationMonths,
        interestRate: this.systemRatePercent(),
      });
    }
  }
}
