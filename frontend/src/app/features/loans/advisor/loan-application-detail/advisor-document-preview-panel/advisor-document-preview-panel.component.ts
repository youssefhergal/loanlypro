import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoanDetailDocumentRow } from '../../../../../core/loans/utils/loan-detail.util';

@Component({
  selector: 'app-advisor-document-preview-panel',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './advisor-document-preview-panel.component.html',
  styleUrl: './advisor-document-preview-panel.component.scss',
})
export class AdvisorDocumentPreviewPanelComponent {
  private readonly sanitizer = inject(DomSanitizer);

  @Input({ required: true }) open = false;
  @Input() row: LoanDetailDocumentRow | null = null;
  @Input() fileLabel = '';
  @Input() fileMeta = '';
  @Input() set previewUrl(value: string | null) {
    this.rawPreviewUrl = value;
    this.safePreviewUrl = value ? this.sanitizer.bypassSecurityTrustResourceUrl(value) : null;
  }
  @Input() contentType = '';
  @Input() loading = false;
  @Input() zoom = 100;
  @Input() canReviewActions = false;
  @Input() actionLoading = false;
  @Input() hasPrev = false;
  @Input() hasNext = false;
  @Input() pageLabel = 'Page 1 / 1';
  @Input() statusBadgeClass = 'doc-badge';
  @Input() statusLabel = '—';

  @Output() closed = new EventEmitter<void>();
  @Output() zoomIn = new EventEmitter<void>();
  @Output() zoomOut = new EventEmitter<void>();
  @Output() prevDocument = new EventEmitter<void>();
  @Output() nextDocument = new EventEmitter<void>();
  @Output() download = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();
  @Output() reject = new EventEmitter<void>();

  rawPreviewUrl: string | null = null;
  safePreviewUrl: SafeResourceUrl | null = null;

  get isImagePreview(): boolean {
    return this.contentType.startsWith('image/');
  }

  get isPdfPreview(): boolean {
    return this.contentType === 'application/pdf' || this.fileMeta.toLowerCase().includes('.pdf');
  }
}
