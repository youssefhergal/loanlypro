import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { NavbarComponent } from '../../navbar/navbar.component';
import { AuthService } from '../../../core/auth/services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [
    NavbarComponent,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss',
})
export class VerifyEmailComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  email = '';
  token = '';
  loading = false;
  resending = false;
  error = '';
  successMessage = '';
  verified = false;

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email')?.trim() ?? '';
  }

  verify(): void {
    this.error = '';
    this.successMessage = '';
    const code = this.token.trim();
    const email = this.email.trim();

    if (!email) {
      this.error = 'Adresse e-mail manquante. Revenez à l’inscription.';
      return;
    }
    if (!code) {
      this.error = 'Veuillez saisir le code reçu par e-mail.';
      return;
    }

    this.loading = true;
    this.auth.verifyEmail(email, code).subscribe({
      next: (response) => {
        this.loading = false;
        this.verified = true;
        this.successMessage = response.message;
        setTimeout(() => void this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Code invalide ou expiré.';
      },
    });
  }

  resend(): void {
    this.error = '';
    this.successMessage = '';
    const email = this.email.trim();
    if (!email) {
      this.error = 'Adresse e-mail manquante.';
      return;
    }

    this.resending = true;
    this.auth.resendVerificationEmail(email).subscribe({
      next: () => {
        this.resending = false;
        this.successMessage = 'Un nouveau code vous a été envoyé.';
      },
      error: (err) => {
        this.resending = false;
        this.error = err?.error?.message ?? 'Impossible de renvoyer le code.';
      },
    });
  }
}
