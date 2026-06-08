export interface AdvisorAssignmentItem {
  loanId: number;
  reference: string;
  advisorId: number;
  advisorName: string;
}

export interface AdvisorAssignmentResult {
  assignedCount: number;
  unassignedRemaining: number;
  advisorsAvailable: number;
  message: string;
  assignments: AdvisorAssignmentItem[];
}
