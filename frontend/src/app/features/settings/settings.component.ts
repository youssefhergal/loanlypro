import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HELP_CONTACT } from '../../core/help/constants/help-faq.constants';
import { UserSettingsService } from '../../core/settings/user-settings.service';
import {
  DEFAULT_USER_SETTINGS,
  SettingsRole,
  UserSettings,
} from '../../core/settings/models/user-settings.model';

interface SettingsCopy {
  intro: string;
  emailHint: string;
  alertsLabel: string;
  alertsHint: string;
  profileLink: string;
  helpLink: string;
}

const SETTINGS_COPY: Record<SettingsRole, SettingsCopy> = {
  client: {
    intro:
      'Personnalisez votre espace client : affichage, alertes et rappels sur vos dossiers.',
    emailHint: 'Récapitulatifs et mises à jour importantes sur vos demandes de prêt.',
    alertsLabel: 'Alertes sur mes dossiers',
    alertsHint: 'Documents refusés, décisions et échéances de prêt.',
    profileLink: '/profil',
    helpLink: '/aide',
  },
  advisor: {
    intro:
      'Configurez votre poste de travail conseiller : menu, alertes dossiers et notifications.',
    emailHint: 'Synthèse des dossiers assignés et rappels de traitement.',
    alertsLabel: 'Alertes dossiers assignés',
    alertsHint: 'Nouveaux dossiers, documents client et dossiers en attente.',
    profileLink: '/conseiller/profil',
    helpLink: '/conseiller/aide',
  },
  admin: {
    intro:
      'Préférences d’affichage et alertes pour la supervision de la plateforme LoanlyPro.',
    emailHint: 'Alertes de supervision (demandes non affectées, retards).',
    alertsLabel: 'Alertes de supervision',
    alertsHint: 'Demandes non affectées et prêts en retard de paiement.',
    profileLink: '/admin/profil',
    helpLink: '/admin/aide',
  },
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatSnackBarModule,
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly settingsService = inject(UserSettingsService);
  private readonly snackBar = inject(MatSnackBar);

  readonly supportEmail = HELP_CONTACT.supportEmail;
  readonly role = signal<SettingsRole>('client');
  readonly settings = signal<UserSettings>({ ...DEFAULT_USER_SETTINGS });
  readonly saved = signal(false);

  readonly copy = computed(() => SETTINGS_COPY[this.role()]);

  ngOnInit(): void {
    const role = this.route.snapshot.data['settingsRole'] as SettingsRole | undefined;
    this.role.set(role ?? 'client');
    const loaded = this.settingsService.load(this.role());
    this.settings.set(loaded);
    this.settingsService.applyToDocument(loaded);
  }

  update<K extends keyof UserSettings>(key: K, value: UserSettings[K]): void {
    this.settings.update((current) => ({ ...current, [key]: value }));
    this.persist(false);
  }

  save(): void {
    this.persist(true);
  }

  reset(): void {
    this.settings.set(this.settingsService.reset(this.role()));
    this.saved.set(true);
    this.snackBar.open('Préférences réinitialisées.', 'OK', { duration: 3000 });
  }

  private persist(showToast: boolean): void {
    this.settingsService.save(this.role(), this.settings());
    this.saved.set(true);
    if (showToast) {
      this.snackBar.open('Préférences enregistrées.', 'OK', { duration: 3000 });
    }
  }
}
