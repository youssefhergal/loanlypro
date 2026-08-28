import { LoanApplicationStatus } from './loan.enums';

export interface AdminAdvisorOption {
  id: number;
  name: string;
}

export interface AdminLoanListSummary {
  totalCount: number;
  unassignedCount: number;
  statusCounts: Partial<Record<LoanApplicationStatus, number>>;
  advisors: AdminAdvisorOption[];
}

export type AdminLoanListSortApi =
  | 'UPDATED_DESC'
  | 'UPDATED_ASC'
  | 'AMOUNT_DESC'
  | 'AMOUNT_ASC';
