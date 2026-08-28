import { IssuedDocumentType } from '../models/document.enums';
import { formatDateFrShort } from './justificatif-display.util';

export function creditDocumentIcon(type: IssuedDocumentType): string {
  switch (type) {
    case 'APPLICATION_RECAP':
      return 'summarize';
    case 'OFFER':
      return 'local_offer';
    case 'LOAN_CONTRACT':
      return 'gavel';
    case 'SEPA_MANDATE':
      return 'account_balance';
    default:
      return 'description';
  }
}

export function formatIssuedAt(isoDate: string | null | undefined): string {
  return formatDateFrShort(isoDate) || '—';
}

export function creditDocumentFileName(doc: {
  documentType: IssuedDocumentType;
  reference: string;
}): string {
  const prefix = {
    APPLICATION_RECAP: 'recapitulatif',
    OFFER: 'offre',
    LOAN_CONTRACT: 'contrat',
    SEPA_MANDATE: 'mandat-sepa',
  }[doc.documentType];
  return `${prefix}-${doc.reference}.pdf`;
}

export function creditDocumentStatusLabel(available: boolean): string {
  return available ? 'Disponible' : 'Indisponible';
}

export function creditDocumentStatusClass(available: boolean): string {
  return available ? 'doc-badge doc-badge--ok' : 'doc-badge doc-badge--muted';
}
