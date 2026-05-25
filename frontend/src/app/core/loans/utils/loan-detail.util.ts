import { LoanApplicationStatus, LoanDocumentType } from '../models/loan.enums';
import { LoanResponseDto } from '../models/loan-response.model';
import { LoanDocumentResponseDto } from '../models/loan-document.model';
import { DOCUMENT_SLOTS } from '../constants/loan.constants';
import { formatDateFrLong } from './loan-list.util';
import { loanCardStatusStyle } from './loan-list.util';
import { LoanHistoryEventResponseDto } from '../models/loan-history.model';
import {
  LoanDocumentAdvisorReview,
  advisorReviewsFromHistory,
  resolveAdvisorReview,
} from './loan-detail-document-review.util';
import { isAdvisorDocumentReviewVisible } from './loan-document-advisor-review';

export interface LoanProgressInfo {
  stepLabel: string;
  stepIndex: number;
  totalSteps: number;
  percent: number;
  estimatedDecisionLabel: string | null;
}

export interface LoanTimelineEvent {
  title: string;
  dateLabel: string;
  description: string;
  state: 'done' | 'current' | 'upcoming' | 'warn';
}

/** Le client ne peut déposer un complément que pendant l’analyse conseiller. */
export function isDocumentComplementAllowed(status: LoanApplicationStatus): boolean {
  return status === 'UNDER_REVIEW';
}

/** Le demandeur peut retirer sa demande avant décision finale. */
export function canClientCancelLoan(status: LoanApplicationStatus): boolean {
  return status === 'SUBMITTED' || status === 'UNDER_REVIEW';
}

export interface LoanDetailDocumentRow {
  type: LoanDocumentType;
  label: string;
  required: boolean;
  files: LoanDocumentResponseDto[];
  status: 'provided' | 'missing' | 'optional';
  advisorReview?: LoanDocumentAdvisorReview | null;
}

export function loanDetailProgress(loan: LoanResponseDto): LoanProgressInfo {
  const totalSteps = 5;
  let stepIndex = 1;
  let stepLabel = 'Dossier reçu';
  let percent = 20;

  switch (loan.status) {
    case 'SUBMITTED':
      stepIndex = 2;
      stepLabel = 'En attente d\'affectation';
      percent = 40;
      break;
    case 'UNDER_REVIEW':
      stepIndex = 3;
      stepLabel = 'Analyse financière';
      percent = 60;
      break;
    case 'APPROVED':
    case 'REJECTED':
      stepIndex = 4;
      stepLabel = 'Décision rendue';
      percent = 100;
      break;
    case 'CANCELLED':
      stepIndex = 1;
      stepLabel = 'Demande annulée';
      percent = 0;
      break;
    default:
      break;
  }

  let estimatedDecisionLabel: string | null = null;
  if (loan.submittedAt && (loan.status === 'SUBMITTED' || loan.status === 'UNDER_REVIEW')) {
    const d = new Date(loan.submittedAt);
    d.setDate(d.getDate() + 10);
    estimatedDecisionLabel = formatDateFrLong(d.toISOString());
  }

  return {
    stepLabel,
    stepIndex,
    totalSteps,
    percent,
    estimatedDecisionLabel,
  };
}

export function historyEventsToTimeline(events: LoanHistoryEventResponseDto[]): LoanTimelineEvent[] {
  return events.map((e) => ({
    title: e.title,
    dateLabel: e.occurredAt ? formatDateFrShort(e.occurredAt) : '—',
    description: e.description,
    state: e.state,
  }));
}

export function loanDetailTimeline(loan: LoanResponseDto, docsProvided: number, docsRequired: number): LoanTimelineEvent[] {
  const events: LoanTimelineEvent[] = [];

  if (loan.submittedAt) {
    events.push({
      title: 'Demande soumise',
      dateLabel: formatDateFrShort(loan.submittedAt),
      description: `Votre dossier a été reçu et enregistré. Référence : #${loan.reference}.`,
      state: 'done',
    });
  }

  if (loan.status !== 'CANCELLED' && loan.submittedAt) {
    const missing = docsRequired - docsProvided;
    events.push({
      title: missing > 0 ? 'Documents en attente' : 'Documents fournis',
      dateLabel: formatDateFrShort(loan.updatedAt),
      description:
        missing > 0
          ? `${docsProvided}/${docsRequired} documents obligatoires reçus. ${missing} pièce(s) manquante(s).`
          : `${docsProvided}/${docsRequired} documents obligatoires ont été déposés.`,
      state: missing > 0 ? 'warn' : 'done',
    });
  }

  if (loan.status === 'UNDER_REVIEW') {
    events.push({
      title: 'Analyse financière',
      dateLabel: formatDateFrShort(loan.updatedAt),
      description:
        'Votre dossier est en cours d\'analyse par nos conseillers. Durée estimée : 2 à 5 jours ouvrés.',
      state: 'current',
    });
    events.push({
      title: 'Décision du comité',
      dateLabel: 'À venir',
      description: 'Le comité de crédit rendra sa décision après analyse complète du dossier.',
      state: 'upcoming',
    });
    events.push({
      title: 'Déblocage des fonds',
      dateLabel: 'À venir',
      description: 'En cas d\'approbation, les fonds seront disponibles sous 24 à 48 heures.',
      state: 'upcoming',
    });
  } else if (loan.status === 'SUBMITTED') {
    events.push({
      title: 'Analyse du dossier',
      dateLabel: 'À venir',
      description: 'Un conseiller va examiner votre dossier sous 2 à 5 jours ouvrés.',
      state: 'upcoming',
    });
  } else if (loan.status === 'APPROVED' && loan.decidedAt) {
    events.push({
      title: 'Demande approuvée',
      dateLabel: formatDateFrShort(loan.decidedAt),
      description: loan.decisionComment ?? 'Votre demande a été acceptée.',
      state: 'done',
    });
    events.push({
      title: 'Déblocage des fonds',
      dateLabel: 'À venir',
      description: 'Les fonds seront disponibles sous 24 à 48 heures.',
      state: 'current',
    });
  } else if (loan.status === 'REJECTED' && loan.decidedAt) {
    events.push({
      title: 'Demande refusée',
      dateLabel: formatDateFrShort(loan.decidedAt),
      description: loan.decisionComment ?? 'Votre demande n\'a pas pu être acceptée.',
      state: 'warn',
    });
  } else if (loan.status === 'CANCELLED') {
    events.push({
      title: 'Demande annulée',
      dateLabel: formatDateFrShort(loan.decidedAt ?? loan.updatedAt),
      description: loan.decisionComment ?? 'Cette demande a été annulée.',
      state: 'warn',
    });
  }

  return events;
}

export function loanDetailDocuments(
  loan: LoanResponseDto,
  docs: LoanDocumentResponseDto[],
  historyEvents: LoanHistoryEventResponseDto[] = []
): LoanDetailDocumentRow[] {
  const showAdvisor = isAdvisorDocumentReviewVisible(loan.status);
  const historyReviews = advisorReviewsFromHistory(historyEvents, docs, loan.status);

  return DOCUMENT_SLOTS.map((slot) => {
    const files = docs.filter((d) => d.documentType === slot.type);
    let status: LoanDetailDocumentRow['status'] = 'optional';
    if (files.length > 0) status = 'provided';
    else if (slot.required) status = 'missing';

    const advisorReview = resolveAdvisorReview(slot.type, historyReviews, showAdvisor);

    return {
      type: slot.type,
      label: slot.label,
      required: slot.required,
      files,
      status,
      advisorReview,
    };
  });
}

export function countDocumentsNeedingClientAction(rows: LoanDetailDocumentRow[]): number {
  return rows.filter((r) => {
    const s = r.advisorReview?.status;
    return s === 'rejected' || s === 'missing_upload';
  }).length;
}

export {
  isAdvisorDocumentReviewVisible,
  advisorReviewStatusLabel,
  advisorReviewStatusIcon,
} from './loan-document-advisor-review';
export type {
  LoanDocumentAdvisorReviewStatus,
  LoanDocumentAdvisorReview,
} from './loan-detail-document-review.util';

export function loanStatusDisplayLabel(status: LoanApplicationStatus): string {
  return loanCardStatusStyle(status).label;
}

function formatDateFrShort(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(d);
}

export function formatDocSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

export function docDisplayName(doc: LoanDocumentResponseDto): string {
  return doc.displayName?.trim() || doc.originalFileName;
}
