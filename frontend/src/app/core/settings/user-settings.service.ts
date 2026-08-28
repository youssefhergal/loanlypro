import { Injectable } from '@angular/core';
import {
  DEFAULT_USER_SETTINGS,
  SettingsRole,
  UserSettings,
} from './models/user-settings.model';

const STORAGE_PREFIX = 'lf-settings-';

const SIDEBAR_KEYS: Record<SettingsRole, string> = {
  client: 'lf-client-sidebar-collapsed',
  advisor: 'lf-advisor-sidebar-collapsed',
  admin: 'lf-admin-sidebar-collapsed',
};

@Injectable({ providedIn: 'root' })
export class UserSettingsService {
  load(role: SettingsRole): UserSettings {
    try {
      const raw = localStorage.getItem(this.storageKey(role));
      if (!raw) {
        return { ...DEFAULT_USER_SETTINGS };
      }
      const parsed = JSON.parse(raw) as Partial<UserSettings>;
      return { ...DEFAULT_USER_SETTINGS, ...parsed };
    } catch {
      return { ...DEFAULT_USER_SETTINGS };
    }
  }

  save(role: SettingsRole, settings: UserSettings): void {
    localStorage.setItem(this.storageKey(role), JSON.stringify(settings));
    localStorage.setItem(
      SIDEBAR_KEYS[role],
      settings.sidebarCollapsedByDefault ? '1' : '0',
    );
    this.applyToDocument(settings);
  }

  reset(role: SettingsRole): UserSettings {
    localStorage.removeItem(this.storageKey(role));
    localStorage.setItem(SIDEBAR_KEYS[role], '0');
    const defaults = { ...DEFAULT_USER_SETTINGS };
    this.applyToDocument(defaults);
    return defaults;
  }

  applyToDocument(settings: UserSettings): void {
    if (typeof document === 'undefined') {
      return;
    }
    document.documentElement.classList.toggle('compact-tables', settings.compactTables);
  }

  private storageKey(role: SettingsRole): string {
    return `${STORAGE_PREFIX}${role}`;
  }
}
