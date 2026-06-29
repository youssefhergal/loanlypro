import { IssuedDocumentType } from './document.enums';

export interface CreditDocumentDto {
  documentType: IssuedDocumentType;
  title: string;
  loanApplicationId: number | null;
  loanId: number | null;
  reference: string;
  issuedAt: string | null;
  available: boolean;
  unavailableReason: string | null;
}
