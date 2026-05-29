import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/services/auth.service';
import { ROLES } from '../../../core/auth/constants/auth.constants';
import { NavbarComponent } from '../../navbar/navbar.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    NavbarComponent,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';
  hidePassword = true;

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(4)]],
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    const { email, password } = this.loginForm.value;

    this.auth.login({ email, password }).subscribe({
      next: () => this.redirectAfterLogin(),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message ?? 'Identifiants incorrects.';
      },
    });
  }

  private redirectAfterLogin(): void {
    this.loading = false;
    const user = this.auth.currentUser();
    if (user?.roles.includes(ROLES.ADMIN)) {
      this.router.navigate(['/admin/dashboard']);
    } else if (user?.roles.includes(ROLES.CONSEILLER)) {
      this.router.navigate(['/conseiller/dashboard']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
