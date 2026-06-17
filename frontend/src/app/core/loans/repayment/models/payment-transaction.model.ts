import { PaymentTransactionStatus } from './repayment.enums';

export interface PaymentTransactionDto {
  id: number;
  installmentId: number;
  sequenceNumber: number;
  attemptNumber: number;
  amount: number;
  status: PaymentTransactionStatus;
  failureReason: string | null;
  externalReference: string | null;
  attemptedAt: string;
  settledAt: string | null;
}
