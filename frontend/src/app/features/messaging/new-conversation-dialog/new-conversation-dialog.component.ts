import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MessagingApiService } from '../../../core/messaging/services/messaging-api.service';
import type { Contact } from '../../../core/messaging/models/conversation.model';

@Component({
  selector: 'app-new-conversation-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './new-conversation-dialog.component.html',
  styleUrl: './new-conversation-dialog.component.scss',
})
export class NewConversationDialogComponent implements OnInit {
  private readonly api = inject(MessagingApiService);
  private readonly dialogRef = inject(MatDialogRef<NewConversationDialogComponent>);

  readonly contacts = signal<Contact[]>([]);
  readonly filtered = signal<Contact[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly selectedContact = signal<Contact | null>(null);

  readonly form = new FormGroup({
    search: new FormControl(''),
    message: new FormControl('', [Validators.required, Validators.maxLength(4000)]),
  });

  ngOnInit(): void {
    this.api.getContacts().subscribe({
      next: (contacts) => {
        this.contacts.set(contacts);
        this.filtered.set(contacts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.form.get('search')!.valueChanges.subscribe((q) => {
      const query = (q ?? '').toLowerCase();
      this.filtered.set(
        this.contacts().filter((c) =>
          `${c.firstName} ${c.lastName}`.toLowerCase().includes(query)
        )
      );
    });
  }

  selectContact(contact: Contact): void {
    this.selectedContact.set(contact);
  }

  submit(): void {
    if (this.form.invalid || !this.selectedContact() || this.sending()) return;

    const contact = this.selectedContact()!;
    const message = this.form.value.message!.trim();
    this.sending.set(true);

    this.api.startConversation(contact.id, message).subscribe({
      next: (conv) => {
        this.dialogRef.close({ conversationId: conv.id });
      },
      error: () => this.sending.set(false),
    });
  }

  roleLabel(role: string): string {
    if (role === 'ROLE_ADMIN') return 'Admin';
    if (role === 'ROLE_CONSEILLER') return 'Conseiller';
    return 'Client';
  }

  roleColor(role: string): string {
    if (role === 'ROLE_ADMIN') return 'admin';
    if (role === 'ROLE_CONSEILLER') return 'conseiller';
    return 'client';
  }

  initials(c: Contact): string {
    return ((c.firstName?.[0] ?? '') + (c.lastName?.[0] ?? '')).toUpperCase() || '?';
  }
}
