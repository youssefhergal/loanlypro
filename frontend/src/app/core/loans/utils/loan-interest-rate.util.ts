import { LoanPurpose } from '../models/loan.enums';

const BASE_RATE_PERCENT = 3.95;
const MIN_RATE_PERCENT = 2.5;
const MAX_RATE_PERCENT = 7.5;
const GREEN_DISCOUNT_PERCENT = 0.75;

/** Taux annuel décimal (ex. 0.0385 pour 3,85 %). */
export function computeIndicativeAnnualRate(
  amount: number,
  durationMonths: number,
  purpose: LoanPurpose = 'PERSONAL'
): number {
  if (!amount || !durationMonths || amount <= 0 || durationMonths <= 0) {
    return BASE_RATE_PERCENT / 100;
  }

  let rate =
    BASE_RATE_PERCENT +
    purposeAdjustmentPercent(purpose) +
    amountAdjustmentPercent(amount) +
    durationAdjustmentPercent(durationMonths);

  rate = Math.min(MAX_RATE_PERCENT, Math.max(MIN_RATE_PERCENT, rate));
  return roundRatePercent(rate) / 100;
}

export function computeIndicativeRatePercent(
  amount: number,
  durationMonths: number,
  purpose: LoanPurpose = 'PERSONAL'
): number {
  return roundRatePercent(computeIndicativeAnnualRate(amount, durationMonths, purpose) * 100);
}

function purposeAdjustmentPercent(purpose: LoanPurpose): number {
  switch (purpose) {
    case 'GREEN':
      return -GREEN_DISCOUNT_PERCENT;
    case 'HOME_IMPROVEMENT':
      return -0.25;
    case 'SOFTWARE':
      return -0.1;
    case 'EDUCATION':
      return -0.05;
    case 'VEHICLE':
      return 0;
    case 'PERSONAL':
      return 0.05;
    case 'OTHER':
      return 0.15;
    default:
      return 0;
  }
}

function amountAdjustmentPercent(amount: number): number {
  if (amount >= 75_000) return -0.35;
  if (amount >= 40_000) return -0.25;
  if (amount >= 15_000) return -0.1;
  if (amount >= 5_000) return 0;
  return 0.2;
}

function durationAdjustmentPercent(durationMonths: number): number {
  if (durationMonths <= 24) return -0.05;
  if (durationMonths <= 60) return 0;
  if (durationMonths <= 120) return 0.2;
  return 0.4;
}

function roundRatePercent(value: number): number {
  return Math.round(value * 100) / 100;
}

export function isGreenLoanPurpose(purpose: LoanPurpose | null | undefined): boolean {
  return purpose === 'GREEN';
}
