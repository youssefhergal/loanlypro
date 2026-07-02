import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/auth/services/auth.service';
import { NotificationApiService } from '../../../core/notifications/services/notification-api.service';
import { NotificationPageCoordinationService } from '../../../core/notifications/services/notification-page-coordination.service';
import { NotificationUnreadService } from '../../../core/notifications/services/notification-unread.service';
import { NotificationBellMenuComponent } from '../../../shared/notification-bell-menu/notification-bell-menu.component';
import { HelpFaqSearchFieldComponent } from '../../../shared/help-faq-search-field/help-faq-search-field.component';
import { AppLogoComponent } from '../../../shared/app-logo/app-logo.component';
import { HelpFaqSearchService } from '../../../core/help/services/help-faq-search.service';
import { UserSettingsService } from '../../../core/settings/user-settings.service';
import { isHelpFaqPath } from '../../../core/help/utils/help-faq.util';
import type { User } from '../../../core/auth/models/user.model';
import type { BreadcrumbItem } from '../client-shell/client-shell.component';

const SIDEBAR_COLLAPSED_KEY = 'lf-advisor-sidebar-collapsed';

@Component({
  selector: 'app-advisor-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    NotificationBellMenuComponent,
    HelpFaqSearchFieldComponent,
    AppLogoComponent,
  ],
  templateUrl: './advisor-shell.component.html',
  styleUrl: './advisor-shell.component.scss',
})
export class AdvisorShellComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificationApi = inject(NotificationApiService);
  private readonly notificationPageCoordination = inject(NotificationPageCoordinationService);
  private readonly helpFaqSearch = inject(HelpFaqSearchService);
  private readonly userSettings = inject(UserSettingsService);
  readonly notificationUnread = inject(NotificationUnreadService);

  readonly sidebarCollapsed = signal(this.readSidebarPreference());
  readonly breadcrumbs = signal<BreadcrumbItem[]>([]);
  readonly pageTitle = signal('');
  readonly pageDescription = signal('');
  readonly showMarkAllNotificationsCta = signal(false);
  readonly showHelpFaqSearch = signal(false);
  readonly markingAllNotifications = signal(false);

  constructor(public readonly auth: AuthService) {
    this.userSettings.applyToDocument(this.userSettings.load('advisor'));
    this.notificationUnread.refresh();
    this.notificationUnread.startPolling();

    this.destroyRef.onDestroy(() => this.notificationUnread.stopPolling());

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.updatePageContext();
        this.notificationUnread.refresh();
      });

    this.updatePageContext();
  }

  markAllNotificationsRead(): void {
    if (this.markingAllNotifications()) {
      return;
    }
    this.markingAllNotifications.set(true);
    this.notificationApi.markAllAsRead().subscribe({
      next: () => {
        this.notificationUnread.refresh();
        this.notificationPageCoordination.requestListRefresh();
        this.markingAllNotifications.set(false);
      },
      error: () => {
        this.markingAllNotifications.set(false);
      },
    });
  }

  private updatePageContext(): void {
    let leaf = this.router.routerState.snapshot.root;
    while (leaf.firstChild) {
      leaf = leaf.firstChild;
    }

    const routeTitle = (leaf.data['title'] as string | undefined) ?? 'Accueil';
    const routeDescription = (leaf.data['description'] as string | undefined) ?? '';
    const routeBreadcrumb =
      (leaf.data['breadcrumb'] as string | undefined) ?? routeTitle;

    let path = this.router.url.split('?')[0].split('#')[0];
    if (path === '/conseiller' || path === '/conseiller/') {
      path = '/conseiller/dashboard';
    }

    const isDashboard = path === '/conseiller/dashboard';
    const dossierDetailMatch = /^\/conseiller\/dossiers\/(\d+)$/.exec(path);
    const repaymentDetailMatch = /^\/conseiller\/prets\/(\d+)$/.exec(path);

    let pageTitle = routeTitle || routeBreadcrumb;
    let pageDescription = routeDescription;
    let breadcrumbCurrentLabel = routeBreadcrumb;

    if (isDashboard) {
      const prenom = this.auth.currentUser()?.firstName?.trim() || 'conseiller';
      pageTitle = `Bonjour ${prenom}`;
      pageDescription = 'Vue d’ensemble de votre activité d’instruction';
      breadcrumbCurrentLabel = 'Accueil';
    }

    this.showMarkAllNotificationsCta.set(path === '/conseiller/notifications');
    this.showHelpFaqSearch.set(isHelpFaqPath(path));
    if (!isHelpFaqPath(path)) {
      this.helpFaqSearch.clear();
    }

    const crumbs: BreadcrumbItem[] = [];
    if (!isDashboard) {
      crumbs.push({ label: 'Accueil', routerLink: '/conseiller/dashboard' });
    }
    if (dossierDetailMatch) {
      crumbs.push({ label: 'Mes dossiers', routerLink: '/conseiller/dossiers' });
      crumbs.push({ label: breadcrumbCurrentLabel, routerLink: path });
    } else if (repaymentDetailMatch) {
      crumbs.push({ label: 'Prêts', routerLink: '/conseiller/prets' });
      crumbs.push({ label: breadcrumbCurrentLabel, routerLink: path });
    } else {
      crumbs.push({ label: breadcrumbCurrentLabel, routerLink: path });
    }

    this.pageTitle.set(pageTitle);
    this.pageDescription.set(pageDescription);
    this.breadcrumbs.set(crumbs);
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => {
      const next = !v;
      try {
        localStorage.setItem(SIDEBAR_COLLAPSED_KEY, next ? '1' : '0');
      } catch {
        /* ignore */
      }
      return next;
    });
  }

  private readSidebarPreference(): boolean {
    try {
      return localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1';
    } catch {
      return false;
    }
  }

  initials(user: User): string {
    const a = user.firstName?.charAt(0) ?? '';
    const b = user.lastName?.charAt(0) ?? '';
    const s = (a + b).toUpperCase();
    return s || '?';
  }
}
