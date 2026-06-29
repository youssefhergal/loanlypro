export type DocumentValidationStatus = 'PENDING' | 'VALIDATED' | 'REJECTED';

export type IssuedDocumentType =
  | 'APPLICATION_RECAP'
  | 'OFFER'
  | 'LOAN_CONTRACT'
  | 'SEPA_MANDATE';

export type LoanDocumentType =
  | 'IDENTITY'
  | 'PAYSLIPS'
  | 'TAX_NOTICE'
  | 'BANK_STATEMENTS'
  | 'PROOF_OF_ADDRESS'
  | 'OTHER';
