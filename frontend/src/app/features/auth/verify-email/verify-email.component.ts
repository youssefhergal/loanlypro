import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../core/auth/services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss',
})
export class VerifyEmailComponent {
  token = '000000';
  loading = false;
  error = '';
  verified = false;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  verify(): void {
    this.error = '';
    const code = this.token.trim();
    if (!code) {
      this.error = 'Veuillez saisir le code reçu par e-mail.';
      return;
    }
    if (code !== '000000') {
      this.error = 'Code invalide (utilise 000000 pour le moment).';
      return;
    }
    this.loading = true;
    this.verified = true;
    this.loading = false;
  }
}
