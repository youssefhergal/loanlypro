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

function mdToHtml(md: string): string {
  let s = md
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```[\w]*\n?([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`\n]+)`/g, '<code>$1</code>')
    .replace(/^#{4}\s+(.+)$/gm, '<h4>$1</h4>')
    .replace(/^#{3}\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^#{2}\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#{1}\s+(.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^>\s?(.+)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^---+$/gm, '<hr>');

  // Lists
  s = s.replace(/^(\s*[-*+]\s.+)(\n\s*[-*+]\s.+)*/gm, (block) => {
    const items = block.replace(/^\s*[-*+]\s(.+)$/gm, '<li>$1</li>');
    return `<ul>${items}</ul>`;
  });
  s = s.replace(/^(\s*\d+\.\s.+)(\n\s*\d+\.\s.+)*/gm, (block) => {
    const items = block.replace(/^\s*\d+\.\s(.+)$/gm, '<li>$1</li>');
    return `<ol>${items}</ol>`;
  });

  // Paragraphs
  s = s.split(/\n{2,}/).map((para) => {
    const t = para.trim();
    if (!t || /^<(h[1-6]|ul|ol|pre|blockquote|hr)/.test(t)) return t;
    return `<p>${t.replace(/\n/g, '<br>')}</p>`;
  }).join('\n');

  return s;
}

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
