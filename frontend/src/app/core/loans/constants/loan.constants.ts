import { LoanApplicationStatus, LoanDocumentType, LoanPurpose } from '../models/loan.enums';

export const LOAN_AMOUNT_MIN = 1000;
export const LOAN_AMOUNT_MAX = 200_000;
export const LOAN_DURATION_MIN = 12;
export const LOAN_DURATION_MAX = 240;
export const LOAN_INTEREST_RATE = 0.0385;
export const LOAN_DEBT_RATIO_MAX = 0.33;
export const LOAN_COMMENT_MAX_LENGTH = 500;
export const LOAN_FILE_MAX_BYTES = 10 * 1024 * 1024;
export const LOAN_ALLOWED_MIME_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];

export const LOAN_PURPOSE_OPTIONS: { value: LoanPurpose; label: string }[] = [
  { value: 'SOFTWARE', label: 'Logiciel / équipement pro' },
  { value: 'VEHICLE', label: 'Véhicule' },
  { value: 'HOME_IMPROVEMENT', label: 'Travaux / aménagement' },
  { value: 'PERSONAL', label: 'Projet personnel' },
  { value: 'EDUCATION', label: 'Formation / études' },
  { value: 'OTHER', label: 'Autre' },
];

export const EMPLOYMENT_OPTIONS: { value: string; label: string; requiresEmployer: boolean }[] = [
  { value: 'CDI', label: 'CDI', requiresEmployer: true },
  { value: 'CDD', label: 'CDD', requiresEmployer: true },
  { value: 'FREELANCE', label: 'Indépendant', requiresEmployer: false },
  { value: 'PUBLIC_SECTOR', label: 'Fonctionnaire', requiresEmployer: true },
  { value: 'RETIRED', label: 'Retraité', requiresEmployer: false },
  { value: 'UNEMPLOYED', label: 'Sans emploi', requiresEmployer: false },
  { value: 'STUDENT', label: 'Étudiant', requiresEmployer: false },
];

export const DOCUMENT_SLOTS: {
  type: LoanDocumentType;
  label: string;
  required: boolean;
}[] = [
  { type: 'IDENTITY', label: "Pièce d'identité", required: true },
  { type: 'PAYSLIPS', label: 'Bulletins de salaire', required: true },
  { type: 'TAX_NOTICE', label: "Avis d'imposition", required: true },
  { type: 'BANK_STATEMENTS', label: 'Relevés bancaires', required: true },
  { type: 'PROOF_OF_ADDRESS', label: 'Justificatif de domicile', required: true },
  { type: 'OTHER', label: 'Autre document', required: false },
];

export const STATUS_LABELS: Record<
  LoanApplicationStatus,
  { label: string; chipClass: string }
> = {
  DRAFT: { label: 'Brouillon', chipClass: 'status-draft' },
  SUBMITTED: { label: 'Soumise', chipClass: 'status-submitted' },
  UNDER_REVIEW: { label: 'En étude', chipClass: 'status-review' },
  APPROVED: { label: 'Acceptée', chipClass: 'status-approved' },
  REJECTED: { label: 'Refusée', chipClass: 'status-rejected' },
  CANCELLED: { label: 'Annulée', chipClass: 'status-cancelled' },
};

export const WIZARD_STORAGE_KEY = 'loanly_wizard_draft';
