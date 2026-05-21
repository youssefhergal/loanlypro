import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/services/auth.service';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [NavbarComponent, MatCardModule, MatButtonModule],
  template: `
    <app-navbar />
    <div class="page">
      <mat-card>
        <mat-card-title>Espace administrateur</mat-card-title>
        <mat-card-content><p>Gestion des comptes, paramétrage.</p></mat-card-content>
        <mat-card-actions>
          <button mat-button color="warn" (click)="auth.logout()">Déconnexion</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`.page { padding: 1.5rem; }`],
})
export class AdminComponent {
  readonly auth = inject(AuthService);
}
