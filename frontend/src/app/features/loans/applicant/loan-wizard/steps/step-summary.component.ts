import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoanWizardStateService } from '../../../../../core/loans/services/loan-wizard-state.service';
import {
  DOCUMENT_SLOTS,
  EMPLOYER_SECTOR_OPTIONS,
  EMPLOYMENT_OPTIONS,
  LOAN_DEBT_RATIO_DISPLAY_MAX,
  LOAN_PURPOSE_OPTIONS,
  SENIORITY_RANGE_OPTIONS,
} from '../../../../../core/loans/constants/loan.constants';
import { LOAN_CGU_ARTICLES } from '../../../../../core/loans/constants/cgu.constants';
import {
  calculateDebtRatio,
  calculateMonthlyPayment,
} from '../../../../../core/loans/utils/loan-calculator';
import { LoanDocumentType } from '../../../../../core/loans/models/loan.enums';
import { LoanDocumentResponseDto } from '../../../../../core/loans/models/loan-document.model';

type RecapSectionKey = 'need' | 'situation' | 'documents' | 'cgu';

interface DocumentRecapItem {
  type: LoanDocumentType;
  label: string;
  required: boolean;
  icon: string;
  files: LoanDocumentResponseDto[];
  status: 'provided' | 'missing' | 'optional';
}

interface CompletionCheckItem {
  label: string;
  ok: boolean;
  warn?: boolean;
  detail?: string;
}

@Component({
  selector: 'app-step-summary',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    CurrencyPipe,
    DecimalPipe,
  ],
  templateUrl: './step-summary.component.html',
  styleUrl: './step-summary.component.scss',
})
export class StepSummaryComponent {
  readonly state = inject(LoanWizardStateService);
  readonly form = this.state.form;
  readonly documents = this.state.documents;
  readonly reference = this.state.reference;
  readonly cguArticles = LOAN_CGU_ARTICLES;

  readonly expanded = signal<Record<RecapSectionKey, boolean>>({
    need: true,
    situation: true,
    documents: true,
    cgu: false,
  });

  readonly fv = () => this.state.formValue();

  readonly purposeLabel = computed(() => {
    const v = this.fv().loanPurpose;
    return LOAN_PURPOSE_OPTIONS.find((p) => p.value === v)?.label ?? v;
  });

  readonly employmentLabel = computed(() => {
    const v = this.fv().employmentStatus;
    return EMPLOYMENT_OPTIONS.find((e) => e.value === v)?.label ?? v;
  });

  readonly sectorLabel = computed(() => {
    const v = this.fv().employerSector;
    return EMPLOYER_SECTOR_OPTIONS.find((s) => s.value === v)?.label ?? '';
  });

  readonly seniorityLabel = computed(() => {
    const v = this.fv().seniorityRange;
    return SENIORITY_RANGE_OPTIONS.find((s) => s.value === v)?.label ?? '';
  });

  readonly monthlyPayment = computed(() => {
    const v = this.fv();
    return calculateMonthlyPayment(
      Number(v.requestedAmount ?? 0),
      Number(v.requestedDurationMonths ?? 0)
    );
  });

  readonly totalIncome = computed(() => {
    const v = this.fv();
    return Number(v.monthlyIncome ?? 0) + Number(v.additionalIncome ?? 0);
  });

  readonly totalCharges = computed(() => {
    const v = this.fv();
    return (
      Number(v.monthlyRent ?? 0) +
      Number(v.monthlyLoanPayments ?? 0) +
      Number(v.monthlyAlimony ?? 0) +
      Number(v.monthlyOtherCharges ?? 0)
    );
  });

  readonly debtRatio = computed(() =>
    calculateDebtRatio(this.totalCharges(), this.monthlyPayment(), this.totalIncome())
  );

  readonly debtRatioPercent = computed(() => this.debtRatio() * 100);

  readonly debtRatioOk = computed(() => this.debtRatio() <= LOAN_DEBT_RATIO_DISPLAY_MAX);

  readonly needSummary = computed(() => {
    const v = this.fv();
    const amount = Number(v.requestedAmount ?? 0);
    const months = Number(v.requestedDurationMonths ?? 0);
    const amountStr = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(amount);
    return `${this.purposeLabel()} · ${amountStr} € sur ${this.formatDurationShort(months)}`;
  });

  readonly situationSummary = computed(() => {
    const parts = [this.employmentLabel()];
    const job = this.fv().jobTitle?.trim();
    if (job) parts.push(job);
    parts.push(`${this.formatCurrency(this.totalIncome())} € net/mois`);
    return parts.join(' · ');
  });

  readonly chargesLabel = computed(() => {
    const v = this.fv();
    const rent = Number(v.monthlyRent ?? 0);
    const total = this.totalCharges();
    if (rent > 0 && total === rent) return `${this.formatCurrency(total)} € (loyer actuel)`;
    if (rent > 0) return `${this.formatCurrency(total)} € (dont loyer ${this.formatCurrency(rent)} €)`;
    return `${this.formatCurrency(total)} €`;
  });

  readonly requiredDocsProvided = computed(
    () => this.documentRecapItems().filter((i) => i.required && i.status === 'provided').length
  );

  readonly requiredDocsCount = computed(() => DOCUMENT_SLOTS.filter((s) => s.required).length);

  readonly documentsSummary = computed(() => {
    const p = this.requiredDocsProvided();
    const t = this.requiredDocsCount();
    if (p >= t) return `${p}/${t} documents obligatoires fournis`;
    return `${p}/${t} documents obligatoires — ${t - p} manquant(s)`;
  });

  readonly documentsComplete = computed(
    () => this.requiredDocsProvided() >= this.requiredDocsCount()
  );

  readonly cguAccepted = computed(() => {
    const v = this.fv();
    return !!v.acceptTerms && !!v.certifyAccuracy;
  });

  readonly infoComplete = computed(() => !!this.fv().title?.trim());

  readonly needComplete = computed(() => {
    const v = this.fv();
    return (
      this.infoComplete() &&
      !!v.loanPurpose &&
      Number(v.requestedAmount) > 0 &&
      Number(v.requestedDurationMonths) > 0
    );
  });

  readonly situationComplete = computed(() => Number(this.fv().monthlyIncome) > 0);

  readonly completionChecks = computed((): CompletionCheckItem[] => [
    { label: 'Informations personnelles', ok: this.infoComplete() },
    { label: 'Besoin de financement', ok: this.needComplete() },
    { label: 'Situation professionnelle', ok: this.situationComplete() },
    {
      label: 'Documents',
      ok: this.documentsComplete(),
      warn: !this.documentsComplete(),
      detail: `(${this.requiredDocsProvided()}/${this.requiredDocsCount()})`,
    },
    { label: 'CGU acceptées', ok: this.cguAccepted() },
  ]);

  readonly completionPercent = computed(() => {
    const checks = this.completionChecks();
    const done = checks.filter((c) => c.ok).length;
    return Math.round((done / checks.length) * 100);
  });

  readonly completionStatusLabel = computed(() => {
    const p = this.completionPercent();
    if (p === 100) return 'Dossier complet';
    if (p >= 75) return 'Dossier quasi complet';
    if (p >= 50) return 'Dossier en cours';
    return 'Dossier à compléter';
  });

  readonly completionHint = computed(() => {
    const missing = this.missingRequiredDocs().length;
    if (missing === 0 && !this.cguAccepted()) {
      return 'Acceptez les CGU pour finaliser votre dossier.';
    }
    if (missing === 0) return 'Votre dossier est prêt pour la soumission.';
    if (missing === 1) {
      return '1 document obligatoire manquant avant soumission finale.';
    }
    return `${missing} documents obligatoires manquants avant soumission finale.`;
  });

  readonly missingRequiredDocs = computed(() =>
    this.documentRecapItems().filter((i) => i.status === 'missing')
  );

  readonly documentRecapItems = computed((): DocumentRecapItem[] => {
    const docs = this.documents();
    return DOCUMENT_SLOTS.map((slot) => {
      const files = docs.filter((d) => d.documentType === slot.type);
      let status: DocumentRecapItem['status'] = 'optional';
      if (files.length > 0) status = 'provided';
      else if (slot.required) status = 'missing';
      return {
        type: slot.type,
        label: slot.label,
        required: slot.required,
        icon: this.documentIcon(slot.type),
        files,
        status,
      };
    });
  });

  readonly durationYearsLabel = computed(() => {
    const months = Number(this.fv().requestedDurationMonths ?? 0);
    if (months % 12 === 0 && months > 0) {
      const y = months / 12;
      return `${y} an${y > 1 ? 's' : ''}`;
    }
    return `${months} mois`;
  });

  toggleSection(key: RecapSectionKey): void {
    this.expanded.update((e) => ({ ...e, [key]: !e[key] }));
  }

  isExpanded(key: RecapSectionKey): boolean {
    return this.expanded()[key];
  }

  goToStep(step: number, event?: Event): void {
    event?.stopPropagation();
    this.state.currentStep.set(step);
    this.state.persistSession();
  }

  docDisplayName(doc: LoanDocumentResponseDto): string {
    return doc.displayName?.trim() || doc.originalFileName;
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  formatDurationLong(months: number): string {
    if (!months) return '—';
    if (months % 12 === 0) {
      const years = months / 12;
      return `${years} an${years > 1 ? 's' : ''} (${months} mois)`;
    }
    return `${months} mois`;
  }

  private formatDurationShort(months: number): string {
    if (!months) return '—';
    if (months % 12 === 0) {
      const years = months / 12;
      return `${years} an${years > 1 ? 's' : ''}`;
    }
    return `${months} mois`;
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(value);
  }

  private documentIcon(type: LoanDocumentType): string {
    const icons: Record<LoanDocumentType, string> = {
      IDENTITY: 'badge',
      PAYSLIPS: 'receipt_long',
      TAX_NOTICE: 'description',
      BANK_STATEMENTS: 'account_balance_wallet',
      PROOF_OF_ADDRESS: 'home',
      OTHER: 'attach_file',
    };
    return icons[type];
  }
}
