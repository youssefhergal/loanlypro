import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface SendProposalDialogData {
  requestedAmount: number;
  requestedDurationMonths: number;
  requestedMonthlyPayment: number;
  approvedAmount: number;
  approvedDurationMonths: number;
  approvedRate: number;
  approvedMonthlyPayment: number;
  clientMessage?: string | null;
}

export interface SendProposalDialogResult {
  clientMessage?: string;
}

@Component({
  selector: 'app-send-proposal-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    CurrencyPipe,
    DecimalPipe,
  ],
  templateUrl: './send-proposal-dialog.component.html',
  styleUrl: './send-proposal-dialog.component.scss',
})
export class SendProposalDialogComponent {
  readonly maxMessageLength = 500;
  readonly data = inject<SendProposalDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(
    MatDialogRef<SendProposalDialogComponent, SendProposalDialogResult | null>
  );

  readonly clientMessage = signal(this.data.clientMessage?.trim() ?? '');

  readonly trimmedMessage = computed(() => this.clientMessage().trim());

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onConfirm(): void {
    this.dialogRef.close({
      clientMessage: this.trimmedMessage() || undefined,
    });
  }
}
