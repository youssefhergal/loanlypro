import { FormGroup } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error.model';

export function parseApiError(error: unknown): ApiError | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const body = error.error;
  if (body && typeof body === 'object' && 'code' in body && 'message' in body) {
    return body as ApiError;
  }
  return null;
}

export function getErrorMessage(error: unknown, fallback = 'Une erreur est survenue.'): string {
  const apiError = parseApiError(error);
  if (apiError?.message) {
    return apiError.message;
  }
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'Impossible de contacter le serveur.';
  }
  return fallback;
}

export function applyApiErrorsToForm(form: FormGroup, error: unknown): void {
  const apiError = parseApiError(error);
  if (!apiError?.details?.length) {
    return;
  }
  for (const detail of apiError.details) {
    const control = form.get(detail.field);
    if (control) {
      control.setErrors({ api: detail.message });
      control.markAsTouched();
    }
  }
}
