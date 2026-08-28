import { LoanApplicationStatus, LoanDocumentType, LoanPurpose } from '../models/loan.enums';

export const LOAN_AMOUNT_MIN = 1000;
export const LOAN_AMOUNT_MAX = 200_000;
export const LOAN_DURATION_MIN = 12;
export const LOAN_DURATION_MAX = 240;
/** Taux de repli si montant/durée/projet indisponibles (véhicule 15 k€ / 48 mois). */
export const LOAN_INTEREST_RATE = 0.0385;
export const LOAN_DEBT_RATIO_MAX = 0.33;
/** Seuil affiché dans le simulateur d'endettement (indicatif banque). */
export const LOAN_DEBT_RATIO_DISPLAY_MAX = 0.35;
export const LOAN_COMMENT_MAX_LENGTH = 500;
export const LOAN_FILE_MAX_BYTES = 10 * 1024 * 1024;
export const OTHER_DOCUMENT_MAX_COUNT = 2;
export const OTHER_DOCUMENT_LABEL_MAX_LENGTH = 120;
export const LOAN_ALLOWED_MIME_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];

export const LOAN_PURPOSE_OPTIONS: { value: LoanPurpose; label: string; hint?: string }[] = [
  { value: 'GREEN', label: 'Crédit vert / éco', hint: 'Taux préférentiel pour projets durables' },
  { value: 'SOFTWARE', label: 'Logiciel / équipement pro' },
  { value: 'VEHICLE', label: 'Véhicule' },
  { value: 'HOME_IMPROVEMENT', label: 'Travaux / aménagement' },
  { value: 'PERSONAL', label: 'Projet personnel' },
  { value: 'EDUCATION', label: 'Formation / études' },
  { value: 'OTHER', label: 'Autre' },
];

export const EMPLOYER_SECTOR_OPTIONS: { value: string; label: string }[] = [
  { value: 'BANKING', label: 'Banque / Finance' },
  { value: 'IT', label: 'Informatique / Tech' },
  { value: 'HEALTH', label: 'Santé' },
  { value: 'EDUCATION', label: 'Enseignement' },
  { value: 'RETAIL', label: 'Commerce / Distribution' },
  { value: 'INDUSTRY', label: 'Industrie' },
  { value: 'PUBLIC', label: 'Secteur public' },
  { value: 'OTHER', label: 'Autre' },
];

export const SENIORITY_RANGE_OPTIONS: {
  value: string;
  label: string;
  months: number;
}[] = [
  { value: 'LT_1', label: 'Moins de 1 an', months: 6 },
  { value: '1_3', label: '1 – 3 ans', months: 24 },
  { value: '3_5', label: '3 – 5 ans', months: 48 },
  { value: '5_10', label: '5 – 10 ans', months: 84 },
  { value: 'GT_10', label: 'Plus de 10 ans', months: 132 },
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
  OFFER_PENDING: { label: 'Offre en attente', chipClass: 'status-offer-pending' },
  APPROVED: { label: 'Acceptée', chipClass: 'status-approved' },
  REJECTED: { label: 'Refusée', chipClass: 'status-rejected' },
  CANCELLED: { label: 'Annulée', chipClass: 'status-cancelled' },
};

export const WIZARD_STORAGE_KEY = 'loanly_wizard_draft';

export function seniorityMonthsFromRange(range: string | null | undefined): number | null {
  if (!range) return null;
  return SENIORITY_RANGE_OPTIONS.find((o) => o.value === range)?.months ?? null;
}

export function seniorityRangeFromMonths(months: number | null | undefined): string {
  if (months == null || months < 0) return '';
  if (months < 12) return 'LT_1';
  if (months < 36) return '1_3';
  if (months < 60) return '3_5';
  if (months < 120) return '5_10';
  return 'GT_10';
}
