import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { JustificatifsTabComponent } from './tabs/justificatifs-tab.component';
import { CreditDocumentsTabComponent } from './tabs/credit-documents-tab.component';
import { RepaymentExportsTabComponent } from './tabs/repayment-exports-tab.component';
import { CreditDocumentPreviewService } from '../../core/documents/services/credit-document-preview.service';
import { AdvisorDocumentPreviewPanelComponent } from '../loans/advisor/loan-application-detail/advisor-document-preview-panel/advisor-document-preview-panel.component';

interface DocumentsAsideTip {
  title: string;
  icon: string;
  paragraphs: string[];
}

const ASIDE_TIPS: DocumentsAsideTip[] = [
  {
    title: 'Bon à savoir',
    icon: 'lightbulb',
    paragraphs: [
      'Les justificatifs listés ici sont ceux déposés lors de vos demandes de prêt.',
      'Leur statut (En attente, Validé, Refusé) reflète l’instruction par votre conseiller.',
      'Pour compléter un dossier, ouvrez-le depuis le lien en bas de chaque liste.',
    ],
  },
  {
    title: 'Bon à savoir',
    icon: 'description',
    paragraphs: [
      'Les documents crédit (récap, offre, contrat, mandat SEPA) sont générés par LoanlyPro.',
      'Leur disponibilité dépend de l’avancement de votre dossier ou de l’activation du mandat.',
      'Les PDF sont téléchargeables dès qu’ils apparaissent comme disponibles.',
    ],
  },
  {
    title: 'Bon à savoir',
    icon: 'picture_as_pdf',
    paragraphs: [
      'L’échéancier et le relevé de prélèvements sont exportés en PDF à la demande.',
      'Sélectionnez le prêt concerné, puis téléchargez le document souhaité.',
      'Ces exports reprennent les données affichées dans Paiements / Échéancier.',
    ],
  },
];

@Component({
  selector: 'app-documents',
  standalone: true,
  providers: [CreditDocumentPreviewService],
  imports: [
    RouterLink,
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    JustificatifsTabComponent,
    CreditDocumentsTabComponent,
    RepaymentExportsTabComponent,
    AdvisorDocumentPreviewPanelComponent,
  ],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
})
export class DocumentsComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  readonly preview = inject(CreditDocumentPreviewService);

  readonly activeTabIndex = signal(0);
  readonly initialRepaymentLoanId = signal<number | null>(null);

  readonly asideTip = computed(() => ASIDE_TIPS[this.activeTabIndex()] ?? ASIDE_TIPS[0]);

  readonly previewPageLabel = computed(() => {
    const index = this.preview.index();
    const total = this.preview.previewableDocuments().length;
    if (index < 0 || total === 0) {
      return 'Document';
    }
    return `Document ${index + 1} / ${total}`;
  });

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      const tab = params.get('tab');
      if (tab === 'repayment' || tab === 'exports') {
        this.activeTabIndex.set(2);
      } else if (tab === 'credit') {
        this.activeTabIndex.set(1);
      } else if (tab === 'justificatifs') {
        this.activeTabIndex.set(0);
      }

      const loanIdParam = params.get('loanId');
      if (loanIdParam) {
        const loanId = Number(loanIdParam);
        if (!Number.isNaN(loanId)) {
          this.initialRepaymentLoanId.set(loanId);
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.preview.destroy();
  }

  onTabChange(index: number): void {
    this.activeTabIndex.set(index);
    if (this.preview.open()) {
      this.preview.close();
    }
  }
}
