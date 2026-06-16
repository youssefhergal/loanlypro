import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { LoanHistoryEventResponseDto } from '../../../../../core/loans/models/loan-history.model';
import { historyEventsToTimeline } from '../../../../../core/loans/utils/loan-detail.util';

@Component({
  selector: 'app-repayment-history-timeline',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <section class="repayment-history">
      <h3 class="repayment-history__title">
        <mat-icon>history</mat-icon>
        Historique du prêt
      </h3>
      @if (loading()) {
        <p class="repayment-history__muted">Chargement de l'historique…</p>
      } @else if (!timeline().length) {
        <p class="repayment-history__muted">
          L'historique sera disponible après le déblocage des fonds.
        </p>
      } @else {
        <ol class="repayment-timeline">
          @for (event of timeline(); track event.title + event.dateLabel) {
            <li
              class="repayment-timeline__item"
              [class.repayment-timeline__item--done]="event.state === 'done'"
              [class.repayment-timeline__item--current]="event.state === 'current'"
              [class.repayment-timeline__item--warn]="event.state === 'warn'"
            >
              <span class="repayment-timeline__dot" aria-hidden="true"></span>
              <div>
                <div class="repayment-timeline__head">
                  <h4>{{ event.title }}</h4>
                  <span class="repayment-timeline__date">{{ event.dateLabel }}</span>
                </div>
                <p>{{ event.description }}</p>
              </div>
            </li>
          }
        </ol>
      }
    </section>
  `,
  styleUrl: './repayment-history-timeline.component.scss',
})
export class RepaymentHistoryTimelineComponent {
  readonly events = input<LoanHistoryEventResponseDto[]>([]);
  readonly loading = input(false);

  timeline() {
    return historyEventsToTimeline(this.events());
  }
}
