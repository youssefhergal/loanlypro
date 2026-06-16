import { InstallmentDto } from './installment.model';
import { LoanSummaryDto } from './loan-summary.model';
import { PaymentTransactionDto } from './payment-transaction.model';

export interface LoanDetailDto extends LoanSummaryDto {
  durationMonths: number | null;
  annualRate: number | null;
  totalRepayable: number | null;
  installmentCount: number | null;
  paidInstallmentsCount: number;
  ibanMasked: string | null;
  holderName: string | null;
  borrowerEmail: string | null;
  advisorName: string | null;
  activatedAt: string | null;
  closedAt: string | null;
  installments?: InstallmentDto[];
  transactions?: PaymentTransactionDto[];
}
