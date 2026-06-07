import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

export interface AcceptCounterOfferDialogData {
  amount: number;
  durationMonths: number;
  rate: number | null;
  monthlyPayment: number;
  offerMessage?: string | null;
}

@Component({
  selector: 'app-accept-counter-offer-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    CurrencyPipe,
    DecimalPipe,
  ],
  templateUrl: './accept-counter-offer-dialog.component.html',
  styleUrl: './accept-counter-offer-dialog.component.scss',
})
export class AcceptCounterOfferDialogComponent {
  readonly data = inject<AcceptCounterOfferDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AcceptCounterOfferDialogComponent, boolean>);

  readonly termsConfirmed = signal(false);

  readonly canConfirm = computed(() => this.termsConfirmed());

  readonly hasOfferMessage = computed(() => Boolean(this.data.offerMessage?.trim()));

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close(true);
  }
}
