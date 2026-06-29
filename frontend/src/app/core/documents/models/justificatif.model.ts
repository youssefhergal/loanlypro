import {
  DocumentValidationStatus,
  LoanDocumentType,
} from './document.enums';

export interface JustificatifItemDto {
  documentId: number;
  documentType: LoanDocumentType;
  documentTypeLabel: string;
  fileName: string;
  uploadedAt: string;
  validationStatus: DocumentValidationStatus;
  rejectionReason: string | null;
  downloadable: boolean;
}

export interface JustificatifGroupDto {
  loanApplicationId: number;
  loanReference: string;
  loanStatus: string;
  submittedAt: string | null;
  updatedAt: string | null;
  documents: JustificatifItemDto[];
}
