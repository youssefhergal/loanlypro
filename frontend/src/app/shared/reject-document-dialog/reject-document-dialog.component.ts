import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface RejectDocumentDialogData {
  documentLabel: string;
  fileName?: string | null;
}

export type RejectDocumentDialogResult = string | null | undefined;

@Component({
  selector: 'app-reject-document-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, FormsModule],
  templateUrl: './reject-document-dialog.component.html',
  styleUrl: './reject-document-dialog.component.scss',
})
export class RejectDocumentDialogComponent {
  readonly maxLength = 500;
  readonly data = inject<RejectDocumentDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RejectDocumentDialogComponent, RejectDocumentDialogResult>);

  readonly comment = signal('');

  readonly trimmedComment = computed(() => this.comment().trim());

  readonly canConfirm = computed(() => this.trimmedComment().length > 0);

  readonly documentLine = computed(() => {
    const name = this.data.fileName?.trim();
    if (name) {
      return `${this.data.documentLabel} (${name})`;
    }
    return this.data.documentLabel;
  });

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onConfirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close(this.trimmedComment());
  }
}
