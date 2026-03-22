import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <div class="page">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Votre compte</mat-card-title>
          <mat-card-subtitle>
            Connecté : {{ auth.currentUser()?.firstName }} {{ auth.currentUser()?.lastName }}
            ({{ auth.currentUser()?.email }})
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>Rôles : {{ auth.currentUser()?.roles?.join(', ') }}</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`.page { padding: 1.5rem; max-width: 800px; margin: 0 auto; }`],
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
}
