import { STATUS_LABELS } from '../../loans/constants/loan.constants';
import { LoanApplicationStatus } from '../../loans/models/loan.enums';
import { formatRelativeTimeFr } from '../../loans/utils/loan-list.util';
import {
  DocumentValidationStatus,
  LoanDocumentType,
} from '../models/document.enums';
import { JustificatifGroupDto } from '../models/justificatif.model';

export function formatDateFrShort(isoDate: string | null | undefined): string {
  if (!isoDate) {
    return '';
  }
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

export function formatUploadedAt(isoDate: string | null | undefined): string {
  const relative = formatRelativeTimeFr(isoDate);
  const absolute = formatDateFrShort(isoDate);
  if (relative && absolute) {
    return `${relative} (${absolute})`;
  }
  return relative || absolute || '—';
}

export function loanStatusLabel(status: string): string {
  const entry = STATUS_LABELS[status as LoanApplicationStatus];
  return entry?.label ?? status;
}

export function loanStatusChipClass(status: string): string {
  const entry = STATUS_LABELS[status as LoanApplicationStatus];
  return entry?.chipClass ?? 'status-default';
}

export function groupSubtitle(group: JustificatifGroupDto): string {
  if (group.loanStatus === 'DRAFT') {
    const updated = formatDateFrShort(group.updatedAt);
    return updated ? `Demande de prêt • Modifiée le ${updated}` : 'Demande de prêt • Brouillon';
  }

  const submitted = formatDateFrShort(group.submittedAt);
  return submitted
    ? `Demande de prêt • Déposée le ${submitted}`
    : 'Demande de prêt';
}

export function documentTypeIcon(type: LoanDocumentType): string {
  switch (type) {
    case 'IDENTITY':
      return 'badge';
    case 'PAYSLIPS':
      return 'payments';
    case 'TAX_NOTICE':
      return 'receipt_long';
    case 'BANK_STATEMENTS':
      return 'account_balance';
    case 'PROOF_OF_ADDRESS':
      return 'home';
    default:
      return 'insert_drive_file';
  }
}

export function validationStatusLabel(status: DocumentValidationStatus): string {
  switch (status) {
    case 'VALIDATED':
      return 'Validé';
    case 'REJECTED':
      return 'Refusé';
    default:
      return 'En attente';
  }
}

export function validationStatusClass(status: DocumentValidationStatus): string {
  switch (status) {
    case 'VALIDATED':
      return 'doc-status--validated';
    case 'REJECTED':
      return 'doc-status--rejected';
    default:
      return 'doc-status--pending';
  }
}

export function documentCountLabel(count: number): string {
  return `${count} document${count > 1 ? 's' : ''}`;
}
