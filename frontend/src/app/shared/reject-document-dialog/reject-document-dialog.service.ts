import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import {
  RejectDocumentDialogComponent,
  RejectDocumentDialogData,
  RejectDocumentDialogResult,
} from './reject-document-dialog.component';

@Injectable({ providedIn: 'root' })
export class RejectDocumentDialogService {
  private readonly dialog = inject(MatDialog);

  open(data: RejectDocumentDialogData): Observable<RejectDocumentDialogResult> {
    return this.dialog
      .open(RejectDocumentDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'first-timer',
        panelClass: 'reject-document-dialog-panel',
        data,
      })
      .afterClosed();
  }
}
