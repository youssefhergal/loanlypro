import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-wizard-advice-panel',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <aside class="advice-panel" [class.advice-panel--info]="icon === 'info'">
      <div class="advice-panel__header">
        <mat-icon class="advice-panel__icon">{{ icon }}</mat-icon>
        <span class="advice-panel__title">{{ title }}</span>
      </div>
      <p class="advice-panel__text">{{ message }}</p>
    </aside>
  `,
  styles: `
    .advice-panel {
      background: #fffbeb;
      border: 1px solid #fde68a;
      border-radius: 12px;
      padding: 0.55rem 0.75rem;
    }

    .advice-panel__header {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      margin-bottom: 0.35rem;
    }

    .advice-panel__icon {
      color: #d97706;
      font-size: 1.05rem;
      width: 1.05rem;
      height: 1.05rem;
    }

    .advice-panel__title {
      font-size: 0.8125rem;
      font-weight: 700;
      color: #92400e;
    }

    .advice-panel__text {
      margin: 0;
      font-size: 0.8125rem;
      line-height: 1.5;
      color: #78350f;
    }

    .advice-panel--info {
      background: #eff6ff;
      border-color: #bfdbfe;
    }

    .advice-panel--info .advice-panel__icon {
      color: #2563eb;
    }

    .advice-panel--info .advice-panel__title {
      color: #1e40af;
    }

    .advice-panel--info .advice-panel__text {
      color: #1e3a8a;
    }
  `,
})
export class WizardAdvicePanelComponent {
  @Input() icon = 'lightbulb';
  @Input() title = 'Conseil';
  @Input() message =
    'Une durée plus longue réduit la mensualité mais augmente le coût total. Choisissez selon votre capacité de remboursement.';
}
