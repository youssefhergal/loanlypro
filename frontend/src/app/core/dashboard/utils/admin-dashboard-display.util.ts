import { LoanApplicationStatus } from '../../loans/models/loan.enums';
import { AdminAdvisorOption } from '../../loans/models/admin-loan-list-summary.model';

export interface PipelineSegment {
  key: LoanApplicationStatus;
  label: string;
  count: number;
  color: string;
  widthPercent: number;
}

export interface AdvisorWorkloadRow {
  id: number | null;
  name: string;
  count: number;
  percent: number;
  barGradient: string;
  chargeTier: 'low' | 'medium' | 'high';
  isUnassigned?: boolean;
}

const PIPELINE_STATUSES: {
  key: LoanApplicationStatus;
  label: string;
  color: string;
}[] = [
  { key: 'SUBMITTED', label: 'Soumise', color: '#90caf9' },
  { key: 'UNDER_REVIEW', label: 'En étude', color: '#ed6c02' },
  { key: 'OFFER_PENDING', label: 'Offre', color: '#ce93d8' },
  { key: 'APPROVED', label: 'Approuvée', color: '#2e7d32' },
  { key: 'REJECTED', label: 'Refusée', color: '#d32f2f' },
  { key: 'CANCELLED', label: 'Annulée', color: '#9e9e9e' },
];

export function applicationStatusLabel(status: string): string {
  switch (status) {
    case 'DRAFT':
      return 'Brouillon';
    case 'SUBMITTED':
      return 'Soumise';
    case 'UNDER_REVIEW':
      return 'En étude';
    case 'OFFER_PENDING':
      return 'Offre en attente';
    case 'APPROVED':
      return 'Approuvée';
    case 'REJECTED':
      return 'Refusée';
    case 'CANCELLED':
      return 'Annulée';
    default:
      return status;
  }
}

export function buildPipelineSegments(
  statusCounts: Partial<Record<LoanApplicationStatus, number>>,
): PipelineSegment[] {
  const segments = PIPELINE_STATUSES.map((item) => ({
    ...item,
    count: statusCounts[item.key] ?? 0,
    widthPercent: 0,
  })).filter((item) => item.count > 0);

  const total = segments.reduce((sum, item) => sum + item.count, 0);
  if (total <= 0) {
    return [];
  }

  return segments.map((item) => ({
    ...item,
    widthPercent: Math.round((item.count / total) * 1000) / 10,
  }));
}

export function pipelineSegmentsTotal(segments: PipelineSegment[]): number {
  return segments.reduce((sum, item) => sum + item.count, 0);
}

export function buildPipelineConicGradient(segments: PipelineSegment[]): string {
  if (segments.length === 0) {
    return '#eef4f0';
  }

  let cursor = 0;
  const stops = segments.map((segment) => {
    const start = cursor;
    cursor += segment.widthPercent * 3.6;
    return `${segment.color} ${start}deg ${cursor}deg`;
  });

  return `conic-gradient(from -90deg, ${stops.join(', ')})`;
}

function chargeTier(count: number, min: number, max: number): 'low' | 'medium' | 'high' {
  if (max <= min) {
    return 'medium';
  }
  const ratio = (count - min) / (max - min);
  if (ratio <= 0.33) {
    return 'low';
  }
  if (ratio >= 0.66) {
    return 'high';
  }
  return 'medium';
}

function workloadBarGradientByTier(tier: 'low' | 'medium' | 'high'): string {
  switch (tier) {
    case 'low':
      return 'linear-gradient(90deg, #d4f0e2, #2e7d32)';
    case 'medium':
      return 'linear-gradient(90deg, #fff3e0, #ed6c02)';
    case 'high':
      return 'linear-gradient(90deg, #ffcdd2, #d32f2f)';
  }
}

/** 2 conseillers les plus chargés + 2 les moins chargés (max 4, sans doublon). */
export function buildExtremesAdvisorWorkloadRows(
  advisorCounts: { id: number; name: string; count: number }[],
): AdvisorWorkloadRow[] {
  if (advisorCounts.length === 0) {
    return [];
  }

  const sortedDesc = [...advisorCounts].sort((a, b) => b.count - a.count);
  const sortedAsc = [...advisorCounts].sort((a, b) => a.count - b.count);

  const top2 = sortedDesc.slice(0, 2);
  const lowest = sortedAsc[0];
  const secondLowest = sortedAsc[1];

  const orderedCandidates = [
    ...top2,
    ...(secondLowest ? [secondLowest] : []),
    ...(lowest ? [lowest] : []),
  ];

  const picked: { id: number; name: string; count: number }[] = [];
  const seen = new Set<number>();
  for (const item of orderedCandidates) {
    if (seen.has(item.id)) {
      continue;
    }
    seen.add(item.id);
    picked.push(item);
  }

  const allCounts = advisorCounts.map((item) => item.count);
  const globalMin = Math.min(...allCounts);
  const globalMax = Math.max(...allCounts);

  return picked.map((item) => {
    const tier = chargeTier(item.count, globalMin, globalMax);
    return {
      id: item.id,
      name: item.name,
      count: item.count,
      percent: globalMax > 0 ? Math.round((item.count / globalMax) * 100) : 0,
      chargeTier: tier,
      barGradient: workloadBarGradientByTier(tier),
    };
  });
}

/** @deprecated Utiliser buildExtremesAdvisorWorkloadRows */
export function buildTopAdvisorWorkloadRows(
  advisorCounts: { id: number; name: string; count: number }[],
  limit = 4,
): AdvisorWorkloadRow[] {
  const sorted = [...advisorCounts].sort((a, b) => b.count - a.count).slice(0, limit);
  if (sorted.length === 0) {
    return [];
  }

  const counts = sorted.map((item) => item.count);
  const min = Math.min(...counts);
  const max = Math.max(...counts);

  return sorted.map((item) => {
    const tier = chargeTier(item.count, min, max);
    return {
      id: item.id,
      name: item.name,
      count: item.count,
      percent: max > 0 ? Math.round((item.count / max) * 100) : 0,
      chargeTier: tier,
      barGradient: workloadBarGradientByTier(tier),
    };
  });
}

export function buildAdvisorWorkloadRows(
  advisors: AdminAdvisorOption[],
  advisorCounts: { id: number; name: string; count: number }[],
  unassignedCount: number,
): AdvisorWorkloadRow[] {
  const rows: AdvisorWorkloadRow[] = advisorCounts.map((item) => ({
    id: item.id,
    name: item.name,
    count: item.count,
    percent: 0,
    chargeTier: 'medium' as const,
    barGradient: workloadBarGradientByTier('medium'),
  }));

  if (unassignedCount > 0) {
    rows.push({
      id: null,
      name: 'Non affecté',
      count: unassignedCount,
      percent: 0,
      chargeTier: 'high',
      barGradient: workloadBarGradientByTier('high'),
      isUnassigned: true,
    });
  }

  const max = Math.max(...rows.map((row) => row.count), 1);
  return rows.map((row) => ({
    ...row,
    percent: Math.round((row.count / max) * 100),
  }));
}

export function formatRelativeSubmittedAt(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  const diffMs = Date.now() - date.getTime();
  const hours = Math.floor(diffMs / (1000 * 60 * 60));
  if (hours < 1) {
    return 'il y a quelques minutes';
  }
  if (hours < 24) {
    return `il y a ${hours} h`;
  }
  const days = Math.floor(hours / 24);
  if (days === 1) {
    return 'il y a 1 j';
  }
  return `il y a ${days} j`;
}

export function statusCount(
  statusCounts: Partial<Record<LoanApplicationStatus, number>>,
  status: LoanApplicationStatus,
): number {
  return statusCounts[status] ?? 0;
}
