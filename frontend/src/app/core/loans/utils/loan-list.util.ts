import { LoanApplicationStatus, LoanPurpose } from '../models/loan.enums';
import { LoanResponseDto } from '../models/loan-response.model';
import { LOAN_PURPOSE_OPTIONS } from '../constants/loan.constants';

export type LoanListSort = 'updatedDesc' | 'updatedAsc' | 'amountDesc' | 'amountAsc';

export type LoanListStatusFilter = 'ALL' | LoanApplicationStatus;

export interface LoanListStatusTab {
  key: LoanListStatusFilter;
  label: string;
}

export const LOAN_LIST_STATUS_TABS: LoanListStatusTab[] = [
  { key: 'ALL', label: 'Toutes' },
  { key: 'DRAFT', label: 'Brouillon' },
  { key: 'SUBMITTED', label: 'Soumise' },
  { key: 'UNDER_REVIEW', label: 'En analyse' },
  { key: 'OFFER_PENDING', label: 'Offre en attente' },
  { key: 'APPROVED', label: 'Approuvée' },
  { key: 'REJECTED', label: 'Rejetée' },
  { key: 'CANCELLED', label: 'Annulée' },
];

/** Onglets liste conseiller — sans brouillon client. */
export const ADVISOR_LOAN_LIST_STATUS_TABS: LoanListStatusTab[] = LOAN_LIST_STATUS_TABS.filter(
  (tab) => tab.key !== 'DRAFT'
);

export interface LoanCardStatusStyle {
  label: string;
  accent: string;
  iconBg: string;
  badgeBg: string;
  badgeColor: string;
  icon: string;
}

const STATUS_STYLES: Record<LoanApplicationStatus, LoanCardStatusStyle> = {
  DRAFT: {
    label: 'Brouillon',
    accent: '#78909c',
    iconBg: '#eceff1',
    badgeBg: '#eceff1',
    badgeColor: '#455a64',
    icon: 'edit_note',
  },
  SUBMITTED: {
    label: 'En attente',
    accent: '#7c4dff',
    iconBg: '#ede7f6',
    badgeBg: '#ede7f6',
    badgeColor: '#5e35b1',
    icon: 'description',
  },
  UNDER_REVIEW: {
    label: 'En analyse',
    accent: '#1e88e5',
    iconBg: '#e3f2fd',
    badgeBg: '#e3f2fd',
    badgeColor: '#1565c0',
    icon: 'analytics',
  },
  OFFER_PENDING: {
    label: 'Offre en attente',
    accent: '#e65100',
    iconBg: '#fff3e0',
    badgeBg: '#fff3e0',
    badgeColor: '#e65100',
    icon: 'mail',
  },
  APPROVED: {
    label: 'Approuvée',
    accent: '#43a047',
    iconBg: '#e8f5e9',
    badgeBg: '#e8f5e9',
    badgeColor: '#2e7d32',
    icon: 'check_circle',
  },
  REJECTED: {
    label: 'Rejetée',
    accent: '#e53935',
    iconBg: '#ffebee',
    badgeBg: '#ffebee',
    badgeColor: '#c62828',
    icon: 'cancel',
  },
  CANCELLED: {
    label: 'Annulée',
    accent: '#757575',
    iconBg: '#f5f5f5',
    badgeBg: '#f5f5f5',
    badgeColor: '#616161',
    icon: 'block',
  },
};

const PURPOSE_ICONS: Record<LoanPurpose, string> = {
  GREEN: 'energy_savings_leaf',
  SOFTWARE: 'computer',
  VEHICLE: 'directions_car',
  HOME_IMPROVEMENT: 'home_repair_service',
  PERSONAL: 'person',
  EDUCATION: 'school',
  OTHER: 'category',
};

export function loanCardStatusStyle(status: LoanApplicationStatus): LoanCardStatusStyle {
  return STATUS_STYLES[status];
}

export function loanPurposeLabel(purpose: LoanPurpose): string {
  return LOAN_PURPOSE_OPTIONS.find((p) => p.value === purpose)?.label ?? purpose;
}

export function loanPurposeIcon(purpose: LoanPurpose): string {
  return PURPOSE_ICONS[purpose] ?? 'category';
}

export function formatRelativeTimeFr(isoDate: string | null | undefined): string {
  if (!isoDate) return '';
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return '';
  const now = Date.now();
  const diffMs = now - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return "à l'instant";
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `il y a ${diffMin} minute${diffMin > 1 ? 's' : ''}`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `il y a ${diffHours} heure${diffHours > 1 ? 's' : ''}`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 30) return `il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
  const diffMonths = Math.floor(diffDays / 30);
  if (diffMonths < 12) return `il y a ${diffMonths} mois`;
  const diffYears = Math.floor(diffMonths / 12);
  return `il y a ${diffYears} an${diffYears > 1 ? 's' : ''}`;
}

export function formatDateFrLong(isoDate: string | null | undefined): string {
  if (!isoDate) return '';
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(date);
}

export function loanCardMetaLine(loan: LoanResponseDto): string {
  const parts: string[] = [];

  if (loan.status === 'DRAFT') {
    const rel = formatRelativeTimeFr(loan.updatedAt);
    if (rel) parts.push(`Modifié ${rel}`);
    parts.push('Reprendre la saisie →');
    return parts.join(' · ');
  }

  if (loan.submittedAt) {
    parts.push(`Soumise le ${formatDateFrLong(loan.submittedAt)}`);
  }

  if (loan.status === 'APPROVED' && loan.decidedAt) {
    parts.push(`Approuvée le ${formatDateFrLong(loan.decidedAt)}`);
  } else if (loan.status === 'REJECTED' && loan.decidedAt) {
    if (loan.decisionComment?.trim()) {
      parts.push(loan.decisionComment.trim());
    } else {
      parts.push(`Refusée le ${formatDateFrLong(loan.decidedAt)}`);
    }
  } else if (loan.advisorName?.trim()) {
    const role =
      loan.status === 'UNDER_REVIEW' ? 'Analyste' : 'Validateur';
    parts.push(`${role} : ${loan.advisorName.trim()}`);
  } else if (loan.status === 'SUBMITTED') {
    parts.push("En attente d'affectation");
  }

  return parts.join(' · ');
}

export function loanCardRelativeTime(loan: LoanResponseDto): string {
  const ref =
    loan.status === 'DRAFT'
      ? loan.updatedAt
      : loan.submittedAt ?? loan.updatedAt ?? loan.createdAt;
  return formatRelativeTimeFr(ref);
}

export function matchesLoanSearch(loan: LoanResponseDto, query: string): boolean {
  const q = query.trim().toLowerCase();
  if (!q) return true;
  return (
    loan.reference.toLowerCase().includes(q) ||
    loan.title.toLowerCase().includes(q) ||
    loanPurposeLabel(loan.loanPurpose).toLowerCase().includes(q)
  );
}

export function matchesAdvisorLoanSearch(loan: LoanResponseDto, query: string): boolean {
  if (matchesLoanSearch(loan, query)) return true;
  const q = query.trim().toLowerCase();
  if (!q) return true;
  return (loan.applicantName?.toLowerCase().includes(q) ?? false);
}

export function sortLoans(loans: LoanResponseDto[], sort: LoanListSort): LoanResponseDto[] {
  const copy = [...loans];
  copy.sort((a, b) => {
    switch (sort) {
      case 'updatedAsc':
        return (
          new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime()
        );
      case 'amountDesc':
        return Number(b.requestedAmount) - Number(a.requestedAmount);
      case 'amountAsc':
        return Number(a.requestedAmount) - Number(b.requestedAmount);
      case 'updatedDesc':
      default:
        return (
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        );
    }
  });
  return copy;
}

export function countByStatus(
  loans: LoanResponseDto[],
  status: LoanListStatusFilter
): number {
  if (status === 'ALL') return loans.length;
  return loans.filter((l) => l.status === status).length;
}

export function matchesAdminLoanSearch(loan: LoanResponseDto, query: string): boolean {
  if (matchesAdvisorLoanSearch(loan, query)) return true;
  const q = query.trim().toLowerCase();
  if (!q) return true;
  return (
    (loan.applicantEmail?.toLowerCase().includes(q) ?? false) ||
    (loan.advisorName?.toLowerCase().includes(q) ?? false)
  );
}

export function advisorInitials(name: string | null | undefined): string {
  if (!name?.trim()) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function formatAdminListSubmittedAt(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

export function formatAdminListUpdatedAt(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfYesterday = new Date(startOfToday);
  startOfYesterday.setDate(startOfYesterday.getDate() - 1);
  const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

  if (startOfDate.getTime() === startOfToday.getTime()) {
    const rel = formatRelativeTimeFr(iso);
    return rel || "Aujourd'hui";
  }
  if (startOfDate.getTime() === startOfYesterday.getTime()) {
    return 'Hier';
  }
  return formatAdminListSubmittedAt(iso);
}
