import { EmploymentStatus, LoanApplicationStatus, LoanPurpose } from './loan.enums';

export interface LoanResponseDto {
  id: number;
  reference: string;
  status: LoanApplicationStatus;
  title: string;
  loanPurpose: LoanPurpose;
  requestedAmount: number;
  requestedDurationMonths: number;
  purpose: string;
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
  submittedAt?: string | null;
  decidedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  decisionComment?: string | null;
  approvedAmount?: number | null;
  approvedDurationMonths?: number | null;
  interestRate?: number | null;
  applicantId?: number | null;
  applicantName?: string | null;
  advisorId?: number | null;
  advisorName?: string | null;
}
