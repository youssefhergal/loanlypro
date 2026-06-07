import { LoanDocumentType } from './loan.enums';
import { LoanDocumentAdvisorReviewStatus } from '../utils/loan-document-advisor-review';

export interface LoanDocumentReviewResponseDto {
  documentType: LoanDocumentType;
  status: LoanDocumentAdvisorReviewStatus;
  comment?: string | null;
  updatedAt?: string | null;
}
