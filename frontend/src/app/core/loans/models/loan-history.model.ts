import type { LoanDocumentType } from './loan.enums';

export type LoanApplicationEventType =
  | 'APPLICATION_CREATED'
  | 'APPLICATION_SUBMITTED'
  | 'DOCUMENT_UPLOADED'
  | 'ADVISOR_ASSIGNED'
  | 'REVIEW_STARTED'
  | 'DOCUMENT_REJECTED'
  | 'DOCUMENT_VALIDATED'
  | 'APPLICATION_APPROVED'
  | 'APPLICATION_REJECTED'
  | 'APPLICATION_CANCELLED'
  | 'FUNDS_RELEASED';

export type LoanEventActorType = 'CLIENT' | 'ADVISOR' | 'ADMIN' | 'SYSTEM';

export type LoanHistoryEventState = 'done' | 'current' | 'upcoming' | 'warn';

export interface LoanHistoryEventResponseDto {
  id?: number;
  eventType?: LoanApplicationEventType;
  occurredAt?: string;
  actorType?: LoanEventActorType;
  actorDisplayName?: string | null;
  title: string;
  description: string;
  state: LoanHistoryEventState;
  documentType?: LoanDocumentType;
  comment?: string | null;
  complement?: boolean | null;
}
