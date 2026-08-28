import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProfileApiService } from '../../core/profile/services/profile-api.service';
import type { User } from '../../core/auth/models/user.model';
import { AuthService } from '../../core/auth/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent implements OnInit {
  private readonly api = inject(ProfileApiService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly user = signal<User | null>(null);
  readonly loading = signal(false);
  readonly globalError = signal<string | null>(null);

  readonly emailSuccess = signal(false);
  readonly emailError = signal<string | null>(null);

  readonly passwordSuccess = signal(false);
  readonly passwordError = signal<string | null>(null);

  emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
  });

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  }, { validators: this.passwordsMatchValidator });

  ngOnInit(): void {
    this.fetchMe();
  }

  private fetchMe(): void {
    this.loading.set(true);
    this.globalError.set(null);
    this.api.getMe().subscribe({
      next: (u) => {
        this.user.set(u);
        this.emailForm.patchValue({ email: u.email });
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.globalError.set(this.extractMessage(err) ?? 'Erreur lors du chargement du profil');
      },
    });
  }

  onUpdateEmail(): void {
    if (this.emailForm.invalid) return;
    this.loading.set(true);
    this.emailSuccess.set(false);
    this.emailError.set(null);
    const payload = { email: this.emailForm.value.email! };
    this.api.updateEmail(payload).subscribe({
      next: (res) => {
        this.auth.updateSession(res.user, res.token);
        this.user.set(res.user);
        this.emailSuccess.set(true);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.emailError.set(this.extractMessage(err) ?? "Impossible de mettre à jour l'email");
      },
    });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) return;
    this.loading.set(true);
    this.passwordSuccess.set(false);
    this.passwordError.set(null);
    const payload = {
      currentPassword: this.passwordForm.value.currentPassword!,
      newPassword: this.passwordForm.value.newPassword!,
    };
    this.api.changePassword(payload).subscribe({
      next: () => {
        this.passwordSuccess.set(true);
        this.passwordForm.reset();
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.passwordError.set(this.extractMessage(err) ?? 'Impossible de changer le mot de passe');
      },
    });
  }

  private extractMessage(err: unknown): string | null {
    // Expect backend error shape { code, message, details }
    const anyErr = err as any;
    return anyErr?.error?.message ?? anyErr?.message ?? null;
  }

  private passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    if (!newPassword || !confirmPassword) {
      // Do not show mismatch error until both fields have a value
      return null;
    }
    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  showConfirmPasswordError(): boolean {
    const confirmCtrl = this.passwordForm.controls.confirmPassword;
    const touchedOrDirty = confirmCtrl.touched || confirmCtrl.dirty;
    return touchedOrDirty && (confirmCtrl.invalid || this.passwordForm.hasError('passwordMismatch'));
  }
}
