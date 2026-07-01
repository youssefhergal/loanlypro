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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AiAdvisorService } from '../../core/ai-advisor/services/ai-advisor.service';
import type { AiMessage } from '../../core/ai-advisor/models/ai-chat.model';

function mdToHtml(md: string): string {
  let s = md
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```[\w]*\n?([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`\n]+)`/g, '<code>$1</code>')
    .replace(/^#{4}\s+(.+)$/gm, '<h4>$1</h4>')
    .replace(/^#{3}\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^#{2}\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#{1}\s+(.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^>\s?(.+)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^---+$/gm, '<hr>');

  s = s.replace(/^(\s*[-*+]\s.+)(\n\s*[-*+]\s.+)*/gm, (block) => {
    const items = block.replace(/^\s*[-*+]\s(.+)$/gm, '<li>$1</li>');
    return `<ul>${items}</ul>`;
  });
  s = s.replace(/^(\s*\d+\.\s.+)(\n\s*\d+\.\s.+)*/gm, (block) => {
    const items = block.replace(/^\s*\d+\.\s(.+)$/gm, '<li>$1</li>');
    return `<ol>${items}</ol>`;
  });

  s = s.split(/\n{2,}/).map((para) => {
    const t = para.trim();
    if (!t || /^<(h[1-6]|ul|ol|pre|blockquote|hr)/.test(t)) return t;
    return `<p>${t.replace(/\n/g, '<br>')}</p>`;
  }).join('\n');

  return s;
}

const INITIAL_MESSAGE: AiMessage = {
  role: 'assistant',
  content:
    "Bonjour ! Je suis Alex, votre conseiller financier IA.\n\n" +
    "Je suis là pour vous aider à prendre de meilleures décisions financières, " +
    "adaptées à votre situation personnelle.\n\n" +
    "Quel projet ou quelle question souhaitez-vous évaluer aujourd'hui ?",
};

const SUGGESTIONS = [
  "Je veux acheter une voiture",
  "Est-ce le bon moment pour investir ?",
  "Comment optimiser mon budget mensuel ?",
  "Je veux faire un crédit immobilier",
];

@Component({
  selector: 'app-ai-advisor',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './ai-advisor.component.html',
  styleUrl: './ai-advisor.component.scss',
})
export class AiAdvisorComponent implements OnInit {
  @ViewChild('messagesEnd') messagesEnd!: ElementRef;
  @ViewChild('inputRef') inputRef!: ElementRef<HTMLTextAreaElement>;

  private readonly api = inject(AiAdvisorService);
  private readonly destroyRef = inject(DestroyRef);

  readonly messages = signal<AiMessage[]>([INITIAL_MESSAGE]);
  readonly isTyping = signal(false);
  readonly suggestions = SUGGESTIONS;
  readonly messageCtrl = new FormControl('');

  showSuggestions(): boolean {
    return this.messages().length === 1 && !this.isTyping();
  }

  ngOnInit(): void {}

  send(content?: string): void {
    const text = (content ?? this.messageCtrl.value ?? '').trim();
    if (!text || this.isTyping()) return;

    this.messageCtrl.reset();
    const userMsg: AiMessage = { role: 'user', content: text };
    this.messages.update((list) => [...list, userMsg]);
    this.scrollToBottom();

    this.isTyping.set(true);
    this.messageCtrl.disable();

    this.api
      .chat(this.messages())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.messages.update((list) => [
            ...list,
            { role: 'assistant', content: res.content },
          ]);
          this.isTyping.set(false);
          this.messageCtrl.enable();
          this.scrollToBottom();
          this.focusInput();
        },
        error: () => {
          this.messages.update((list) => [
            ...list,
            {
              role: 'assistant',
              content:
                "Une erreur est survenue. Veuillez vérifier votre connexion et réessayer.",
            },
          ]);
          this.isTyping.set(false);
          this.messageCtrl.enable();
          this.scrollToBottom();
        },
      });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  reset(): void {
    this.messages.set([INITIAL_MESSAGE]);
    this.messageCtrl.reset();
    this.messageCtrl.enable();
    this.isTyping.set(false);
    this.focusInput();
  }

  formatContent(content: string): string {
    return mdToHtml(content);
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
    }, 50);
  }

  private focusInput(): void {
    setTimeout(() => this.inputRef?.nativeElement?.focus(), 100);
  }
}
