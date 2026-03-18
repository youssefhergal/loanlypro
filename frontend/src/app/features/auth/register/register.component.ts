import { Component } from '@angular/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = false;
  error = '';
  hidePassword = true;
  hidePasswordConfirm = true;

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {
    this.registerForm = this.fb.group(
      {
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        email: ['', [Validators.required, Validators.email]],
        phone: [''],
        password: ['', [Validators.required, Validators.minLength(6)]],
        passwordConfirm: ['', [Validators.required]],
      },
      { validators: (g) => this.passwordMatchValidator(g) }
    );
  }

  private passwordMatchValidator(g: AbstractControl): { mismatch: boolean } | null {
    const group = g as FormGroup;
    const pass = group.get('password')?.value;
    const confirm = group.get('passwordConfirm')?.value;
    if (!pass || !confirm) return null;
    return pass === confirm ? null : { mismatch: true };
  }

  submit(): void {
    this.error = '';
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      if (this.registerForm.hasError('mismatch')) {
        this.error = 'Les deux mots de passe doivent être identiques.';
      }
      return;
    }
    this.loading = true;
    const v = this.registerForm.value;
    this.auth
      .register({
        email: v.email,
        firstname: v.firstName,
        lastname: v.lastName,
        password: v.password,
      })
      .subscribe({
        next: () => this.router.navigate(['/verify-email']),
        error: (err) => {
          this.loading = false;
          this.error = err?.error?.message ?? 'Une erreur est survenue.';
        },
      });
  }
}
