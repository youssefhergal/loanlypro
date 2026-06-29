import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentsApiService } from '../../../core/documents/services/documents-api.service';
import { JustificatifGroupDto, JustificatifItemDto } from '../../../core/documents/models/justificatif.model';
import {
  documentCountLabel,
  documentTypeIcon,
  formatUploadedAt,
  groupSubtitle,
  loanStatusChipClass,
  loanStatusLabel,
  validationStatusClass,
  validationStatusLabel,
} from '../../../core/documents/utils/justificatif-display.util';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-justificatifs-tab',
  standalone: true,
  imports: [
    RouterLink,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './justificatifs-tab.component.html',
  styleUrl: './justificatifs-tab.component.scss',
})
export class JustificatifsTabComponent implements OnInit {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly notImplemented = signal(false);
  readonly groups = signal<JustificatifGroupDto[]>([]);
  readonly expandedGroups = signal<Set<number>>(new Set());
  readonly menuContext = signal<{ doc: JustificatifItemDto; group: JustificatifGroupDto } | null>(
    null,
  );

  readonly helpers = {
    groupSubtitle,
    loanStatusLabel,
    loanStatusChipClass,
    documentCountLabel,
    documentTypeIcon,
    formatUploadedAt,
    validationStatusLabel,
    validationStatusClass,
  };

  ngOnInit(): void {
    this.loadJustificatifs();
  }

  loadJustificatifs(): void {
    this.loading.set(true);
    this.notImplemented.set(false);
    this.documentsApi.getJustificatifs().subscribe({
      next: (groups) => {
        this.groups.set(groups);
        if (groups.length > 0) {
          this.expandedGroups.set(new Set([groups[0].loanApplicationId]));
        }
        this.loading.set(false);
      },
      error: (err) => this.handleLoadError(err, 'Impossible de charger vos justificatifs.'),
    });
  }

  isExpanded(groupId: number): boolean {
    return this.expandedGroups().has(groupId);
  }

  toggleGroup(groupId: number): void {
    this.expandedGroups.update((current) => {
      const next = new Set(current);
      if (next.has(groupId)) {
        next.delete(groupId);
      } else {
        next.add(groupId);
      }
      return next;
    });
  }

  dossierLink(group: JustificatifGroupDto): string[] {
    if (group.loanStatus === 'DRAFT') {
      return ['/nouvelle-demande', String(group.loanApplicationId)];
    }
    return ['/mes-demandes', String(group.loanApplicationId)];
  }

  openDocMenu(doc: JustificatifItemDto, group: JustificatifGroupDto): void {
    this.menuContext.set({ doc, group });
  }

  download(doc: JustificatifItemDto): void {
    if (!doc.downloadable) {
      return;
    }
    this.documentsApi.downloadJustificatif(doc.documentId).subscribe({
      next: (blob) => this.triggerDownload(blob, doc.fileName),
      error: (err) => {
        this.snackBar.open(getErrorMessage(err, 'Téléchargement indisponible.'), 'Fermer', {
          duration: 5000,
        });
      },
    });
  }

  private handleLoadError(err: unknown, fallback: string): void {
    if (err instanceof HttpErrorResponse && err.status === 501) {
      this.notImplemented.set(true);
      this.groups.set([]);
    } else {
      this.snackBar.open(getErrorMessage(err, fallback), 'Fermer', { duration: 5000 });
    }
    this.loading.set(false);
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
