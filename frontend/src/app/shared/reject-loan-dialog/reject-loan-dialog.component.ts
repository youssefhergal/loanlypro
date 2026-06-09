import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type RejectLoanDialogResult = string | null | undefined;

@Component({
  selector: 'app-reject-loan-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, FormsModule],
  templateUrl: './reject-loan-dialog.component.html',
  styleUrl: './reject-loan-dialog.component.scss',
})
export class RejectLoanDialogComponent {
  readonly maxLength = 500;
  private readonly dialogRef = inject(
    MatDialogRef<RejectLoanDialogComponent, RejectLoanDialogResult>
  );

  readonly comment = signal('');

  readonly trimmedComment = computed(() => this.comment().trim());

  readonly canConfirm = computed(() => this.trimmedComment().length > 0);

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onConfirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close(this.trimmedComment());
  }
}
