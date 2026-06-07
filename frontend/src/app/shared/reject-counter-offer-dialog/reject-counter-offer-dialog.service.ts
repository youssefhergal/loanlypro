import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import {
  RejectCounterOfferDialogComponent,
  RejectCounterOfferDialogData,
  RejectCounterOfferDialogResult,
} from './reject-counter-offer-dialog.component';

@Injectable({ providedIn: 'root' })
export class RejectCounterOfferDialogService {
  private readonly dialog = inject(MatDialog);

  open(data: RejectCounterOfferDialogData): Observable<RejectCounterOfferDialogResult> {
    return this.dialog
      .open(RejectCounterOfferDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'first-timer',
        panelClass: 'reject-counter-offer-dialog-panel',
        data,
      })
      .afterClosed();
  }
}
