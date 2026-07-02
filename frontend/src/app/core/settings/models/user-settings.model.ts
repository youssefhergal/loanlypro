export type SettingsRole = 'client' | 'advisor' | 'admin';

export interface UserSettings {
  sidebarCollapsedByDefault: boolean;
  emailNotifications: boolean;
  inAppAlerts: boolean;
  compactTables: boolean;
}

export const DEFAULT_USER_SETTINGS: UserSettings = {
  sidebarCollapsedByDefault: false,
  emailNotifications: true,
  inAppAlerts: true,
  compactTables: false,
};
