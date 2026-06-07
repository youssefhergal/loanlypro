import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AcceptCounterOfferDialogComponent,
  AcceptCounterOfferDialogData,
} from './accept-counter-offer-dialog.component';

@Injectable({ providedIn: 'root' })
export class AcceptCounterOfferDialogService {
  private readonly dialog = inject(MatDialog);

  open(data: AcceptCounterOfferDialogData): Observable<boolean> {
    return this.dialog
      .open(AcceptCounterOfferDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'dialog',
        panelClass: 'accept-counter-offer-dialog-panel',
        data,
      })
      .afterClosed()
      .pipe(map((result) => result === true));
  }
}
