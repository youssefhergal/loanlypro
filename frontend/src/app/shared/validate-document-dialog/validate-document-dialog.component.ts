import { Component, computed, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ValidateDocumentDialogData {
  documentLabel: string;
  fileName?: string | null;
}

export type ValidateDocumentDialogResult = boolean | null | undefined;

@Component({
  selector: 'app-validate-document-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './validate-document-dialog.component.html',
  styleUrl: './validate-document-dialog.component.scss',
})
export class ValidateDocumentDialogComponent {
  readonly data = inject<ValidateDocumentDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(
    MatDialogRef<ValidateDocumentDialogComponent, ValidateDocumentDialogResult>
  );

  readonly documentLine = computed(() => {
    const name = this.data.fileName?.trim();
    if (name) {
      return `${this.data.documentLabel} (${name})`;
    }
    return this.data.documentLabel;
  });

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
