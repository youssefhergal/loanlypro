import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { JustificatifsTabComponent } from './tabs/justificatifs-tab.component';
import { CreditDocumentsTabComponent } from './tabs/credit-documents-tab.component';
import { RepaymentExportsTabComponent } from './tabs/repayment-exports-tab.component';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [
    MatTabsModule,
    JustificatifsTabComponent,
    CreditDocumentsTabComponent,
    RepaymentExportsTabComponent,
  ],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
})
export class DocumentsComponent {}
