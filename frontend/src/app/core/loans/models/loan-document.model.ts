import { LoanDocumentType } from './loan.enums';

export interface LoanDocumentResponseDto {
  id: number;
  loanApplicationId: number;
  documentType: LoanDocumentType;
  originalFileName: string;
  displayName?: string | null;
  contentType: string;
  fileSizeBytes: number;
  uploadedAt: string;
}
