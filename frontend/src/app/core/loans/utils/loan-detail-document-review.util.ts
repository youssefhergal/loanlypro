import { LoanApplicationStatus, LoanDocumentType } from '../models/loan.enums';
import { LoanDocumentResponseDto } from '../models/loan-document.model';
import { LoanHistoryEventResponseDto } from '../models/loan-history.model';
import { DOCUMENT_SLOTS } from '../constants/loan.constants';
import {
  LoanDocumentAdvisorReview,
  LoanDocumentAdvisorReviewStatus,
} from './loan-document-advisor-review';

/**
 * Dérive les statuts conseiller par type de pièce à partir du journal d'historique.
 * Le dernier événement documentaire l'emporte pour chaque type.
 */
export function advisorReviewsFromHistory(
  events: LoanHistoryEventResponseDto[],
  docs: LoanDocumentResponseDto[],
  loanStatus: LoanApplicationStatus
): Partial<Record<LoanDocumentType, LoanDocumentAdvisorReview>> {
  const byType: Partial<Record<LoanDocumentType, LoanDocumentAdvisorReview>> = {};

  for (const event of events) {
    if (!event.documentType || !event.eventType) {
      continue;
    }
    const type = event.documentType;

    switch (event.eventType) {
      case 'DOCUMENT_REJECTED':
        byType[type] = {
          status: 'rejected',
          comment: event.comment?.trim() || undefined,
        };
        break;
      case 'DOCUMENT_VALIDATED':
        byType[type] = { status: 'validated' };
        break;
      case 'DOCUMENT_UPLOADED':
        if (event.complement) {
          byType[type] = { status: 'pending_review' };
        }
        break;
    }
  }

  for (const slot of DOCUMENT_SLOTS) {
    if (byType[slot.type]) {
      continue;
    }
    const hasFile = docs.some((d) => d.documentType === slot.type);
    if (!slot.required && !hasFile) {
      continue;
    }
    byType[slot.type] = defaultReviewForSlot(slot.type, slot.required, hasFile, loanStatus);
  }

  return byType;
}

function defaultReviewForSlot(
  _type: LoanDocumentType,
  required: boolean,
  hasFile: boolean,
  loanStatus: LoanApplicationStatus
): LoanDocumentAdvisorReview {
  if (!hasFile) {
    return {
      status: 'missing_upload',
      comment: 'Document obligatoire non fourni.',
    };
  }

  if (loanStatus === 'APPROVED') {
    return { status: 'validated' };
  }

  if (loanStatus === 'SUBMITTED' || loanStatus === 'UNDER_REVIEW') {
    return { status: 'pending_review' };
  }

  if (loanStatus === 'REJECTED' || loanStatus === 'CANCELLED') {
    return { status: 'validated' };
  }

  return { status: 'pending_review' };
}

export function resolveAdvisorReview(
  documentType: LoanDocumentType,
  historyReviews: Partial<Record<LoanDocumentType, LoanDocumentAdvisorReview>>,
  showAdvisor: boolean
): LoanDocumentAdvisorReview | null {
  if (!showAdvisor) {
    return null;
  }
  return historyReviews[documentType] ?? null;
}

export type { LoanDocumentAdvisorReviewStatus, LoanDocumentAdvisorReview };
