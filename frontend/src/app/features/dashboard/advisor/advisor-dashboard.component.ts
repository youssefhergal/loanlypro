import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../core/auth/services/auth.service';

@Component({
  selector: 'app-advisor-dashboard',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <div class="advisor-dashboard">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Espace conseiller</mat-card-title>
          <mat-card-subtitle>
            Bonjour {{ auth.currentUser()?.firstName }} — instruction des dossiers de prêt.
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>
            La file de dossiers et l’écran d’instruction seront disponibles dans une prochaine version.
            Utilisez l’API ou Swagger pour tester les actions conseiller en attendant.
          </p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .advisor-dashboard {
        max-width: 720px;
      }
    `,
  ],
})
export class AdvisorDashboardComponent {
  readonly auth = inject(AuthService);
}
