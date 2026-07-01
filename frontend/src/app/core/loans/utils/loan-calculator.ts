import { LoanPurpose } from '../models/loan.enums';
import { computeIndicativeAnnualRate } from './loan-interest-rate.util';

export interface LoanSimulation {
  monthlyPayment: number;
  totalCost: number;
  totalInterest: number;
}

export function calculateMonthlyPayment(
  principal: number,
  durationMonths: number,
  annualRate?: number,
  purpose?: LoanPurpose
): number {
  const effectiveRate =
    annualRate ??
    computeIndicativeAnnualRate(principal, durationMonths, purpose ?? 'PERSONAL');

  if (!principal || !durationMonths || principal <= 0 || durationMonths <= 0) {
    return 0;
  }
  const monthlyRate = effectiveRate / 12;
  if (monthlyRate === 0) {
    return principal / durationMonths;
  }
  const factor = Math.pow(1 + monthlyRate, durationMonths);
  return (principal * monthlyRate * factor) / (factor - 1);
}

export function calculateLoanSimulation(
  principal: number,
  durationMonths: number,
  purpose?: LoanPurpose,
  annualRate?: number
): LoanSimulation {
  const effectiveRate =
    annualRate ??
    computeIndicativeAnnualRate(principal, durationMonths, purpose ?? 'PERSONAL');
  const monthlyPayment = calculateMonthlyPayment(principal, durationMonths, effectiveRate);
  const totalCost = monthlyPayment * durationMonths;
  return {
    monthlyPayment,
    totalCost,
    totalInterest: Math.max(0, totalCost - principal),
  };
}

export function calculateDebtRatio(
  monthlyCharges: number,
  estimatedMonthlyPayment: number,
  totalIncome: number
): number {
  if (!totalIncome || totalIncome <= 0) {
    return 0;
  }
  return (monthlyCharges + estimatedMonthlyPayment) / totalIncome;
}
