import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-coming-soon',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <div class="coming-soon">
      <mat-card>
        <mat-card-content>
          <p>Cette section sera disponible prochainement.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .coming-soon {
        padding: 0;
        max-width: 800px;
      }
    `,
  ],
})
export class ComingSoonComponent {
  private readonly route = inject(ActivatedRoute);
}
