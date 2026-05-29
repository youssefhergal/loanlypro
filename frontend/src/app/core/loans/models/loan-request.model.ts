import { EmploymentStatus, LoanPurpose } from './loan.enums';

export interface LoanRequestDto {
  title: string;
  loanPurpose: LoanPurpose;
  requestedAmount: number;
  requestedDurationMonths: number;
  comment?: string | null;
  monthlyIncome: number;
  employmentStatus: EmploymentStatus;
  additionalIncome?: number | null;
  employerName?: string | null;
  jobTitle?: string | null;
  employerSector?: string | null;
  hireDate?: string | null;
  seniorityMonths?: number | null;
  monthlyRent?: number | null;
  monthlyLoanPayments?: number | null;
  monthlyAlimony?: number | null;
  monthlyOtherCharges?: number | null;
}
