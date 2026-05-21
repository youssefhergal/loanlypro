import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { LoanWizardStateService } from '../../../core/loans/services/loan-wizard-state.service';
import { StepNeedComponent } from './steps/step-need.component';
import { StepSituationComponent } from './steps/step-situation.component';
import { StepDocumentsComponent } from './steps/step-documents.component';
import { StepSummaryComponent } from './steps/step-summary.component';
import { applyApiErrorsToForm, getErrorMessage } from '../../../core/loans/utils/api-error.util';
import { switchMap } from 'rxjs';

@Component({
  selector: 'app-loan-wizard',
  standalone: true,
  providers: [LoanWizardStateService],
  imports: [
    RouterLink,
    MatStepperModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    StepNeedComponent,
    StepSituationComponent,
    StepDocumentsComponent,
    StepSummaryComponent,
  ],
  templateUrl: './loan-wizard.component.html',
  styleUrl: './loan-wizard.component.scss',
})
export class LoanWizardComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly state = inject(LoanWizardStateService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    this.state.initFromRoute(Number.isFinite(id) && id ? id : null).subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loadError.set(getErrorMessage(err, 'Impossible de charger le dossier.'));
        this.loading.set(false);
      },
    });
  }

  onStepChange(event: StepperSelectionEvent): void {
    this.state.currentStep.set(event.selectedIndex);
    this.state.persistSession();
  }

  goNext(stepIndex: number): void {
    if (!this.state.validateStep(stepIndex)) {
      return;
    }
    if (stepIndex <= 1) {
      this.state.saveProgress().subscribe({
        next: () => {
          if (stepIndex === 1 && this.state.applicationId()) {
            this.state.refreshDocuments(this.state.applicationId()!).subscribe();
          }
          this.state.currentStep.set(stepIndex + 1);
          this.state.persistSession();
        },
        error: (err) => {
          applyApiErrorsToForm(this.state.form, err);
          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
        },
      });
      return;
    }
    if (stepIndex === 2) {
      this.state.currentStep.set(3);
      this.state.persistSession();
    }
  }

  goBack(stepIndex: number): void {
    if (stepIndex > 0) {
      this.state.currentStep.set(stepIndex - 1);
      this.state.persistSession();
    }
  }

  saveAndQuit(): void {
    if (!this.state.validateStep(0)) {
      return;
    }
    this.state.saveProgress().subscribe({
      next: () => {
        this.snackBar.open('Brouillon enregistré.', 'OK', { duration: 3000 });
        this.router.navigate(['/mes-demandes']);
      },
      error: (err) => {
        applyApiErrorsToForm(this.state.form, err);
        this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
      },
    });
  }

  submit(): void {
    if (!this.state.validateStep(3)) {
      this.snackBar.open('Veuillez accepter les conditions.', 'Fermer', { duration: 4000 });
      return;
    }
    this.state
      .saveProgress()
      .pipe(switchMap(() => this.state.finalizeSubmit()))
      .subscribe({
        next: (loan) => {
          this.snackBar.open(`Demande ${loan.reference} soumise avec succès.`, 'OK', {
            duration: 5000,
          });
          this.router.navigate(['/mes-demandes'], {
            queryParams: { submitted: loan.reference },
          });
        },
        error: (err) => {
          applyApiErrorsToForm(this.state.form, err);
          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 6000 });
        },
      });
  }
}
