import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatBadgeModule } from '@angular/material/badge';
import { MessagingApiService } from '../../core/messaging/services/messaging-api.service';
import { StompMessagingService } from '../../core/messaging/services/stomp-messaging.service';
import { AuthService } from '../../core/auth/services/auth.service';
import { NewConversationDialogComponent } from './new-conversation-dialog/new-conversation-dialog.component';
import type { Conversation } from '../../core/messaging/models/conversation.model';
import type { Message } from '../../core/messaging/models/message.model';

const MAX_FILE_SIZE = 10 * 1024 * 1024;

@Component({
  selector: 'app-messaging',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatBadgeModule,
  ],
  templateUrl: './messaging.component.html',
  styleUrl: './messaging.component.scss',
})
export class MessagingComponent implements OnInit {
  @ViewChild('messagesEnd') messagesEnd!: ElementRef;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  private readonly api = inject(MessagingApiService);
  private readonly stomp = inject(StompMessagingService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly conversations = signal<Conversation[]>([]);
  readonly messages = signal<Message[]>([]);
  readonly selectedConv = signal<Conversation | null>(null);
  readonly loading = signal(false);
  readonly sending = signal(false);
  readonly searchQuery = signal('');
  readonly attachedFile = signal<File | null>(null);
  readonly fileError = signal<string | null>(null);

  readonly messageCtrl = new FormControl('');

  ngOnInit(): void {
    this.loadConversations();

    const token = this.auth.getToken();
    if (token) {
      this.stomp.connect(token);
    }

    this.stomp.incomingMessage$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((msg) => this.onIncomingMessage(msg));
  }

  loadConversations(): void {
    this.api.getConversations().subscribe((convs) => {
      this.conversations.set(convs);
    });
  }

  selectConversation(conv: Conversation): void {
    this.selectedConv.set(conv);
    this.loading.set(true);
    this.api.getMessages(conv.id).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.loading.set(false);
        this.scrollToBottom();
        this.markRead(conv.id);
        this.conversations.update((list) =>
          list.map((c) => (c.id === conv.id ? { ...c, unreadCount: 0 } : c))
        );
      },
      error: () => this.loading.set(false),
    });
  }

  send(): void {
    const conv = this.selectedConv();
    const content = this.messageCtrl.value?.trim() ?? '';
    const file = this.attachedFile();

    if (!conv || this.sending()) return;
    if (!content && !file) return;

    this.sending.set(true);

    const onSuccess = (msg: Message) => {
      this.messageCtrl.reset();
      this.clearAttachment();
      this.addMessageIfNew(msg);
      this.scrollToBottom();
      this.sending.set(false);
      this.updateConvPreview(conv.id, content || `📎 ${file?.name}`);
    };
    const onError = () => this.sending.set(false);

    if (file) {
      this.api.sendMessageWithAttachment(conv.id, content, file).subscribe({ next: onSuccess, error: onError });
    } else {
      this.api.sendMessage(conv.id, content).subscribe({ next: onSuccess, error: onError });
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.fileError.set(null);
    if (!file) return;
    if (file.size > MAX_FILE_SIZE) {
      this.fileError.set('Le fichier dépasse 10 Mo.');
      input.value = '';
      return;
    }
    this.attachedFile.set(file);
    input.value = '';
  }

  clearAttachment(): void {
    this.attachedFile.set(null);
    this.fileError.set(null);
  }

  isImage(contentType: string | null | undefined): boolean {
    return !!contentType?.startsWith('image/');
  }

  openNewConversationDialog(): void {
    const ref = this.dialog.open(NewConversationDialogComponent, {
      width: '480px',
      panelClass: 'messaging-dialog-panel',
    });
    ref.afterClosed().subscribe((result: { conversationId: number } | undefined) => {
      if (result) {
        this.loadConversations();
        setTimeout(() => {
          const conv = this.conversations().find((c) => c.id === result.conversationId);
          if (conv) this.selectConversation(conv);
        }, 300);
      }
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  filteredConversations(): Conversation[] {
    const q = this.searchQuery().toLowerCase();
    if (!q) return this.conversations();
    return this.conversations().filter((c) =>
      `${c.otherUserFirstName} ${c.otherUserLastName}`.toLowerCase().includes(q)
    );
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

  initials(firstName: string, lastName: string): string {
    return ((firstName?.[0] ?? '') + (lastName?.[0] ?? '')).toUpperCase() || '?';
  }

  totalUnread(): number {
    return this.conversations().reduce((acc, c) => acc + c.unreadCount, 0);
  }

  private addMessageIfNew(msg: Message): void {
    if (this.messages().some(m => m.id === msg.id)) return;
    this.messages.update((list) => [...list, msg]);
  }

  private onIncomingMessage(msg: Message): void {
    const current = this.selectedConv();
    if (current && msg.conversationId === current.id) {
      this.addMessageIfNew(msg);
      this.scrollToBottom();
      this.markRead(current.id);
    } else {
      this.conversations.update((list) =>
        list.map((c) =>
          c.id === msg.conversationId
            ? { ...c, unreadCount: c.unreadCount + 1, lastMessagePreview: msg.content }
            : c
        )
      );
    }
    this.api.getConversations().subscribe((convs) => this.conversations.set(convs));
  }

  private markRead(convId: number): void {
    this.api.markRead(convId).subscribe();
  }

  private updateConvPreview(convId: number, content: string): void {
    this.conversations.update((list) =>
      list.map((c) =>
        c.id === convId
          ? { ...c, lastMessagePreview: content, lastMessageAt: new Date().toISOString() }
          : c
      )
    );
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
    }, 50);
  }
}
