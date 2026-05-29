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
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/auth/services/auth.service';
import type { User } from '../../../core/auth/models/user.model';

const SIDEBAR_COLLAPSED_KEY = 'lf-client-sidebar-collapsed';

export interface BreadcrumbItem {
  label: string;
  routerLink: string;
}

@Component({
  selector: 'app-client-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatBadgeModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  templateUrl: './client-shell.component.html',
  styleUrl: './client-shell.component.scss',
})
export class ClientShellComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Rail replié (icônes seules) — uniquement appliqué en CSS au-dessus de ~900px */
  readonly sidebarCollapsed = signal(this.readSidebarPreference());

  /** Fil d’Ariane (topbar gauche) */
  readonly breadcrumbs = signal<BreadcrumbItem[]>([]);
  /** Titre et sous-titre affichés au-dessus du contenu principal */
  readonly pageTitle = signal('');
  readonly pageDescription = signal('');
  /** Bouton « Nouvelle demande » à droite du titre / description */
  readonly showNewRequestCta = signal(false);

  constructor(public readonly auth: AuthService) {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.updatePageContext());

    this.updatePageContext();
  }

  /** Met à jour titre, description et fil d’Ariane à partir des `data` de la route active */
  private updatePageContext(): void {
    let leaf = this.router.routerState.snapshot.root;
    while (leaf.firstChild) {
      leaf = leaf.firstChild;
    }

    const routeTitle =
      (leaf.data['title'] as string | undefined) ?? 'Tableau de bord';
    const routeDescription =
      (leaf.data['description'] as string | undefined) ?? '';

    let path = this.router.url.split('?')[0].split('#')[0];
    if (path === '' || path === '/') {
      path = '/dashboard';
    }

    const isDashboard = path === '/dashboard';
    const loanDetailMatch = /^\/mes-demandes\/(\d+)$/.exec(path);

    let pageTitle = routeTitle;
    let pageDescription = routeDescription;
    let breadcrumbCurrentLabel = routeTitle;

    if (isDashboard) {
      const prenom =
        this.auth.currentUser()?.firstName?.trim() || 'bienvenue';
      pageTitle = `Bonjour ${prenom} 👋`;
      pageDescription = 'Voici un aperçu de votre espace';
      breadcrumbCurrentLabel = 'Accueil';
    }

    this.showNewRequestCta.set(isDashboard || path === '/mes-demandes');

    const crumbs: BreadcrumbItem[] = [];
    if (!isDashboard) {
      crumbs.push({ label: 'Accueil', routerLink: '/dashboard' });
    }
    if (loanDetailMatch) {
      crumbs.push({ label: 'Mes demandes', routerLink: '/mes-demandes' });
      crumbs.push({ label: 'Détail du dossier', routerLink: path });
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
