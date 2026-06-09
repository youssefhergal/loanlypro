import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import {
  RejectLoanDialogComponent,
  RejectLoanDialogResult,
} from './reject-loan-dialog.component';

@Injectable({ providedIn: 'root' })
export class RejectLoanDialogService {
  private readonly dialog = inject(MatDialog);

  open(): Observable<RejectLoanDialogResult> {
    return this.dialog
      .open(RejectLoanDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'first-timer',
        panelClass: 'reject-loan-dialog-panel',
      })
      .afterClosed();
  }
}
