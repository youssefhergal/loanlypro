import { Injectable, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable, of, tap, catchError, throwError, finalize, switchMap, startWith } from 'rxjs';
import { LoanApiService } from './loan-api.service';
import { LoanResponseDto } from '../models/loan-response.model';
import { LoanRequestDto } from '../models/loan-request.model';
import { LoanDocumentResponseDto } from '../models/loan-document.model';
import { EmploymentStatus, LoanPurpose } from '../models/loan.enums';
import {
  LOAN_AMOUNT_MAX,
  LOAN_AMOUNT_MIN,
  LOAN_COMMENT_MAX_LENGTH,
  LOAN_DURATION_MAX,
  LOAN_DURATION_MIN,
  WIZARD_STORAGE_KEY,
  seniorityRangeFromMonths,
} from '../constants/loan.constants';
import { getErrorMessage } from '../utils/api-error.util';

@Injectable()
export class LoanWizardStateService {
  private readonly fb = inject(FormBuilder);
  private readonly loanApi = inject(LoanApiService);

  readonly applicationId = signal<number | null>(null);
  readonly reference = signal<string | null>(null);
  readonly currentStep = signal(0);
  readonly documents = signal<LoanDocumentResponseDto[]>([]);
  readonly isSaving = signal(false);
  readonly isSubmitting = signal(false);
  readonly lastError = signal<string | null>(null);

  readonly form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    loanPurpose: ['PERSONAL' as LoanPurpose, Validators.required],
    requestedAmount: [
      15000,
      [
        Validators.required,
        Validators.min(LOAN_AMOUNT_MIN),
        Validators.max(LOAN_AMOUNT_MAX),
      ],
    ],
    requestedDurationMonths: [
      48,
      [
        Validators.required,
        Validators.min(LOAN_DURATION_MIN),
        Validators.max(LOAN_DURATION_MAX),
      ],
    ],
    comment: ['', Validators.maxLength(LOAN_COMMENT_MAX_LENGTH)],
    monthlyIncome: [0, [Validators.required, Validators.min(0)]],
    additionalIncome: [0, [Validators.min(0)]],
    employmentStatus: ['CDI' as EmploymentStatus, Validators.required],
    employerName: [''],
    employerSector: [''],
    jobTitle: [''],
    hireDate: [''],
    seniorityRange: [''],
    seniorityMonths: [null as number | null, [Validators.min(0)]],
    monthlyRent: [0, [Validators.min(0)]],
    monthlyLoanPayments: [0, [Validators.min(0)]],
    monthlyAlimony: [0, [Validators.min(0)]],
    monthlyOtherCharges: [0, [Validators.min(0)]],
    certifyAccuracy: [false, Validators.requiredTrue],
    acceptTerms: [false, Validators.requiredTrue],
  });

  readonly stepFieldGroups: string[][] = [
    ['title', 'loanPurpose', 'requestedAmount', 'requestedDurationMonths', 'comment'],
    [
      'monthlyIncome',
      'additionalIncome',
      'employmentStatus',
      'employerName',
      'employerSector',
      'jobTitle',
      'hireDate',
      'seniorityRange',
      'seniorityMonths',
      'monthlyRent',
      'monthlyLoanPayments',
      'monthlyAlimony',
      'monthlyOtherCharges',
    ],
    [],
    ['certifyAccuracy', 'acceptTerms'],
  ];

  readonly hasApplication = computed(() => this.applicationId() !== null);

  /** Réactif aux changements du formulaire (pour computed() dans le récapitulatif). */
  readonly formValue = toSignal(
    this.form.valueChanges.pipe(startWith(this.form.getRawValue())),
    { initialValue: this.form.getRawValue() }
  );

  initFromRoute(id: number | null): Observable<LoanResponseDto | null> {
    if (!id) {
      this.restoreFromSession();
      return of(null);
    }
    return this.loanApi.getById(id).pipe(
      tap((loan) => {
        if (loan.status !== 'DRAFT') {
          throw new Error('Ce dossier ne peut plus être modifié.');
        }
        this.patchFromResponse(loan);
        this.applicationId.set(loan.id);
        this.reference.set(loan.reference);
      }),
      switchMap((loan) => this.refreshDocuments(loan.id).pipe(switchMap(() => of(loan))))
    );
  }

  refreshDocuments(loanId: number): Observable<LoanDocumentResponseDto[]> {
    return this.loanApi.getDocuments(loanId).pipe(
      tap((docs) => this.documents.set(docs))
    );
  }

  patchFromResponse(loan: LoanResponseDto): void {
    this.form.patchValue({
      title: loan.title,
      loanPurpose: loan.loanPurpose,
      requestedAmount: Number(loan.requestedAmount),
      requestedDurationMonths: loan.requestedDurationMonths,
      comment: loan.comment ?? '',
      monthlyIncome: Number(loan.monthlyIncome),
      additionalIncome: Number(loan.additionalIncome ?? 0),
      employmentStatus: loan.employmentStatus,
      employerName: loan.employerName ?? '',
      employerSector: loan.employerSector ?? '',
      jobTitle: loan.jobTitle ?? '',
      hireDate: loan.hireDate ?? '',
      seniorityRange: seniorityRangeFromMonths(loan.seniorityMonths),
      seniorityMonths: loan.seniorityMonths,
      monthlyRent: Number(loan.monthlyRent ?? 0),
      monthlyLoanPayments: Number(loan.monthlyLoanPayments ?? 0),
      monthlyAlimony: Number(loan.monthlyAlimony ?? 0),
      monthlyOtherCharges: Number(loan.monthlyOtherCharges ?? 0),
    });
    this.persistSession();
  }

  toRequestDto(): LoanRequestDto {
    const v = this.form.getRawValue();
    return {
      title: v.title,
      loanPurpose: v.loanPurpose,
      requestedAmount: Number(v.requestedAmount),
      requestedDurationMonths: Number(v.requestedDurationMonths),
      comment: v.comment || null,
      monthlyIncome: Number(v.monthlyIncome),
      employmentStatus: v.employmentStatus,
      additionalIncome: Number(v.additionalIncome ?? 0),
      employerName: v.employerName || null,
      jobTitle: v.jobTitle || null,
      employerSector: v.employerSector || null,
      hireDate: v.hireDate || null,
      seniorityMonths: v.seniorityMonths != null ? Number(v.seniorityMonths) : null,
      monthlyRent: Number(v.monthlyRent ?? 0),
      monthlyLoanPayments: Number(v.monthlyLoanPayments ?? 0),
      monthlyAlimony: Number(v.monthlyAlimony ?? 0),
      monthlyOtherCharges: Number(v.monthlyOtherCharges ?? 0),
    };
  }

  validateStep(stepIndex: number): boolean {
    const fields = this.stepFieldGroups[stepIndex] ?? [];
    if (!fields.length) {
      return true;
    }
    let valid = true;
    for (const name of fields) {
      const control = this.form.get(name);
      control?.markAsTouched();
      if (control?.invalid) {
        valid = false;
      }
    }
    return valid;
  }

  saveProgress(): Observable<LoanResponseDto> {
    this.isSaving.set(true);
    this.lastError.set(null);
    const body = this.toRequestDto();
    const id = this.applicationId();
    const request$ = id
      ? this.loanApi.update(id, body)
      : this.loanApi.create(body);

    return request$.pipe(
      tap((loan) => {
        this.applicationId.set(loan.id);
        this.reference.set(loan.reference);
        this.persistSession();
      }),
      catchError((err) => {
        this.lastError.set(getErrorMessage(err));
        return throwError(() => err);
      }),
      finalize(() => this.isSaving.set(false))
    );
  }

  finalizeSubmit(): Observable<LoanResponseDto> {
    const id = this.applicationId();
    if (!id) {
      return throwError(() => new Error('Aucun dossier à soumettre.'));
    }
    this.isSubmitting.set(true);
    this.lastError.set(null);
    return this.loanApi.submit(id).pipe(
      tap(() => this.clearSession()),
      catchError((err) => {
        this.lastError.set(getErrorMessage(err));
        return throwError(() => err);
      }),
      finalize(() => this.isSubmitting.set(false))
    );
  }

  persistSession(): void {
    sessionStorage.setItem(
      WIZARD_STORAGE_KEY,
      JSON.stringify({
        applicationId: this.applicationId(),
        reference: this.reference(),
        currentStep: this.currentStep(),
        formValue: this.form.getRawValue(),
      })
    );
  }

  restoreFromSession(): void {
    const raw = sessionStorage.getItem(WIZARD_STORAGE_KEY);
    if (!raw) return;
    try {
      const data = JSON.parse(raw) as {
        applicationId: number | null;
        reference: string | null;
        currentStep: number;
        formValue: Record<string, unknown>;
      };
      this.applicationId.set(data.applicationId);
      this.reference.set(data.reference);
      this.currentStep.set(data.currentStep ?? 0);
      this.form.patchValue(data.formValue);
    } catch {
      sessionStorage.removeItem(WIZARD_STORAGE_KEY);
    }
  }

  clearSession(): void {
    sessionStorage.removeItem(WIZARD_STORAGE_KEY);
  }
}
