import { LoanApplicationStatus } from '../models/loan.enums';

/** Statut conseiller affiché sur la page détail. */
export type LoanDocumentAdvisorReviewStatus =
  | 'pending_review'
  | 'validated'
  | 'rejected'
  | 'missing_upload';

export interface LoanDocumentAdvisorReview {
  status: LoanDocumentAdvisorReviewStatus;
  comment?: string;
}

const REVIEWABLE_STATUSES: LoanApplicationStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
];

export function isAdvisorDocumentReviewVisible(status: LoanApplicationStatus): boolean {
  return REVIEWABLE_STATUSES.includes(status);
}

export function advisorReviewStatusLabel(status: LoanDocumentAdvisorReviewStatus): string {
  switch (status) {
    case 'validated':
      return 'Validé';
    case 'pending_review':
      return 'En cours de validation';
    case 'rejected':
      return 'À remplacer';
    case 'missing_upload':
      return 'À ajouter';
  }
}

export function advisorReviewStatusIcon(status: LoanDocumentAdvisorReviewStatus): string {
  switch (status) {
    case 'validated':
      return 'verified';
    case 'pending_review':
      return 'hourglass_top';
    case 'rejected':
      return 'error_outline';
    case 'missing_upload':
      return 'upload_file';
  }
}
