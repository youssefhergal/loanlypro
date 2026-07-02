import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AiLearningService } from '../../core/ai-learning/services/ai-learning.service';
import type { LearningSession, LearningStep } from '../../core/ai-learning/models/learning.model';
import { mdToHtml } from '../../core/shared/utils/markdown.util';

type ViewState = 'goal-input' | 'roadmap' | 'step-detail';

const SUGGESTIONS = [
  'Gérer mon budget mensuel',
  'Comprendre comment investir',
  'Rembourser mes dettes efficacement',
  'Préparer ma retraite',
  'Acheter mon premier bien immobilier',
];

@Component({
  selector: 'app-ai-learning',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './ai-learning.component.html',
  styleUrl: './ai-learning.component.scss',
})
export class AiLearningComponent implements OnInit {
  @ViewChild('goalInput') goalInputRef!: ElementRef<HTMLTextAreaElement>;

  private readonly api = inject(AiLearningService);
  private readonly destroyRef = inject(DestroyRef);

  readonly view = signal<ViewState>('goal-input');
  readonly session = signal<LearningSession | null>(null);
  readonly pastSessions = signal<LearningSession[]>([]);
  readonly activeStepIndex = signal<number | null>(null);
  readonly stepContent = signal<string | null>(null);
  readonly loadingRoadmap = signal(false);
  readonly loadingContent = signal(false);
  readonly completingStep = signal(false);
  readonly suggestions = SUGGESTIONS;

  readonly goalCtrl = new FormControl('', [Validators.required, Validators.minLength(5)]);

  ngOnInit(): void {
    this.api.getSessions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (s) => this.pastSessions.set(s), error: () => {} });
  }

  generateRoadmap(objective?: string): void {
    const goal = objective ?? this.goalCtrl.value?.trim() ?? '';
    if (!goal || this.loadingRoadmap()) return;

    if (objective) this.goalCtrl.setValue(objective);
    this.loadingRoadmap.set(true);
    this.goalCtrl.disable();

    this.api.generateRoadmap(goal)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (s) => {
          this.session.set(s);
          this.pastSessions.update((list) => [s, ...list]);
          this.view.set('roadmap');
          this.loadingRoadmap.set(false);
          this.goalCtrl.enable();
        },
        error: () => {
          this.loadingRoadmap.set(false);
          this.goalCtrl.enable();
        },
      });
  }

  resumeSession(session: LearningSession): void {
    this.session.set(session);
    this.view.set('roadmap');
  }

  openStep(stepIndex: number): void {
    const s = this.session();
    if (!s || this.loadingContent()) return;

    this.activeStepIndex.set(stepIndex);
    this.stepContent.set(null);
    this.view.set('step-detail');
    this.loadingContent.set(true);

    this.api.getStepContent(s.id, stepIndex)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.stepContent.set(res.content);
          this.loadingContent.set(false);
        },
        error: () => {
          this.stepContent.set("Erreur lors du chargement du contenu. Veuillez réessayer.");
          this.loadingContent.set(false);
        },
      });
  }

  completeStep(): void {
    const s = this.session();
    const idx = this.activeStepIndex();
    if (!s || idx === null || this.completingStep()) return;

    this.completingStep.set(true);
    this.api.completeStep(s.id, idx)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.session.set(updated);
          this.completingStep.set(false);
          const nextIndex = idx + 1;
          if (nextIndex < updated.steps.length) {
            this.openStep(nextIndex);
          } else {
            this.view.set('roadmap');
            this.activeStepIndex.set(null);
          }
        },
        error: () => this.completingStep.set(false),
      });
  }

  backToRoadmap(): void {
    this.view.set('roadmap');
    this.activeStepIndex.set(null);
    this.stepContent.set(null);
  }

  backToGoal(): void {
    this.view.set('goal-input');
    this.session.set(null);
    this.activeStepIndex.set(null);
    this.stepContent.set(null);
  }

  isStepCompleted(stepIndex: number): boolean {
    return this.session()?.completedSteps?.includes(stepIndex) ?? false;
  }

  isStepActive(stepIndex: number): boolean {
    return this.activeStepIndex() === stepIndex;
  }

  get progressPercent(): number {
    const s = this.session();
    if (!s || s.totalSteps === 0) return 0;
    return Math.round((s.completedCount / s.totalSteps) * 100);
  }

  get activeStep(): LearningStep | null {
    const s = this.session();
    const idx = this.activeStepIndex();
    if (!s || idx === null) return null;
    return s.steps[idx] ?? null;
  }

  formatContent(content: string): string {
    return mdToHtml(content);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
