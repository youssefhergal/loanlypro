export type LoanApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'OFFER_PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED';

export type EmploymentStatus =
  | 'CDI'
  | 'CDD'
  | 'FREELANCE'
  | 'PUBLIC_SECTOR'
  | 'UNEMPLOYED'
  | 'RETIRED'
  | 'STUDENT';

export type LoanPurpose =
  | 'SOFTWARE'
  | 'VEHICLE'
  | 'HOME_IMPROVEMENT'
  | 'PERSONAL'
  | 'EDUCATION'
  | 'OTHER';

export type LoanDocumentType =
  | 'IDENTITY'
  | 'PAYSLIPS'
  | 'TAX_NOTICE'
  | 'BANK_STATEMENTS'
  | 'PROOF_OF_ADDRESS'
  | 'OTHER';
