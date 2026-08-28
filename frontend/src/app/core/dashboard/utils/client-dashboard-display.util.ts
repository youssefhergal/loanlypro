import { STATUS_LABELS } from '../../loans/constants/loan.constants';
import { LoanApplicationStatus } from '../../loans/models/loan.enums';
import { LoanSummaryDto } from '../../loans/repayment/models/loan-summary.model';
import { JustificatifGroupDto, JustificatifItemDto } from '../../documents/models/justificatif.model';
import { NotificationDto } from '../../notifications/models/notification.model';
import { LoanApplicationSummaryDto } from '../services/dashboard-api.service';
import { PaymentTransactionDto } from '../services/dashboard-api.service';

export interface ApplicationProgressStep {
  label: string;
  state: 'done' | 'current' | 'upcoming';
}

export interface DocumentAlertInfo {
  loanApplicationId: number;
  loanReference: string;
  rejectedCount: number;
}

export interface PaymentFailureAlertInfo {
  loanId: number;
  loanReference: string;
}

export interface RepaymentProgressInfo {
  percent: number;
  paidCount: number;
  totalCount: number;
  capitalRepaid: number;
}

export type HeroCarouselVisual =
  | 'payment'
  | 'documents'
  | 'progress'
  | 'welcome'
  | 'personal-loan'
  | 'works-loan'
  | 'dossier'
  | 'advisor-dossiers'
  | 'advisor-portfolio'
  | 'advisor-sla'
  | 'advisor-welcome';

export interface HeroCarouselSlide {
  id: string;
  eyebrow: string;
  amountLine: string;
  ctaLabel: string;
  routerLink: (string | number)[];
  queryParams?: Record<string, string | number>;
  visual: HeroCarouselVisual;
}

export interface NewClientOnboardingStep {
  icon: string;
  label: string;
}

export const NEW_CLIENT_ONBOARDING_STEPS: NewClientOnboardingStep[] = [
  { icon: 'calculate', label: 'Simuler' },
  { icon: 'upload_file', label: 'Déposer vos pièces' },
  { icon: 'track_changes', label: 'Suivre votre dossier' },
];

export function isNewClientProfile(
  demandes: LoanApplicationSummaryDto[],
  loans: LoanSummaryDto[],
): boolean {
  return demandes.length === 0 && loans.length === 0;
}

export function hasActiveRepaymentLoan(loans: LoanSummaryDto[]): boolean {
  return loans.some((loan) => loan.status === 'ACTIVE');
}

/** Profil C : prêt ACTIVE (emprunteur en cours de remboursement). */
export function isActiveBorrowerProfile(
  demandes: LoanApplicationSummaryDto[],
  loans: LoanSummaryDto[],
): boolean {
  return hasActiveRepaymentLoan(loans) && !isDossierEnCoursProfile(demandes, loans);
}

/** Profil D : au moins un prêt CLOSED, sans prêt ACTIVE ni dossier en cours. */
export function isClosedLoanProfile(
  demandes: LoanApplicationSummaryDto[],
  loans: LoanSummaryDto[],
): boolean {
  if (isNewClientProfile(demandes, loans)) {
    return false;
  }
  if (hasActiveRepaymentLoan(loans)) {
    return false;
  }
  if (isDossierEnCoursProfile(demandes, loans)) {
    return false;
  }
  return loans.some((loan) => loan.status === 'CLOSED');
}

export function pickPrimaryClosedLoan(loans: LoanSummaryDto[]): LoanSummaryDto | null {
  const closed = loans.filter((loan) => loan.status === 'CLOSED');
  if (closed.length === 0) {
    return null;
  }
  return closed[0];
}

export function countClosedLoans(loans: LoanSummaryDto[]): number {
  return loans.filter((loan) => loan.status === 'CLOSED').length;
}

const ARCHIVED_APPLICATION_STATUSES = ['APPROVED', 'REJECTED', 'CANCELLED'] as const;

export function countArchivedApplications(demandes: LoanApplicationSummaryDto[]): number {
  return demandes.filter((d) =>
    ARCHIVED_APPLICATION_STATUSES.includes(d.status as (typeof ARCHIVED_APPLICATION_STATUSES)[number]),
  ).length;
}

export interface ClosedLoanRecapInfo {
  paidCount: number;
  totalCount: number;
  reference: string;
  loanId: number;
}

export function computeClosedLoanRecap(loan: LoanSummaryDto | null): ClosedLoanRecapInfo | null {
  if (!loan || loan.status !== 'CLOSED' || !loan.installmentCount) {
    return null;
  }
  const totalCount = loan.installmentCount;
  const remaining = loan.remainingInstallmentsCount ?? 0;
  const paidCount = Math.max(0, totalCount - remaining);
  return {
    paidCount,
    totalCount,
    reference: loan.reference,
    loanId: loan.id,
  };
}

export function buildClosedLoanHeroCarouselSlides(): HeroCarouselSlide[] {
  return [
    {
      id: 'personal-loan',
      eyebrow: 'Crédit personnel',
      amountLine: 'Taux à partir de 3,5 % TAEG',
      ctaLabel: 'Simuler mon projet',
      routerLink: ['/nouvelle-demande'],
      visual: 'personal-loan',
    },
    {
      id: 'works-loan',
      eyebrow: 'Crédit travaux',
      amountLine: "Jusqu'à 75 000 € pour vos rénovations",
      ctaLabel: "Découvrir l'offre",
      routerLink: ['/nouvelle-demande'],
      visual: 'works-loan',
    },
    {
      id: 'welcome',
      eyebrow: 'Un nouveau projet ?',
      amountLine: 'Déposez une nouvelle demande en quelques minutes',
      ctaLabel: 'Nouvelle demande',
      routerLink: ['/nouvelle-demande'],
      visual: 'welcome',
    },
  ];
}

/** Profil B : au moins une demande ouverte, sans prêt ACTIVE. */
export function isDossierEnCoursProfile(
  demandes: LoanApplicationSummaryDto[],
  loans: LoanSummaryDto[],
): boolean {
  if (isNewClientProfile(demandes, loans)) {
    return false;
  }
  if (hasActiveRepaymentLoan(loans)) {
    return false;
  }
  return demandes.some((d) => !['REJECTED', 'CANCELLED'].includes(d.status));
}

export function pickPrimaryTrackedDemande(
  demandes: LoanApplicationSummaryDto[],
): LoanApplicationSummaryDto | null {
  const inProgress = demandes.filter((d) =>
    IN_PROGRESS_STATUSES.includes(d.status as LoanApplicationStatus),
  );
  const pool = inProgress.length > 0 ? inProgress : demandes.filter(
    (d) => !['REJECTED', 'CANCELLED'].includes(d.status),
  );
  if (pool.length === 0) {
    return null;
  }
  return [...pool].sort((a, b) => {
    const aTime = new Date(a.submittedAt ?? a.createdAt ?? 0).getTime();
    const bTime = new Date(b.submittedAt ?? b.createdAt ?? 0).getTime();
    return bTime - aTime;
  })[0];
}

function dossierFollowUpEyebrow(status: string): string {
  switch (status) {
    case 'UNDER_REVIEW':
      return "Votre dossier est en cours d'étude";
    case 'SUBMITTED':
      return 'Votre dossier a bien été déposé';
    case 'OFFER_PENDING':
      return 'Offre en attente de votre réponse';
    default:
      return 'Suivi de votre dossier';
  }
}

function dossierFollowUpSubtitle(status: string): string {
  switch (status) {
    case 'UNDER_REVIEW':
    case 'SUBMITTED':
      return 'Réponse sous 48 h ouvrées';
    case 'OFFER_PENDING':
      return 'Consultez et répondez à votre offre';
    default:
      return "Consultez l'avancement de votre dossier";
  }
}

export const DOSSIER_VERTICAL_TIMELINE_LABELS = [
  'Soumis',
  'Étude en cours',
  'Offre',
  'Déblocage',
] as const;

export function dossierVerticalTimelineSteps(status: string): ApplicationProgressStep[] {
  let currentIndex = -1;
  switch (status) {
    case 'SUBMITTED':
      currentIndex = 0;
      break;
    case 'UNDER_REVIEW':
      currentIndex = 1;
      break;
    case 'OFFER_PENDING':
      currentIndex = 2;
      break;
    case 'APPROVED':
      currentIndex = 3;
      break;
    default:
      currentIndex = -1;
  }

  return DOSSIER_VERTICAL_TIMELINE_LABELS.map((label, index) => {
    if (currentIndex < 0) {
      return { label, state: 'upcoming' as const };
    }
    if (index < currentIndex) {
      return { label, state: 'done' as const };
    }
    if (index === currentIndex) {
      return { label, state: 'current' as const };
    }
    return { label, state: 'upcoming' as const };
  });
}

export function buildHeroCarouselSlides(input: {
  loan: LoanSummaryDto | null;
  primaryDemande?: LoanApplicationSummaryDto | null;
  paymentDays: number | null;
  documentAlert: DocumentAlertInfo | null;
  progress: RepaymentProgressInfo | null;
  dossierEnCoursMode?: boolean;
  activeBorrowerMode?: boolean;
}): HeroCarouselSlide[] {
  const slides: HeroCarouselSlide[] = [];
  const activeLoan = input.loan?.status === 'ACTIVE' ? input.loan : null;
  const primaryDemande = input.primaryDemande ?? null;

  const dossierSlide =
    input.dossierEnCoursMode &&
    primaryDemande &&
    showApplicationStepper(primaryDemande.status)
      ? ({
          id: 'dossier-follow-up',
          eyebrow: dossierFollowUpEyebrow(primaryDemande.status),
          amountLine: `${primaryDemande.reference} — ${dossierFollowUpSubtitle(primaryDemande.status)}`,
          ctaLabel: 'Suivre mon dossier',
          routerLink: ['/mes-demandes', primaryDemande.id],
          visual: 'dossier',
        } satisfies HeroCarouselSlide)
      : null;

  const documentSlide = input.documentAlert
    ? ({
        id: 'documents',
        eyebrow: 'Document à compléter',
        amountLine: `${input.documentAlert.rejectedCount} justificatif${input.documentAlert.rejectedCount > 1 ? 's' : ''} refusé${input.documentAlert.rejectedCount > 1 ? 's' : ''} — ${input.documentAlert.loanReference}`,
        ctaLabel: 'Compléter mon dossier',
        routerLink: ['/mes-demandes', input.documentAlert.loanApplicationId],
        visual: 'documents',
      } satisfies HeroCarouselSlide)
    : null;

  let paymentSlide: HeroCarouselSlide | null = null;
  let progressSlide: HeroCarouselSlide | null = null;

  if (activeLoan) {
    const amount = activeLoan.nextInstallmentAmount ?? activeLoan.monthlyPayment;
    const amountText =
      amount != null
        ? new Intl.NumberFormat('fr-FR', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2,
          }).format(amount)
        : '—';
    paymentSlide = {
      id: 'payment',
      eyebrow: nextPaymentLabel(input.paymentDays),
      amountLine: `${amountText} — Prêt ${activeLoan.reference}`,
      ctaLabel: "Voir l'échéancier",
      routerLink: ['/paiements'],
      queryParams: { loanId: activeLoan.id },
      visual: 'payment',
    };
  }

  if (activeLoan && input.progress) {
    progressSlide = {
      id: 'progress',
      eyebrow: 'Progression de votre crédit',
      amountLine: `${input.progress.percent}% remboursé — ${activeLoan.reference}`,
      ctaLabel: 'Voir mon prêt',
      routerLink: ['/mes-prets', activeLoan.id],
      visual: 'progress',
    };
  }

  if (input.activeBorrowerMode) {
    if (paymentSlide) slides.push(paymentSlide);
    if (progressSlide) slides.push(progressSlide);
    if (documentSlide) slides.push(documentSlide);
  } else {
    if (dossierSlide) slides.push(dossierSlide);
    if (documentSlide) slides.push(documentSlide);
    if (paymentSlide) slides.push(paymentSlide);
    if (progressSlide) slides.push(progressSlide);
  }

  if (slides.length === 0) {
    slides.push({
      id: 'welcome',
      eyebrow: 'Bienvenue sur LoanlyPro',
      amountLine: 'Déposez votre première demande de prêt en quelques minutes',
      ctaLabel: 'Nouvelle demande',
      routerLink: ['/nouvelle-demande'],
      visual: 'welcome',
    });
  }

  return slides;
}

export function dashboardApplicationStatusLabel(status: string): string {
  if (status === 'APPROVED') {
    return 'Approuvé';
  }
  if (status === 'UNDER_REVIEW') {
    return 'En étude';
  }
  return applicationStatusLabel(status);
}

export function applicationStatusIcon(status: string): string {
  switch (status) {
    case 'DRAFT':
      return 'edit_note';
    case 'SUBMITTED':
      return 'send';
    case 'UNDER_REVIEW':
      return 'hourglass_top';
    case 'OFFER_PENDING':
      return 'local_offer';
    case 'APPROVED':
      return 'verified';
    case 'REJECTED':
      return 'block';
    case 'CANCELLED':
      return 'cancel';
    default:
      return 'folder_open';
  }
}

const IN_PROGRESS_STATUSES: LoanApplicationStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'OFFER_PENDING',
];

const PROGRESS_STEP_LABELS = ['Déposé', 'Étude', 'Offre', 'Déblocage'] as const;

export function countInProgressApplications(
  demandes: LoanApplicationSummaryDto[],
): number {
  return demandes.filter((d) =>
    IN_PROGRESS_STATUSES.includes(d.status as LoanApplicationStatus),
  ).length;
}

export function applicationStatusLabel(status: string): string {
  return STATUS_LABELS[status as LoanApplicationStatus]?.label ?? status;
}

export function applicationStatusClass(status: string): string {
  return STATUS_LABELS[status as LoanApplicationStatus]?.chipClass ?? 'status-default';
}

export function applicationRouterLink(d: LoanApplicationSummaryDto): (string | number)[] {
  return d.status === 'DRAFT'
    ? ['/nouvelle-demande', d.id]
    : ['/mes-demandes', d.id];
}

export function showApplicationStepper(status: string): boolean {
  return !['DRAFT', 'REJECTED', 'CANCELLED'].includes(status);
}

export function applicationProgressSteps(status: string): ApplicationProgressStep[] {
  let currentIndex = -1;
  switch (status) {
    case 'SUBMITTED':
      currentIndex = 0;
      break;
    case 'UNDER_REVIEW':
      currentIndex = 1;
      break;
    case 'OFFER_PENDING':
      currentIndex = 2;
      break;
    case 'APPROVED':
      currentIndex = 4;
      break;
    default:
      currentIndex = -1;
  }

  return PROGRESS_STEP_LABELS.map((label, index) => {
    if (currentIndex < 0) {
      return { label, state: 'upcoming' as const };
    }
    if (index < currentIndex) {
      return { label, state: 'done' as const };
    }
    if (index === currentIndex) {
      return { label, state: 'current' as const };
    }
    return { label, state: 'upcoming' as const };
  });
}

export function pickPrimaryActiveLoan(loans: LoanSummaryDto[]): LoanSummaryDto | null {
  const active = loans.find((loan) => loan.status === 'ACTIVE');
  if (active) {
    return active;
  }
  return loans.find((loan) => loan.status === 'PENDING_MANDATE') ?? loans[0] ?? null;
}

export function daysUntilDate(isoDate: string | null | undefined): number | null {
  if (!isoDate) {
    return null;
  }
  const target = new Date(isoDate);
  if (Number.isNaN(target.getTime())) {
    return null;
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

export function nextPaymentLabel(days: number | null): string {
  if (days == null) {
    return 'Prochain prélèvement';
  }
  if (days === 0) {
    return "Prochain prélèvement aujourd'hui";
  }
  if (days === 1) {
    return 'Prochain prélèvement demain';
  }
  if (days > 1) {
    return `Prochain prélèvement dans ${days} jours`;
  }
  return `Prochain prélèvement (${Math.abs(days)} j. de retard)`;
}

export function computeRepaymentProgress(loan: LoanSummaryDto | null): RepaymentProgressInfo | null {
  if (!loan?.installmentCount) {
    return null;
  }
  const totalCount = loan.installmentCount;
  const remaining = loan.remainingInstallmentsCount ?? 0;
  const paidCount = Math.max(0, totalCount - remaining);
  const percent = totalCount > 0 ? Math.round((paidCount / totalCount) * 100) : 0;
  const capitalRepaid = Math.max(0, loan.principalAmount - loan.remainingBalance);
  return { percent, paidCount, totalCount, capitalRepaid };
}

export function flattenJustificatifDocuments(
  groups: JustificatifGroupDto[],
): Array<JustificatifItemDto & { loanReference: string; loanApplicationId: number }> {
  return groups.flatMap((group) =>
    group.documents.map((doc) => ({
      ...doc,
      loanReference: group.loanReference,
      loanApplicationId: group.loanApplicationId,
    })),
  );
}

export function countDocumentsToProcess(groups: JustificatifGroupDto[]): number {
  return flattenJustificatifDocuments(groups).filter(
    (doc) => doc.validationStatus === 'REJECTED',
  ).length;
}

export function findDocumentAlert(groups: JustificatifGroupDto[]): DocumentAlertInfo | null {
  for (const group of groups) {
    const rejectedCount = group.documents.filter(
      (doc) => doc.validationStatus === 'REJECTED',
    ).length;
    if (rejectedCount > 0) {
      return {
        loanApplicationId: group.loanApplicationId,
        loanReference: group.loanReference,
        rejectedCount,
      };
    }
  }
  return null;
}

export function findPaymentFailureAlert(
  loans: LoanSummaryDto[],
  transactions: PaymentTransactionDto[],
  notifications: NotificationDto[] = [],
): PaymentFailureAlertInfo | null {
  const activeLoan = loans.find((loan) => loan.status === 'ACTIVE');
  if (!activeLoan) {
    return null;
  }

  const hasFailedTransaction = transactions.some((tx) => tx.status === 'FAILED');
  const hasOverdueInstallments = (activeLoan.overdueInstallmentsCount ?? 0) > 0;
  const hasFailedNotification = notifications.some(
    (notification) => notification.eventType === 'PAYMENT_FAILED',
  );

  if (!hasFailedTransaction && !hasOverdueInstallments && !hasFailedNotification) {
    return null;
  }

  return {
    loanId: activeLoan.id,
    loanReference: activeLoan.reference,
  };
}

export function recentActionDocuments(
  groups: JustificatifGroupDto[],
  limit = 3,
): Array<JustificatifItemDto & { loanReference: string }> {
  return flattenJustificatifDocuments(groups)
    .filter(
      (doc) =>
        doc.validationStatus === 'REJECTED' || doc.validationStatus === 'PENDING',
    )
    .sort(
      (a, b) =>
        new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime(),
    )
    .slice(0, limit);
}
