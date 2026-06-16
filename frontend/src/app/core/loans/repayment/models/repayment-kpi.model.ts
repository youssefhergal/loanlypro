export interface RepaymentKpiDto {
  activeLoansCount: number;
  closedLoansCount: number;
  totalOutstanding: number;
  collectedThisMonth: number;
  failureRatePercent: number;
  overdueInstallmentsCount: number;
}
