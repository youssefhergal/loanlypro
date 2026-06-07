import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import {
  SendProposalDialogComponent,
  SendProposalDialogData,
  SendProposalDialogResult,
} from './send-proposal-dialog.component';

@Injectable({ providedIn: 'root' })
export class SendProposalDialogService {
  private readonly dialog = inject(MatDialog);

  open(data: SendProposalDialogData): Observable<SendProposalDialogResult | null | undefined> {
    return this.dialog
      .open(SendProposalDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        autoFocus: 'first-timer',
        panelClass: 'send-proposal-dialog-panel',
        data,
      })
      .afterClosed();
  }
}
