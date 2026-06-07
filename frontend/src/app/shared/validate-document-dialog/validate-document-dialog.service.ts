import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ValidateDocumentDialogComponent,
  ValidateDocumentDialogData,
  ValidateDocumentDialogResult,
} from './validate-document-dialog.component';

@Injectable({ providedIn: 'root' })
export class ValidateDocumentDialogService {
  private readonly dialog = inject(MatDialog);

  open(data: ValidateDocumentDialogData): Observable<boolean> {
    return this.dialog
      .open(ValidateDocumentDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'dialog',
        panelClass: 'validate-document-dialog-panel',
        data,
      })
      .afterClosed()
      .pipe(map((result: ValidateDocumentDialogResult) => result === true));
  }
}
