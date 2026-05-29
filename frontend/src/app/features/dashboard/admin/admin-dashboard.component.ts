import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../core/auth/services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <div class="admin-dashboard">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Espace administrateur</mat-card-title>
          <mat-card-subtitle>
            Bonjour {{ auth.currentUser()?.firstName }} — supervision de la plateforme LoanlyFans.
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>
            La liste des demandes et la supervision des dossiers seront disponibles dans une prochaine
            version. Utilisez l’API ou Swagger pour les opérations back-office en attendant.
          </p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .admin-dashboard {
        max-width: 720px;
      }
    `,
  ],
})
export class AdminDashboardComponent {
  readonly auth = inject(AuthService);
}
