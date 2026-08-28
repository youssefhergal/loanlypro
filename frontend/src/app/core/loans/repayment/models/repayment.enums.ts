export type RepaymentLoanStatus = 'PENDING_MANDATE' | 'ACTIVE' | 'DEFAULTED' | 'CLOSED';

export type MandateStatus = 'NONE' | 'ACTIVE' | 'REVOKED' | 'PENDING';

export type InstallmentStatus =
  | 'UPCOMING'
  | 'PAID'
  | 'FAILED'
  | 'BLOCKED'
  | 'OVERDUE';

export type PaymentTransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
