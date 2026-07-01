import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
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
  size: number; // page size
  number: number; // current page index (0-based)
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDividerModule,
  ],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/admin/users`;

  // Filtres & pagination
  readonly q = signal<string>('');
  readonly role = signal<string>(''); // '', 'CLIENT', 'CONSEILLER', 'ADMIN'
  readonly page = signal<number>(0);
  readonly size = signal<number>(10);

  // Etat liste
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly users = signal<UserDto[]>([]);
  readonly total = signal<number>(0);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));

  // Formulaire création
  readonly cEmail = signal('');
  readonly cFirstName = signal('');
  readonly cLastName = signal('');
  readonly cPassword = signal('');
  readonly cRole = signal<'CONSEILLER' | 'ADMIN'>('CONSEILLER');
  readonly creating = signal(false);
  readonly createError = signal<string | null>(null);
  readonly createSuccess = signal<string | null>(null);

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

    this.http
      .get<PageResponse<UserDto>>(this.baseUrl, { params })
      .subscribe({
        next: (res) => {
          this.users.set(res.content ?? []);
          this.total.set(res.totalElements ?? (res.content?.length ?? 0));
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err?.error?.message || 'Échec du chargement des utilisateurs.');
          this.loading.set(false);
        },
      });
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

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    const current = this.page();
    const totalPages = this.totalPages();
    if (current + 1 < totalPages) {
      this.page.set(current + 1);
      this.load();
    }
  }

  createUser(): void {
    this.createError.set(null);
    this.createSuccess.set(null);
    this.creating.set(true);

    const payload = {
      email: this.cEmail().trim(),
      firstName: this.cFirstName().trim(),
      lastName: this.cLastName().trim(),
      password: this.cPassword(),
      role: this.cRole(),
    } as const;

    this.http.post<UserDto>(this.baseUrl, payload).subscribe({
      next: () => {
        this.creating.set(false);
        this.createSuccess.set('Utilisateur créé avec succès.');
        // reset minimal
        this.cEmail.set('');
        this.cFirstName.set('');
        this.cLastName.set('');
        this.cPassword.set('');
        this.cRole.set('CONSEILLER');
        // recharger la liste
        this.applyFilters();
      },
      error: (err) => {
        this.creating.set(false);
        this.createError.set(
          err?.error?.message || 'Échec de la création. Vérifiez les champs et réessayez.'
        );
      },
    });
  }
}
