export interface ActivateMandateRequestDto {
  iban: string;
  holderName: string;
}

export interface MandateResponseDto {
  loanId: number;
  mandateReference: string;
  status: string;
  ibanMasked: string;
  holderName: string;
}
