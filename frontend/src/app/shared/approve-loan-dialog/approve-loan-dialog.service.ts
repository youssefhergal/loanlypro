import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ApproveLoanDialogComponent,
  ApproveLoanDialogData,
} from './approve-loan-dialog.component';

@Injectable({ providedIn: 'root' })
export class ApproveLoanDialogService {
  private readonly dialog = inject(MatDialog);

  open(data: ApproveLoanDialogData): Observable<boolean> {
    return this.dialog
      .open(ApproveLoanDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'dialog',
        panelClass: 'approve-loan-dialog-panel',
        data,
      })
      .afterClosed()
      .pipe(map((result) => result === true));
  }
}
