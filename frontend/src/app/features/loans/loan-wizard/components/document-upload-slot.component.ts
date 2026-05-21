import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoanDocumentType } from '../../../../core/loans/models/loan.enums';
import { LoanDocumentResponseDto } from '../../../../core/loans/models/loan-document.model';
import {
  LOAN_ALLOWED_MIME_TYPES,
  LOAN_FILE_MAX_BYTES,
} from '../../../../core/loans/constants/loan.constants';

@Component({
  selector: 'app-document-upload-slot',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="upload-slot" [class.required]="required">
      <div class="slot-header">
        <h4>{{ label }} @if (required) { <span class="req">*</span> }</h4>
        <label class="upload-btn">
          <input
            type="file"
            accept=".pdf,.jpg,.jpeg,.png"
            (change)="onFileSelected($event)"
            [disabled]="uploading()"
          />
          <span mat-stroked-button>Ajouter un fichier</span>
        </label>
      </div>
      @if (errorMessage()) {
        <p class="error">{{ errorMessage() }}</p>
      }
      @if (uploading()) {
        <mat-progress-bar mode="indeterminate" />
      }
      <ul class="file-list">
        @for (doc of files; track doc.id) {
          <li>
            <span>{{ doc.originalFileName }} ({{ formatSize(doc.fileSizeBytes) }})</span>
            <button type="button" mat-icon-button (click)="remove.emit(doc.id)" aria-label="Supprimer">
              <mat-icon>delete</mat-icon>
            </button>
          </li>
        }
      </ul>
    </div>
  `,
  styles: `
    .upload-slot {
      border: 1px dashed rgba(0,0,0,0.2);
      border-radius: 8px;
      padding: 1rem;
      margin-bottom: 1rem;
    }
    .slot-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      flex-wrap: wrap;
    }
    h4 { margin: 0; font-size: 1rem; }
    .req { color: #c62828; }
    .upload-btn input { display: none; }
    .upload-btn span {
      display: inline-block;
      padding: 0.35rem 1rem;
      border: 1px solid rgba(0,0,0,0.2);
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.875rem;
    }
    .error { color: #c62828; font-size: 0.875rem; margin: 0.5rem 0 0; }
    .file-list { list-style: none; padding: 0; margin: 0.75rem 0 0; }
    .file-list li {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.35rem 0;
      border-top: 1px solid rgba(0,0,0,0.06);
    }
  `,
})
export class DocumentUploadSlotComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) documentType!: LoanDocumentType;
  @Input() required = false;
  @Input() files: LoanDocumentResponseDto[] = [];
  @Output() upload = new EventEmitter<File>();
  @Output() remove = new EventEmitter<number>();

  readonly uploading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    this.errorMessage.set(null);
    if (file.size > LOAN_FILE_MAX_BYTES) {
      this.errorMessage.set('Le fichier dépasse 10 Mo.');
      return;
    }
    if (!LOAN_ALLOWED_MIME_TYPES.includes(file.type)) {
      this.errorMessage.set('Formats acceptés : PDF, JPG, PNG.');
      return;
    }
    this.uploading.set(true);
    this.upload.emit(file);
    this.uploading.set(false);
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }
}
