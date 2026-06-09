import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface RejectCounterOfferDialogData {
  amount: number;
  durationMonths: number;
  rate: number | null;
  monthlyPayment: number;
  offerMessage?: string | null;
}

/** null = annulé, string = confirmé (commentaire optionnel, peut être vide) */
export type RejectCounterOfferDialogResult = string | null;

@Component({
  selector: 'app-reject-counter-offer-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, FormsModule, CurrencyPipe, DecimalPipe],
  templateUrl: './reject-counter-offer-dialog.component.html',
  styleUrl: './reject-counter-offer-dialog.component.scss',
})
export class RejectCounterOfferDialogComponent {
  readonly maxLength = 500;
  readonly data = inject<RejectCounterOfferDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(
    MatDialogRef<RejectCounterOfferDialogComponent, RejectCounterOfferDialogResult>
  );

  readonly comment = signal('');

  readonly hasOfferMessage = computed(() => Boolean(this.data.offerMessage?.trim()));

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onConfirm(): void {
    this.dialogRef.close(this.comment().trim());
  }
}
