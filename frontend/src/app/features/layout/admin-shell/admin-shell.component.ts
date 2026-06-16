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
import type { User } from '../../../core/auth/models/user.model';
import type { BreadcrumbItem } from '../client-shell/client-shell.component';

const SIDEBAR_COLLAPSED_KEY = 'lf-admin-sidebar-collapsed';

@Component({
  selector: 'app-admin-shell',
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
  ],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
})
export class AdminShellComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly sidebarCollapsed = signal(this.readSidebarPreference());
  readonly breadcrumbs = signal<BreadcrumbItem[]>([]);
  readonly pageTitle = signal('');
  readonly pageDescription = signal('');

  constructor(public readonly auth: AuthService) {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.updatePageContext());

    this.updatePageContext();
  }

  private updatePageContext(): void {
    let leaf = this.router.routerState.snapshot.root;
    while (leaf.firstChild) {
      leaf = leaf.firstChild;
    }

    const routeTitle = (leaf.data['title'] as string | undefined) ?? 'Accueil';
    const routeDescription = (leaf.data['description'] as string | undefined) ?? '';

    let path = this.router.url.split('?')[0].split('#')[0];
    if (path === '/admin' || path === '/admin/') {
      path = '/admin/dashboard';
    }

    const isDashboard = path === '/admin/dashboard';
    const repaymentDetailMatch = /^\/admin\/prets\/(\d+)$/.exec(path);

    let pageTitle = routeTitle;
    let pageDescription = routeDescription;
    let breadcrumbCurrentLabel = routeTitle;

    if (isDashboard) {
      const prenom = this.auth.currentUser()?.firstName?.trim() || 'admin';
      pageTitle = `Bonjour ${prenom}`;
      pageDescription = 'Supervision de la plateforme';
      breadcrumbCurrentLabel = 'Accueil';
    }

    const crumbs: BreadcrumbItem[] = [];
    if (!isDashboard) {
      crumbs.push({ label: 'Accueil', routerLink: '/admin/dashboard' });
    }
    if (repaymentDetailMatch) {
      crumbs.push({ label: 'Prêts', routerLink: '/admin/prets' });
      crumbs.push({ label: 'Détail du prêt', routerLink: path });
    } else if (path === '/admin/prets') {
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
