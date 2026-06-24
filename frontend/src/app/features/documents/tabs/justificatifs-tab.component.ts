import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentsApiService } from '../../../core/documents/services/documents-api.service';
import { JustificatifGroupDto } from '../../../core/documents/models/justificatif.model';
import { getErrorMessage } from '../../../core/loans/utils/api-error.util';

@Component({
  selector: 'app-justificatifs-tab',
  standalone: true,
  imports: [MatIconModule, MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './justificatifs-tab.component.html',
  styleUrl: './justificatifs-tab.component.scss',
})
export class JustificatifsTabComponent implements OnInit {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly notImplemented = signal(false);
  readonly groups = signal<JustificatifGroupDto[]>([]);

  ngOnInit(): void {
    this.loadJustificatifs();
  }

  loadJustificatifs(): void {
    this.loading.set(true);
    this.notImplemented.set(false);
    this.documentsApi.getJustificatifs().subscribe({
      next: (groups) => {
        this.groups.set(groups);
        this.loading.set(false);
      },
      error: (err) => this.handleLoadError(err, 'Impossible de charger vos justificatifs.'),
    });
  }

  download(documentId: number): void {
    this.documentsApi.downloadJustificatif(documentId).subscribe({
      next: (blob) => this.triggerDownload(blob, `justificatif-${documentId}.pdf`),
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
