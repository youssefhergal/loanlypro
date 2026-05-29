import { DatePipe } from '@angular/common';
import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoanDocumentType } from '../../../../../core/loans/models/loan.enums';
import { LoanDocumentResponseDto } from '../../../../../core/loans/models/loan-document.model';
import {
  LOAN_ALLOWED_MIME_TYPES,
  LOAN_FILE_MAX_BYTES,
  OTHER_DOCUMENT_LABEL_MAX_LENGTH,
} from '../../../../../core/loans/constants/loan.constants';

export interface DocumentSlotUploadState {
  fileName: string;
  fileSize: number;
  mimeType: string;
  progress: number;
  displayName?: string;
}

export interface DocumentUploadPayload {
  file: File;
  displayName?: string;
}

@Component({
  selector: 'app-document-upload-slot',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    DatePipe,
  ],
  templateUrl: './document-upload-slot.component.html',
  styleUrl: './document-upload-slot.component.scss',
})
export class DocumentUploadSlotComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) documentType!: LoanDocumentType;
  @Input() required = false;
  @Input() files: LoanDocumentResponseDto[] = [];
  @Input() uploadState: DocumentSlotUploadState | null = null;
  @Input() maxFiles = 1;
  @Input() requireDisplayName = false;

  @Output() upload = new EventEmitter<DocumentUploadPayload>();
  @Output() remove = new EventEmitter<number>();
  @Output() download = new EventEmitter<number>();
  @Output() cancelUpload = new EventEmitter<void>();

  readonly dragOver = signal(false);
  readonly localError = signal<string | null>(null);
  readonly pendingFile = signal<File | null>(null);
  pendingDisplayName = '';

  readonly labelMaxLength = OTHER_DOCUMENT_LABEL_MAX_LENGTH;

  get isUploading(): boolean {
    return this.uploadState != null;
  }

  get isPending(): boolean {
    return this.required && this.files.length === 0 && !this.isUploading && !this.pendingFile();
  }

  get canAddMore(): boolean {
    return this.files.length < this.maxFiles;
  }

  get showDropZone(): boolean {
    return (
      !this.isUploading &&
      !this.pendingFile() &&
      this.canAddMore
    );
  }

  get slotsHint(): string | null {
    if (this.documentType !== 'OTHER') return null;
    return `${this.files.length} / ${this.maxFiles} document(s) — nom obligatoire pour chaque fichier`;
  }

  @HostListener('dragover', ['$event'])
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.showDropZone) {
      this.dragOver.set(true);
    }
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
    if (!this.showDropZone) return;
    const file = event.dataTransfer?.files?.[0];
    if (file) this.queueFile(file);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) this.queueFile(file);
  }

  confirmPendingUpload(): void {
    const file = this.pendingFile();
    if (!file) return;

    if (this.requireDisplayName) {
      const name = this.pendingDisplayName.trim();
      if (!name) {
        this.localError.set('Indiquez un nom pour ce document.');
        return;
      }
      if (name.length > this.labelMaxLength) {
        this.localError.set(`Le nom ne doit pas dépasser ${this.labelMaxLength} caractères.`);
        return;
      }
      this.upload.emit({ file, displayName: name });
    } else {
      this.upload.emit({ file });
    }

    this.clearPending();
  }

  cancelPending(): void {
    this.clearPending();
  }

  docTitle(doc: LoanDocumentResponseDto): string {
    return doc.displayName?.trim() || doc.originalFileName;
  }

  private queueFile(file: File): void {
    this.localError.set(null);
    if (file.size > LOAN_FILE_MAX_BYTES) {
      this.localError.set('Le fichier dépasse 10 Mo.');
      return;
    }
    if (!LOAN_ALLOWED_MIME_TYPES.includes(file.type)) {
      this.localError.set('Formats acceptés : PDF, JPG, PNG.');
      return;
    }
    this.pendingFile.set(file);
    this.pendingDisplayName = '';
  }

  private clearPending(): void {
    this.pendingFile.set(null);
    this.pendingDisplayName = '';
    this.localError.set(null);
  }

  fileIcon(doc: LoanDocumentResponseDto): string {
    if (doc.contentType === 'application/pdf') return 'picture_as_pdf';
    if (doc.contentType.startsWith('image/')) return 'image';
    return 'insert_drive_file';
  }

  uploadFileIcon(): string {
    const mime = this.uploadState?.mimeType ?? '';
    if (mime === 'application/pdf') return 'picture_as_pdf';
    if (mime.startsWith('image/')) return 'image';
    return 'insert_drive_file';
  }

  formatType(contentType: string): string {
    if (contentType === 'application/pdf') return 'PDF';
    if (contentType === 'image/jpeg') return 'JPG';
    if (contentType === 'image/png') return 'PNG';
    return contentType.split('/').pop()?.toUpperCase() ?? 'Fichier';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }
}
