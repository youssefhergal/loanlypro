import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { EMPLOYMENT_OPTIONS, LOAN_INTEREST_RATE } from '../../../../core/loans/constants/loan.constants';
import { LoanDocumentResponseDto } from '../../../../core/loans/models/loan-document.model';
import { LoanDocumentReviewResponseDto } from '../../../../core/loans/models/loan-document-review.model';
import { LoanDocumentType } from '../../../../core/loans/models/loan.enums';
import { LoanHistoryEventResponseDto } from '../../../../core/loans/models/loan-history.model';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { calculateDebtRatio, calculateMonthlyPayment } from '../../../../core/loans/utils/loan-calculator';
import {
  LoanDetailDocumentRow,
  advisorReviewStatusLabel,
  docDisplayName,
  formatDocSize,
  historyEventsToTimeline,
  loanDetailDocuments,
} from '../../../../core/loans/utils/loan-detail.util';
import { LoanDocumentAdvisorReviewStatus } from '../../../../core/loans/utils/loan-detail-document-review.util';
import { getErrorMessage } from '../../../../core/loans/utils/api-error.util';
import {
  advisorInitials,
  formatDateFrLong,
  loanCardStatusStyle,
  loanPurposeLabel,
} from '../../../../core/loans/utils/loan-list.util';
import { AdvisorDocumentPreviewPanelComponent } from '../../advisor/loan-application-detail/advisor-document-preview-panel/advisor-document-preview-panel.component';

@Component({
  selector: 'app-admin-loan-application-detail',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    AdvisorDocumentPreviewPanelComponent,
  ],
  templateUrl: './admin-loan-application-detail.component.html',
  styleUrl: './admin-loan-application-detail.component.scss',
})
export class AdminLoanApplicationDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly loanApi = inject(LoanApiService);
  private readonly snackBar = inject(MatSnackBar);
  private previewObjectUrl: string | null = null;

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly loan = signal<LoanResponseDto | null>(null);
  readonly documents = signal<LoanDocumentResponseDto[]>([]);
  readonly documentReviews = signal<LoanDocumentReviewResponseDto[]>([]);
  readonly historyEvents = signal<LoanHistoryEventResponseDto[]>([]);

  readonly previewOpen = signal(false);
  readonly previewRow = signal<LoanDetailDocumentRow | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly previewZoom = signal(100);

  readonly advisorInitials = advisorInitials;
  readonly formatDate = formatDateFrLong;
  readonly docSize = formatDocSize;

  readonly statusStyle = computed(() => {
    const l = this.loan();
    return l ? loanCardStatusStyle(l.status) : null;
  });

  readonly purposeLabel = computed(() => {
    const l = this.loan();
    if (!l) return '—';
    return `${loanPurposeLabel(l.loanPurpose)}${l.purpose ? ` — ${l.purpose}` : ''}`;
  });

  readonly employmentLine = computed(() => {
    const l = this.loan();
    if (!l) return '—';
    const emp =
      EMPLOYMENT_OPTIONS.find((e) => e.value === l.employmentStatus)?.label ??
      l.employmentStatus;
    const parts = [emp];
    if (l.jobTitle?.trim()) parts.push(l.jobTitle.trim());
    if (l.employerName?.trim()) parts.push(`chez ${l.employerName.trim()}`);
    return parts.join(' · ');
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

  readonly documentRows = computed(() => {
    const l = this.loan();
    if (!l) return [];
    return loanDetailDocuments(
      l,
      this.documents(),
      this.documentReviews(),
      this.historyEvents()
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

  readonly requiredCount = computed(
    () => this.documentRows().filter((row) => row.required).length
  );

  readonly requiredProvided = computed(
    () =>
      this.documentRows().filter((row) => row.required && row.files.length > 0).length
  );

  readonly timeline = computed(() => historyEventsToTimeline(this.historyEvents()));

  readonly hasOfferTerms = computed(() => {
    const l = this.loan();
    return !!l?.approvedAmount && !!l.approvedDurationMonths;
  });

  readonly isCounterOffer = computed(() => {
    const l = this.loan();
    if (!l?.approvedAmount) return false;
    return (
      Number(l.approvedAmount) !== Number(l.requestedAmount) ||
      l.approvedDurationMonths !== l.requestedDurationMonths
    );
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!id || Number.isNaN(id)) {
      this.error.set('Identifiant invalide.');
      this.loading.set(false);
      return;
    }
    this.fetch(id);
  }

  ngOnDestroy(): void {
    this.revokePreviewUrl();
  }

  private fetch(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      loan: this.loanApi.getById(id),
      documents: this.loanApi.getDocuments(id),
      history: this.loanApi.getHistory(id),
      reviews: this.loanApi.getDocumentReviews(id),
    }).subscribe({
      next: ({ loan, documents, history, reviews }) => {
        this.loan.set(loan);
        this.documents.set(documents);
        this.historyEvents.set(history);
        this.documentReviews.set(reviews);
        this.loading.set(false);
      },
      error: (err) => {
        const message =
          err?.status === 404
            ? 'Demande introuvable.'
            : getErrorMessage(err);
        this.error.set(message);
        this.loading.set(false);
      },
    });
  }

  displayFileName(row: LoanDetailDocumentRow): string | null {
    const file = row.files[0];
    return file ? docDisplayName(file) : null;
  }

  reviewBadgeClass(status: LoanDocumentAdvisorReviewStatus | undefined): string {
    switch (status) {
      case 'validated':
        return 'admin-doc-badge admin-doc-badge--ok';
      case 'rejected':
        return 'admin-doc-badge admin-doc-badge--ko';
      case 'pending_review':
        return 'admin-doc-badge admin-doc-badge--pending';
      default:
        return 'admin-doc-badge';
    }
  }

  reviewLabel(status: LoanDocumentAdvisorReviewStatus | undefined): string {
    if (!status) return '—';
    return advisorReviewStatusLabel(status);
  }

  previewPanelStatusLabel(status: LoanDocumentAdvisorReviewStatus | undefined): string {
    if (status === 'pending_review') return 'À VÉRIFIER';
    if (status === 'validated') return 'VALIDÉ ✓';
    if (status === 'rejected') return 'REJETÉ';
    if (status === 'missing_upload') return 'MANQUANT';
    return '—';
  }

  previewStatusBadgeClass(status: LoanDocumentAdvisorReviewStatus | undefined): string {
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

  previewFileMeta(row: LoanDetailDocumentRow): string {
    const file = row.files[0];
    if (!file) return '—';
    return `${docDisplayName(file)} • ${formatDocSize(file.fileSizeBytes)}`;
  }

  openDocumentPreview(row: LoanDetailDocumentRow): void {
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
