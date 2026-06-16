import { InstallmentStatus } from './repayment.enums';

export interface InstallmentDto {
  id: number;
  sequenceNumber: number;
  dueDate: string;
  amountDue: number;
  principalPart: number;
  interestPart: number;
  remainingBalance: number;
  status: InstallmentStatus;
  attemptCount: number;
  nextRetryDate: string | null;
}
