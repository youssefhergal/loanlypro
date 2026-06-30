import { InstallmentStatus, RepaymentLoanStatus } from '../models/repayment.enums';

export interface StatusChipStyle {
  label: string;
  accent: string;
  badgeBg: string;
  badgeColor: string;
}

/** Libellés liste conseiller (consultation prêts). */
export const ADVISOR_REPAYMENT_LOAN_STATUS_STYLES: Record<RepaymentLoanStatus, StatusChipStyle> = {
  PENDING_MANDATE: {
    label: 'Mandat en attente',
    accent: '#e65100',
    badgeBg: '#fff3e0',
    badgeColor: '#e65100',
  },
  ACTIVE: {
    label: 'Actif',
    accent: '#1a7a4a',
    badgeBg: '#e8f5ee',
    badgeColor: '#1a7a4a',
  },
  DEFAULTED: {
    label: 'En défaut',
    accent: '#c62828',
    badgeBg: '#ffebee',
    badgeColor: '#c62828',
  },
  CLOSED: {
    label: 'Soldé',
    accent: '#546e7a',
    badgeBg: '#eceff1',
    badgeColor: '#455a64',
  },
};

export const REPAYMENT_LOAN_STATUS_STYLES: Record<RepaymentLoanStatus, StatusChipStyle> = {
  PENDING_MANDATE: {
    label: 'Mandat à configurer',
    accent: '#e65100',
    badgeBg: '#fff3e0',
    badgeColor: '#e65100',
  },
  ACTIVE: {
    label: 'En cours',
    accent: '#1a7a4a',
    badgeBg: '#e8f5ee',
    badgeColor: '#1a7a4a',
  },
  DEFAULTED: {
    label: 'En défaut',
    accent: '#c62828',
    badgeBg: '#ffebee',
    badgeColor: '#c62828',
  },
  CLOSED: {
    label: 'Soldé',
    accent: '#546e7a',
    badgeBg: '#eceff1',
    badgeColor: '#455a64',
  },
};

export const INSTALLMENT_STATUS_STYLES: Record<InstallmentStatus, StatusChipStyle> = {
  UPCOMING: {
    label: 'À venir',
    accent: '#1565c0',
    badgeBg: '#e3f2fd',
    badgeColor: '#1565c0',
  },
  PAID: {
    label: 'Payée',
    accent: '#1a7a4a',
    badgeBg: '#e8f5ee',
    badgeColor: '#1a7a4a',
  },
  FAILED: {
    label: 'Échec',
    accent: '#e65100',
    badgeBg: '#fff3e0',
    badgeColor: '#e65100',
  },
  BLOCKED: {
    label: 'Bloquée',
    accent: '#6d4c41',
    badgeBg: '#efebe9',
    badgeColor: '#6d4c41',
  },
  OVERDUE: {
    label: 'En retard',
    accent: '#c62828',
    badgeBg: '#ffebee',
    badgeColor: '#c62828',
  },
};

export const MANDATE_CONSENT_TEXT =
  'J’autorise LoanlyPro à prélever automatiquement le montant de chaque échéance sur le compte indiqué ci-dessus conformément au mandat de prélèvement SEPA.';

export const MANDATE_CONSENT_FINE_PRINT =
  'En signant ce mandat, vous autorisez LoanlyPro à envoyer des instructions à votre banque pour débiter votre compte, et votre banque à débiter votre compte conformément aux instructions de LoanlyPro.';

export function advisorRepaymentLoanStatusStyle(
  status: RepaymentLoanStatus | string,
): StatusChipStyle {
  return (
    ADVISOR_REPAYMENT_LOAN_STATUS_STYLES[status as RepaymentLoanStatus] ?? {
      label: status,
      accent: '#546e7a',
      badgeBg: '#eceff1',
      badgeColor: '#455a64',
    }
  );
}

export function repaymentLoanStatusStyle(
  status: RepaymentLoanStatus | string,
): StatusChipStyle {
  return (
    REPAYMENT_LOAN_STATUS_STYLES[status as RepaymentLoanStatus] ?? {
      label: status,
      accent: '#546e7a',
      badgeBg: '#eceff1',
      badgeColor: '#455a64',
    }
  );
}

export function installmentStatusStyle(status: InstallmentStatus | string): StatusChipStyle {
  return (
    INSTALLMENT_STATUS_STYLES[status as InstallmentStatus] ?? {
      label: status,
      accent: '#546e7a',
      badgeBg: '#eceff1',
      badgeColor: '#455a64',
    }
  );
}

export function needsMandateSetup(
  loanStatus: RepaymentLoanStatus | string,
  mandateStatus: string | null | undefined,
): boolean {
  return (
    loanStatus === 'PENDING_MANDATE' ||
    mandateStatus === 'NONE' ||
    mandateStatus === 'REVOKED' ||
    !mandateStatus
  );
}
