import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

export interface ApproveLoanDialogData {
  amount: number;
  durationMonths: number;
  rate: number;
  monthlyPayment: number;
}

@Component({
  selector: 'app-approve-loan-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    CurrencyPipe,
    DecimalPipe,
  ],
  templateUrl: './approve-loan-dialog.component.html',
  styleUrl: './approve-loan-dialog.component.scss',
})
export class ApproveLoanDialogComponent {
  readonly data = inject<ApproveLoanDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ApproveLoanDialogComponent, boolean>);

  readonly documentsConfirmed = signal(false);

  readonly canConfirm = computed(() => this.documentsConfirmed());

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close(true);
  }
}
