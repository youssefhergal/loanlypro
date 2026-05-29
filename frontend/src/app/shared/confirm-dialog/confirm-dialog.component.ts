import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmColor?: 'primary' | 'warn' | 'accent';
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="confirm-dialog__message">{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="onCancel()">
        {{ data.cancelLabel ?? 'Annuler' }}
      </button>
      <button
        mat-flat-button
        type="button"
        [color]="data.confirmColor ?? 'primary'"
        (click)="onConfirm()"
      >
        {{ data.confirmLabel ?? 'Confirmer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    :host {
      display: block;
    }

    .confirm-dialog__message {
      margin: 0;
      font-size: 0.9375rem;
      line-height: 1.5;
      color: var(--color-text-muted, #6b7c72);
      white-space: pre-line;
    }

    :host ::ng-deep mat-dialog-actions {
      padding: 0.5rem 1.5rem 1.25rem;
      gap: 0.5rem;
    }

    :host ::ng-deep mat-dialog-actions .mdc-button {
      border-radius: var(--radius-sm, 8px) !important;
      min-height: 38px;
      font-weight: 600;
      font-size: 0.8125rem;
      padding: 0 14px !important;
    }

    :host ::ng-deep .mat-mdc-dialog-container {
      border-radius: var(--radius-md, 12px) !important;
    }
  `,
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent, boolean>);

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
