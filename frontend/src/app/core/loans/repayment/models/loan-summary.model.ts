import { MandateStatus, RepaymentLoanStatus } from './repayment.enums';

export interface LoanSummaryDto {
  id: number;
  loanApplicationId: number;
  reference: string;
  status: RepaymentLoanStatus;
  principalAmount: number;
  remainingBalance: number;
  monthlyPayment: number | null;
  nextInstallmentDate: string | null;
  nextInstallmentAmount: number | null;
  nextInstallmentStatus: string | null;
  installmentCount: number | null;
  remainingInstallmentsCount: number | null;
  mandateStatus: MandateStatus | string;
  borrowerName: string | null;
  borrowerEmail: string | null;
  overdueInstallmentsCount: number;
}
