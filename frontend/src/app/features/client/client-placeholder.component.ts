import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-client-placeholder',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <div class="page">
      <mat-card>
        <mat-card-content>
          <p>Cette section sera disponible prochainement.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .page {
        padding: 1.5rem;
        max-width: 800px;
      }
    `,
  ],
})
export class ClientPlaceholderComponent {}
