import { LOAN_INTEREST_RATE } from '../constants/loan.constants';

export interface LoanSimulation {
  monthlyPayment: number;
  totalCost: number;
  totalInterest: number;
}

export function calculateMonthlyPayment(
  principal: number,
  durationMonths: number,
  annualRate = LOAN_INTEREST_RATE
): number {
  if (!principal || !durationMonths || principal <= 0 || durationMonths <= 0) {
    return 0;
  }
  const monthlyRate = annualRate / 12;
  if (monthlyRate === 0) {
    return principal / durationMonths;
  }
  const factor = Math.pow(1 + monthlyRate, durationMonths);
  return (principal * monthlyRate * factor) / (factor - 1);
}

export function calculateLoanSimulation(
  principal: number,
  durationMonths: number
): LoanSimulation {
  const monthlyPayment = calculateMonthlyPayment(principal, durationMonths);
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
