import { CurrencyPipe, DatePipe, NgIf, NgFor } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { LoanApiService } from '../../../../core/loans/services/loan-api.service';
import { LoanResponseDto } from '../../../../core/loans/models/loan-response.model';
import { LoanDocumentResponseDto } from '../../../../core/loans/models/loan-document.model';
import { LoanHistoryEventResponseDto } from '../../../../core/loans/models/loan-history.model';
import { docDisplayName, formatDocSize } from '../../../../core/loans/utils/loan-detail.util';
import { ConfirmDialogService } from '../../../../shared/confirm-dialog/confirm-dialog.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-advisor-loan-application-detail',
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './advisor-loan-application-detail.component.html',
  styleUrl: './advisor-loan-application-detail.component.scss',
})
export class AdvisorLoanApplicationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly loanApi = inject(LoanApiService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly loan = signal<LoanResponseDto | null>(null);
  readonly documents = signal<LoanDocumentResponseDto[]>([]);
  readonly docsLoading = signal(false);
  readonly docsError = signal<string | null>(null);
  readonly actionLoading = signal(false);

  // Historique
  readonly history = signal<LoanHistoryEventResponseDto[]>([]);
  readonly historyLoading = signal(false);
  readonly historyError = signal<string | null>(null);

  // helpers for template
  readonly docSize = (bytes: number) => formatDocSize(bytes);
  readonly docTitle = (doc: LoanDocumentResponseDto) => docDisplayName(doc);

  // UI helpers
  readonly statusIcon = (s: LoanResponseDto['status']) => {
    switch (s) {
      case 'SUBMITTED':
        return 'schedule';
      case 'UNDER_REVIEW':
        return 'find_in_page';
      case 'APPROVED':
        return 'check_circle';
      case 'REJECTED':
        return 'cancel';
      case 'CANCELLED':
        return 'do_not_disturb';
      default:
        return 'info';
    }
  };

  readonly statusClass = (s: LoanResponseDto['status']) => `status-chip status-${s.toLowerCase()}`;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!id || Number.isNaN(id)) {
      this.error.set('Identifiant invalide.');
      return;
    }
    this.fetch(id);
  }

  private fetch(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.loanApi.getById(id).subscribe({
      next: (data) => {
        this.loan.set(data);
        this.loading.set(false);
        this.loadDocuments(id);
        this.loadHistory(id);
      },
      error: (err) => {
        const message = err?.status === 404
          ? 'Dossier introuvable (404).'
          : (err?.error?.message || err?.message || 'Erreur de chargement');
        this.error.set(message);
        this.loading.set(false);
      },
    });
  }

  private loadDocuments(id: number): void {
    this.docsLoading.set(true);
    this.docsError.set(null);
    this.loanApi.getDocuments(id).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.docsLoading.set(false);
      },
      error: (err) => {
        const message = err?.error?.message || err?.message || 'Erreur de chargement des documents';
        this.docsError.set(message);
        this.docsLoading.set(false);
      },
    });
  }

  refreshDocuments(): void {
    const id = this.loan()?.id;
    if (id) {
      this.loadDocuments(id);
    }
  }

  download(doc: LoanDocumentResponseDto): void {
    const loanId = this.loan()?.id;
    if (!loanId) return;
    this.loanApi.downloadDocument(loanId, doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.displayName?.trim() || doc.originalFileName || `document-${doc.id}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        // Optionally we could expose a snack bar; for now set docsError
        this.docsError.set('Échec du téléchargement du document.');
      },
    });
  }

  // Historique
  private loadHistory(id: number): void {
    this.historyLoading.set(true);
    this.historyError.set(null);
    this.loanApi.getHistory(id).subscribe({
      next: (events) => {
        // Tri antichronologique (du plus récent au plus ancien)
        const sorted = [...events].sort((a, b) => {
          const da = a.occurredAt ? new Date(a.occurredAt).getTime() : 0;
          const db = b.occurredAt ? new Date(b.occurredAt).getTime() : 0;
          return db - da;
        });
        this.history.set(sorted);
        this.historyLoading.set(false);
      },
      error: (err) => {
        const message = err?.error?.message || err?.message || "Erreur de chargement de l'historique";
        this.historyError.set(message);
        this.historyLoading.set(false);
      },
    });
  }

  refreshHistory(): void {
    const id = this.loan()?.id;
    if (id) {
      this.loadHistory(id);
    }
  }

  // Actions conseiller
  startReview(): void {
    const l = this.loan();
    if (!l || this.actionLoading()) return;
    if (l.status !== 'SUBMITTED') return;

    this.confirmDialog
      .open({
        title: 'Mettre en analyse ?',
        message:
          `Le dossier « ${l.title} » passera en analyse (UNDER_REVIEW).\n\nContinuer ?`,
        confirmLabel: 'Mettre en analyse',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.actionLoading.set(true);
        this.loanApi.startReview(l.id).subscribe({
          next: (updated) => {
            this.loan.set(updated);
            this.actionLoading.set(false);
            this.snackBar.open('Dossier mis en analyse.', 'OK', { duration: 3000 });
            this.refreshDocuments();
            this.refreshHistory();
          },
          error: (err) => {
            this.actionLoading.set(false);
            const msg = err?.error?.message || err?.message || "Échec de l'action";
            this.snackBar.open(msg, 'Fermer', { duration: 5000 });
          },
        });
      });
  }

  approve(): void {
    const l = this.loan();
    if (!l || this.actionLoading()) return;
    if (l.status !== 'UNDER_REVIEW') return;

    this.confirmDialog
      .open({
        title: 'Approuver ce dossier ?',
        message:
          `Le dossier « ${l.title} » sera approuvé. Cette action déclenche la décision.\n\nConfirmez-vous ?`,
        confirmLabel: 'Approuver',
        confirmColor: 'primary',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.actionLoading.set(true);
        this.loanApi.approve(l.id).subscribe({
          next: (updated) => {
            this.loan.set(updated);
            this.actionLoading.set(false);
            this.snackBar.open('Dossier approuvé.', 'OK', { duration: 3000 });
            this.refreshHistory();
          },
          error: (err) => {
            this.actionLoading.set(false);
            const msg = err?.error?.message || err?.message || "Échec de l'action";
            this.snackBar.open(msg, 'Fermer', { duration: 5000 });
          },
        });
      });
  }

  reject(): void {
    const l = this.loan();
    if (!l || this.actionLoading()) return;
    if (l.status !== 'UNDER_REVIEW') return;

    this.confirmDialog
      .open({
        title: 'Rejeter ce dossier ?',
        message:
          `Le dossier « ${l.title} » sera rejeté. Cette action est irréversible.\n\nConfirmez-vous ?`,
        confirmLabel: 'Rejeter',
        confirmColor: 'warn',
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.actionLoading.set(true);
        this.loanApi.reject(l.id).subscribe({
          next: (updated) => {
            this.loan.set(updated);
            this.actionLoading.set(false);
            this.snackBar.open('Dossier rejeté.', 'OK', { duration: 3000 });
            this.refreshHistory();
          },
          error: (err) => {
            this.actionLoading.set(false);
            const msg = err?.error?.message || err?.message || "Échec de l'action";
            this.snackBar.open(msg, 'Fermer', { duration: 5000 });
          },
        });
      });
  }
}
