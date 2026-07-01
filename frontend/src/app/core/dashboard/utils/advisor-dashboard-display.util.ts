import { LoanApplicationSummaryDto } from '../services/dashboard-api.service';
import { LoanSummaryDto } from '../../loans/repayment/models/loan-summary.model';
import {
  applicationProgressSteps,
  applicationStatusClass,
  applicationStatusIcon,
  applicationStatusLabel,
  HeroCarouselSlide,
  showApplicationStepper,
} from './client-dashboard-display.util';

export {
  applicationProgressSteps,
  applicationStatusClass,
  applicationStatusIcon,
  applicationStatusLabel,
  showApplicationStepper,
};

const ACTIVE_APPLICATION_STATUSES = ['SUBMITTED', 'UNDER_REVIEW', 'OFFER_PENDING'] as const;

const DEMO_APPLICATION_HINTS: Record<string, string> = {
  'LF-DEMO-B001': '2 documents refusés — en attente client',
};

export interface AdvisorPriorityAlert {
  reference: string;
  message: string;
  applicationId: number;
}

export function isEmptyAdvisorProfile(
  applications: LoanApplicationSummaryDto[],
  loans: LoanSummaryDto[],
): boolean {
  return applications.length === 0 && loans.length === 0;
}

export function countUnderReviewApplications(applications: LoanApplicationSummaryDto[]): number {
  return applications.filter((app) => app.status === 'UNDER_REVIEW').length;
}

export function countActionRequiredApplications(applications: LoanApplicationSummaryDto[]): number {
  return applications.filter((app) =>
    ACTIVE_APPLICATION_STATUSES.includes(app.status as (typeof ACTIVE_APPLICATION_STATUSES)[number]),
  ).length;
}

export function countActiveLoans(loans: LoanSummaryDto[]): number {
  return loans.filter((loan) => loan.status === 'ACTIVE').length;
}

export function totalOutstandingBalance(loans: LoanSummaryDto[]): number {
  return loans
    .filter((loan) => loan.status === 'ACTIVE')
    .reduce((sum, loan) => sum + (loan.remainingBalance ?? 0), 0);
}

export function hoursSince(isoDate: string | null | undefined): number | null {
  if (!isoDate) {
    return null;
  }
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return Math.floor((Date.now() - date.getTime()) / 3_600_000);
}

export function advisorApplicationHint(app: LoanApplicationSummaryDto): string | null {
  if (DEMO_APPLICATION_HINTS[app.reference]) {
    return DEMO_APPLICATION_HINTS[app.reference];
  }
  if (app.status === 'OFFER_PENDING') {
    return 'Réponse client attendue';
  }
  const hours = hoursSince(app.submittedAt ?? app.createdAt);
  if (app.status === 'SUBMITTED' && hours != null && hours >= 48) {
    const days = Math.floor(hours / 24);
    return days <= 1 ? 'Soumis il y a 2 jours' : `Soumis il y a ${days} jours`;
  }
  if (app.status === 'UNDER_REVIEW') {
    return 'Examen du dossier en cours';
  }
  return null;
}

export function advisorApplicationHintTone(
  app: LoanApplicationSummaryDto,
): 'warning' | 'info' | 'neutral' {
  if (DEMO_APPLICATION_HINTS[app.reference] || app.status === 'OFFER_PENDING') {
    return 'warning';
  }
  const hours = hoursSince(app.submittedAt ?? app.createdAt);
  if (app.status === 'SUBMITTED' && hours != null && hours >= 48) {
    return 'warning';
  }
  return 'info';
}

function applicationPriorityScore(app: LoanApplicationSummaryDto): number {
  if (DEMO_APPLICATION_HINTS[app.reference]) {
    return 1000;
  }
  switch (app.status) {
    case 'UNDER_REVIEW':
      return 800;
    case 'OFFER_PENDING':
      return 700;
    case 'SUBMITTED': {
      const hours = hoursSince(app.submittedAt ?? app.createdAt) ?? 0;
      // Ne jamais dépasser UNDER_REVIEW : l'ancienneté ne doit pas masquer un dossier en étude.
      return 500 + Math.min(hours, 48);
    }
    case 'DRAFT':
      return 100;
    default:
      return 0;
  }
}

export function pickPriorityApplications(
  applications: LoanApplicationSummaryDto[],
  limit = 3,
): LoanApplicationSummaryDto[] {
  return [...applications]
    .filter((app) => !['APPROVED', 'REJECTED', 'CANCELLED'].includes(app.status))
    .sort((a, b) => applicationPriorityScore(b) - applicationPriorityScore(a))
    .slice(0, limit);
}

export function buildAdvisorPriorityAlerts(
  applications: LoanApplicationSummaryDto[],
  limit = 2,
): AdvisorPriorityAlert[] {
  const alerts: AdvisorPriorityAlert[] = [];

  for (const app of pickPriorityApplications(applications, applications.length)) {
    const hint = advisorApplicationHint(app);
    if (!hint) {
      continue;
    }
    alerts.push({
      reference: app.reference,
      message: hint,
      applicationId: app.id,
    });
    if (alerts.length >= limit) {
      break;
    }
  }

  return alerts;
}

export function advisorApplicationRouterLink(app: LoanApplicationSummaryDto): (string | number)[] {
  return ['/conseiller/dossiers', app.id];
}

export function advisorApplicationCtaLabel(status: string): string {
  switch (status) {
    case 'UNDER_REVIEW':
      return 'Ouvrir le dossier';
    case 'SUBMITTED':
      return 'Examiner';
    case 'OFFER_PENDING':
      return 'Voir le dossier';
    default:
      return 'Voir le dossier';
  }
}

export function displayApplicantName(app: LoanApplicationSummaryDto): string {
  return app.applicantName?.trim() || app.title || app.reference;
}

export function applicantInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return '?';
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

export function buildAdvisorHeroSlides(
  applications: LoanApplicationSummaryDto[],
  loans: LoanSummaryDto[],
): HeroCarouselSlide[] {
  const underReview = countUnderReviewApplications(applications);
  const actionRequired = countActionRequiredApplications(applications);
  const activeLoans = countActiveLoans(loans);
  const outstanding = totalOutstandingBalance(loans);
  const priority = pickPriorityApplications(applications, 1)[0] ?? null;

  const slides: HeroCarouselSlide[] = [];

  if (underReview > 0 || actionRequired > 0) {
    const refusedHint = applications.some((app) => DEMO_APPLICATION_HINTS[app.reference]);
    slides.push({
      id: 'dossiers-etude',
      eyebrow:
        underReview > 0
          ? `${underReview} dossier${underReview > 1 ? 's' : ''} en cours d'étude`
          : `${actionRequired} dossier${actionRequired > 1 ? 's' : ''} à traiter`,
      amountLine: refusedHint
        ? 'Dont 1 avec documents refusés en attente client'
        : 'Consultez vos dossiers assignés et avancez les dossiers en attente',
      ctaLabel: 'Traiter les dossiers',
      routerLink: ['/conseiller/dossiers'],
      visual: 'advisor-dossiers',
    });
  }

  if (activeLoans > 0) {
    slides.push({
      id: 'portefeuille',
      eyebrow: `Portefeuille : ${activeLoans} prêt${activeLoans > 1 ? 's' : ''} actif${activeLoans > 1 ? 's' : ''}`,
      amountLine:
        outstanding > 0
          ? `Encours total suivi : ${Math.round(outstanding).toLocaleString('fr-FR')} €`
          : 'Suivez les échéances et le recouvrement de vos clients',
      ctaLabel: 'Voir mes prêts',
      routerLink: ['/conseiller/prets'],
      visual: 'advisor-portfolio',
    });
  }

  if (priority) {
    const clientName = displayApplicantName(priority);
    const hours = hoursSince(priority.submittedAt ?? priority.createdAt);
    slides.push({
      id: 'sla',
      eyebrow: 'Objectif réponse : sous 48 h ouvrées',
      amountLine:
        hours != null && hours >= 48
          ? `Dossier ${priority.reference} — ${clientName} — en attente depuis 48 h`
          : `Dossier ${priority.reference} — ${clientName}`,
      ctaLabel: 'Ouvrir le dossier',
      routerLink: advisorApplicationRouterLink(priority),
      visual: 'advisor-sla',
    });
  }

  if (slides.length === 0) {
    slides.push({
      id: 'welcome',
      eyebrow: 'Bienvenue sur votre espace conseiller',
      amountLine: 'Les dossiers vous seront assignés automatiquement ou par un administrateur',
      ctaLabel: 'Voir mes dossiers',
      routerLink: ['/conseiller/dossiers'],
      visual: 'advisor-welcome',
    });
  }

  return slides.slice(0, 3);
}

export function formatNextInstallment(loan: LoanSummaryDto): string {
  if (!loan.nextInstallmentDate) {
    return '—';
  }
  const date = new Date(loan.nextInstallmentDate);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  const formatted = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  if (loan.nextInstallmentAmount != null) {
    return `${formatted} — ${Math.round(loan.nextInstallmentAmount).toLocaleString('fr-FR')} €`;
  }
  return formatted;
}

export function recentLoans(loans: LoanSummaryDto[], limit = 5): LoanSummaryDto[] {
  return [...loans]
    .sort((a, b) => {
      const rank = (status: string) => (status === 'ACTIVE' ? 0 : status === 'PENDING_MANDATE' ? 1 : 2);
      const diff = rank(a.status) - rank(b.status);
      if (diff !== 0) {
        return diff;
      }
      return (b.id ?? 0) - (a.id ?? 0);
    })
    .slice(0, limit);
}
