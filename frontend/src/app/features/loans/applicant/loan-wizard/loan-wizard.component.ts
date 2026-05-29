import { isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { LoanWizardStateService } from '../../../../core/loans/services/loan-wizard-state.service';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { StepNeedComponent } from './steps/step-need.component';
import { StepSituationComponent } from './steps/step-situation.component';
import { StepDocumentsComponent } from './steps/step-documents.component';
import { StepSummaryComponent } from './steps/step-summary.component';
import { applyApiErrorsToForm, getErrorMessage } from '../../../../core/loans/utils/api-error.util';
import { switchMap, filter } from 'rxjs';
import { ConfirmDialogService } from '../../../../shared/confirm-dialog/confirm-dialog.service';

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
export class LoanWizardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly platformId = inject(PLATFORM_ID);
  readonly state = inject(LoanWizardStateService);
  private readonly loanApi = inject(LoanApiService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  private pageHeaderResize?: ResizeObserver;

  readonly steps = [
    { label: 'Besoin' },
    { label: 'Situation' },
    { label: 'Documents' },
    { label: 'Récapitulatif' },
  ] as const;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    this.state.initFromRoute(Number.isFinite(id) && id ? id : null).subscribe({
      next: () => {
        this.loading.set(false);
        this.scheduleStickyOffsetSync();
      },
      error: (err) => {
        this.loadError.set(getErrorMessage(err, 'Impossible de charger le dossier.'));
        this.loading.set(false);
      },
    });
  }

  ngAfterViewInit(): void {
    this.scheduleStickyOffsetSync();
  }

  ngOnDestroy(): void {
    this.pageHeaderResize?.disconnect();
  }

  /** Aligne le stepper sticky sous le bandeau titre (hauteur mesurée). */
  private scheduleStickyOffsetSync(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    requestAnimationFrame(() => this.syncPageHeaderStickyOffset());
  }

  private syncPageHeaderStickyOffset(): void {
    const header = document.querySelector('.main > .page-header');
    const wizardPage = document.querySelector('app-loan-wizard .wizard-page');
    if (!header || !wizardPage) {
      return;
    }

    const apply = () => {
      const height = header.getBoundingClientRect().height;
      (wizardPage as HTMLElement).style.setProperty(
        '--page-header-sticky-offset',
        `${height}px`
      );
    };

    apply();

    if (!this.pageHeaderResize) {
      this.pageHeaderResize = new ResizeObserver(apply);
      this.pageHeaderResize.observe(header);
    }
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

  saveDraft(): void {
    const step = this.state.currentStep();
    if (!this.state.validateStep(step)) {
      this.snackBar.open('Veuillez corriger les champs obligatoires.', 'Fermer', {
        duration: 4000,
      });
      return;
    }
    this.state.saveProgress().subscribe({
      next: () => {
        this.snackBar.open('Brouillon enregistré.', 'OK', { duration: 3000 });
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
    this.confirmDialog
      .open({
        title: 'Soumettre votre demande ?',
        message:
          'Après soumission, vous ne pourrez plus modifier le formulaire ni les documents.\n\nConfirmez-vous l’envoi définitif de votre dossier ?',
        confirmLabel: 'Soumettre',
        confirmColor: 'primary',
      })
      .pipe(
        filter((ok) => ok),
        switchMap(() => this.state.saveProgress()),
        switchMap(() => this.state.finalizeSubmit())
      )
      .subscribe({
        next: (loan) => {
          this.router.navigate(['/demande-soumise', loan.id]);
        },
        error: (err) => {
          applyApiErrorsToForm(this.state.form, err);
          this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 6000 });
        },
      });
  }

  deleteDraft(): void {
    const id = this.state.applicationId();
    if (!id) {
      return;
    }
    this.confirmDialog
      .open({
        title: 'Supprimer ce brouillon ?',
        message: 'Cette action est irréversible. Toutes les données et documents associés seront perdus.',
        confirmLabel: 'Supprimer',
        confirmColor: 'warn',
      })
      .pipe(filter((ok) => ok))
      .subscribe(() => {
        this.loanApi.deleteApplication(id).subscribe({
          next: () => {
            this.state.clearSession();
            this.snackBar.open('Brouillon supprimé.', 'OK', { duration: 3000 });
            this.router.navigate(['/mes-demandes']);
          },
          error: (err) => {
            this.snackBar.open(getErrorMessage(err), 'Fermer', { duration: 5000 });
          },
        });
      });
  }
}
