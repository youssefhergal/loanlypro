import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/services/auth.service';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-conseiller',
  standalone: true,
  imports: [NavbarComponent, MatCardModule, MatButtonModule],
  template: `
    <app-navbar />
    <div class="page">
      <mat-card>
        <mat-card-title>Espace conseiller</mat-card-title>
        <mat-card-content><p>Instruction des demandes, suivi des dossiers.</p></mat-card-content>
        <mat-card-actions>
          <button mat-button color="warn" (click)="auth.logout()">Déconnexion</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`.page { padding: 1.5rem; }`],
})
export class ConseillerComponent {
  readonly auth = inject(AuthService);
}
