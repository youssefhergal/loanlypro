import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { CreateUserDialogComponent } from './create-user-dialog/create-user-dialog.component';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../../../environments/environment';

type RoleName = 'ROLE_CLIENT' | 'ROLE_CONSEILLER' | 'ROLE_ADMIN';

interface UserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: RoleName[] | string[];
  emailVerified: boolean;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const ROLE_OPTIONS = [
  { value: '', label: 'Tous les rôles' },
  { value: 'CLIENT', label: 'Clients' },
  { value: 'CONSEILLER', label: 'Conseillers' },
  { value: 'ADMIN', label: 'Administrateurs' },
] as const;

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly baseUrl = `${environment.apiUrl}/v1/admin/users`;

  readonly roleOptions = ROLE_OPTIONS;
  readonly displayedColumns = ['user', 'roles', 'verified'] as const;
  readonly pageSizeOptions = [10, 25, 50];

  readonly q = signal('');
  readonly role = signal('');
  readonly page = signal(0);
  readonly size = signal(10);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly users = signal<UserDto[]>([]);
  readonly total = signal(0);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));

  readonly verifiedOnPage = computed(
    () => this.users().filter((u) => u.emailVerified).length,
  );
  readonly unverifiedOnPage = computed(
    () => this.users().filter((u) => !u.emailVerified).length,
  );
  readonly clientsOnPage = computed(
    () => this.users().filter((u) => u.roles.some((r) => r.includes('CLIENT'))).length,
  );
  readonly staffOnPage = computed(
    () =>
      this.users().filter((u) =>
        u.roles.some((r) => r.includes('CONSEILLER') || r.includes('ADMIN')),
      ).length,
  );
  readonly rangeStart = computed(() =>
    this.total() === 0 ? 0 : this.page() * this.size() + 1,
  );
  readonly rangeEnd = computed(() =>
    Math.min((this.page() + 1) * this.size(), this.total()),
  );
  readonly activeFilterLabel = computed(() => {
    const parts: string[] = [];
    if (this.q().trim()) parts.push(`« ${this.q().trim()} »`);
    const roleOpt = ROLE_OPTIONS.find((o) => o.value === this.role());
    if (roleOpt?.value) parts.push(roleOpt.label.toLowerCase());
    return parts.length ? parts.join(' · ') : null;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    let params = new HttpParams()
      .set('page', this.page().toString())
      .set('size', this.size().toString());
    if (this.q().trim()) params = params.set('q', this.q().trim());
    if (this.role().trim()) params = params.set('role', this.role().trim());

    this.http.get<PageResponse<UserDto>>(this.baseUrl, { params }).subscribe({
      next: (res) => {
        this.users.set(res.content ?? []);
        this.total.set(res.totalElements ?? res.content?.length ?? 0);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Échec du chargement des utilisateurs.');
        this.loading.set(false);
      },
    });
  }

  refresh(): void {
    this.load();
  }

  applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  resetFilters(): void {
    this.q.set('');
    this.role.set('');
    this.applyFilters();
  }

  onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
    this.load();
  }

  openCreateUserDialog(): void {
    const ref = this.dialog.open(CreateUserDialogComponent, {
      width: '520px',
      panelClass: 'admin-users-dialog-panel',
    });

    ref.afterClosed().subscribe((created) => {
      if (created) {
        this.snackBar.open('Compte créé avec succès.', 'Fermer', { duration: 4000 });
        this.applyFilters();
      }
    });
  }

  initials(user: UserDto): string {
    const first = user.firstName?.trim().charAt(0) ?? '';
    const last = user.lastName?.trim().charAt(0) ?? '';
    return (first + last).toUpperCase() || '?';
  }

  formatRole(role: string): string {
    return role.replace('ROLE_', '');
  }

  roleBadgeClass(role: string): string {
    if (role.includes('ADMIN')) return 'role-badge--admin';
    if (role.includes('CONSEILLER')) return 'role-badge--advisor';
    return 'role-badge--client';
  }

  avatarClass(user: UserDto): string {
    const roles = user.roles.map((r) => r.toUpperCase());
    if (roles.some((r) => r.includes('ADMIN'))) return 'user-avatar--admin';
    if (roles.some((r) => r.includes('CONSEILLER'))) return 'user-avatar--advisor';
    return 'user-avatar--client';
  }

}
