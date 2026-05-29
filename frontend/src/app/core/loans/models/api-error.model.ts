export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'BUSINESS_RULE'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'INTERNAL_ERROR';

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  code: ApiErrorCode;
  message: string;
  details: ApiFieldError[];
}
