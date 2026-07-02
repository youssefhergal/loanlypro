import { Component, HostListener, computed, effect, signal, untracked } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { LoanPurpose } from '../../core/loans/models/loan.enums';
import {
  LOAN_AMOUNT_MAX,
  LOAN_AMOUNT_MIN,
  LOAN_DURATION_MAX,
  LOAN_DURATION_MIN,
} from '../../core/loans/constants/loan.constants';
import { calculateLoanSimulation } from '../../core/loans/utils/loan-calculator';
import { computeIndicativeRatePercent } from '../../core/loans/utils/loan-interest-rate.util';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

interface LandingFeature {
  icon: string;
  title: string;
  description: string;
  badge?: string;
  highlight?: boolean;
}

interface SimulatorPurposeOption {
  value: LoanPurpose;
  label: string;
  icon: string;
}

interface LandingStep {
  icon: string;
  title: string;
  description: string;
}

interface LandingFaqItem {
  id: string;
  question: string;
  answer: string;
}

interface LearningStep {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    RouterLink,
    AppLogoComponent,
    CurrencyPipe,
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSliderModule,
    MatSelectModule,
    MatFormFieldModule,
  ],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent {
  readonly currentYear = new Date().getFullYear();

  readonly amountMin = LOAN_AMOUNT_MIN;
  readonly amountMax = 75_000;
  readonly durationMin = LOAN_DURATION_MIN;
  readonly durationMax = Math.min(LOAN_DURATION_MAX, 84);
  readonly durationStep = 12;

  readonly amount = signal(15_000);
  readonly durationMonths = signal(48);
  readonly purpose = signal<LoanPurpose>('VEHICLE');
  readonly mobileNavOpen = signal(false);
  readonly navbarScrolled = signal(false);
  readonly expandedFaqId = signal<string | null>('faq-1');
  readonly resultAnimating = signal(false);

  /** Visuel hero (mockup dashboard) — asset dans public/images/landing */
  readonly heroImageSrc = '/images/landing/image_hero_landing.png';
  readonly greenCreditImageSrc = '/images/landing/vert%20credit.png';
  readonly testimonialAvatarSrc = '/images/landing/Youssef%20Hergal%20Image.png';
  readonly ctaBackgroundImageSrc = '/images/landing/cta%20images.png';

  readonly heroTrustItems = [
    { icon: 'person', line1: 'Parcours 100 %', line2: 'en ligne' },
    { icon: 'schedule', line1: 'Suivi', line2: 'en temps réel' },
    { icon: 'energy_savings_leaf', line1: 'Crédit vert', line2: 'avantageux' },
  ];

  readonly navLinks = [
    { label: 'Fonctionnalités', anchor: 'fonctionnalites' },
    { label: 'Comment ça marche', anchor: 'parcours' },
    { label: 'Crédit vert', anchor: 'credit-vert' },
    { label: 'IA & conseil', anchor: 'ia' },
    { label: 'FAQ', anchor: 'faq' },
  ];

  readonly simulatorPurposes: SimulatorPurposeOption[] = [
    { value: 'PERSONAL', label: 'Projet personnel', icon: 'person' },
    { value: 'VEHICLE', label: 'Véhicule', icon: 'directions_car' },
    { value: 'HOME_IMPROVEMENT', label: 'Travaux / aménagement', icon: 'home_repair_service' },
    { value: 'GREEN', label: 'Crédit vert / éco', icon: 'energy_savings_leaf' },
  ];

  readonly features: LandingFeature[] = [
    {
      icon: 'description',
      title: 'Demande guidée',
      description: 'Wizard étape par étape, brouillon sauvegardé à tout moment.',
    },
    {
      icon: 'trending_up',
      title: 'Taux transparent',
      description: 'Calcul indicatif selon votre profil et le type de projet.',
    },
    {
      icon: 'energy_savings_leaf',
      title: 'Crédit vert',
      description: 'Réduction de taux sur les projets durables et éco-responsables.',
    },
    {
      icon: 'sync',
      title: 'Suivi live',
      description: 'Statuts clairs, historique et notifications en temps réel.',
    },
    {
      icon: 'chat',
      title: 'Messagerie conseiller',
      description: 'Échanges sécurisés avec votre conseiller dédié.',
    },
    {
      icon: 'auto_awesome',
      title: 'Conseiller IA',
      description: 'Réponses personnalisées 24/7 sur vos questions crédit.',
      badge: 'Nouveau',
      highlight: true,
    },
  ];

  readonly steps: LandingStep[] = [
    {
      icon: 'account_circle',
      title: 'Créez votre compte',
      description: 'Inscription rapide et vérification e-mail.',
    },
    {
      icon: 'edit_note',
      title: 'Complétez votre demande',
      description: 'Projet, situation, justificatifs : on vous guide.',
    },
    {
      icon: 'support_agent',
      title: 'Étude & échange',
      description: 'Un conseiller instruit votre dossier et reste à vos côtés.',
    },
    {
      icon: 'account_balance',
      title: 'Prêt & remboursement',
      description: 'Offre, mandat SEPA et échéancier clair.',
    },
  ];

  readonly trustPillars = [
    { icon: 'lock', label: 'Chiffrement & authentification sécurisée' },
    { icon: 'shield', label: 'Conformité RGPD' },
    { icon: 'mark_email_read', label: 'Vérification e-mail à l\'inscription' },
    { icon: 'visibility', label: 'Transparence sur le statut de votre dossier' },
  ];

  readonly faqItems: LandingFaqItem[] = [
    {
      id: 'faq-1',
      question: 'Combien de temps pour une réponse ?',
      answer:
        'Après soumission complète de votre dossier, un conseiller vous recontacte généralement sous 48 h ouvrées. Vous suivez l\'avancement en direct depuis votre espace.',
    },
    {
      id: 'faq-2',
      question: 'Puis-je enregistrer un brouillon ?',
      answer:
        'Oui. Le wizard enregistre votre progression : vous pouvez quitter et reprendre votre demande à tout moment avant l\'envoi final.',
    },
    {
      id: 'faq-3',
      question: 'Quels documents sont demandés ?',
      answer:
        'Selon votre projet : pièce d\'identité, justificatif de domicile, relevés de compte et justificatifs de revenus. La liste exacte s\'adapte à votre situation.',
    },
    {
      id: 'faq-4',
      question: 'Comment contacter un conseiller ?',
      answer:
        'Via la messagerie intégrée une fois connecté, ou par e-mail à conseil@loanlypro.fr. L\'assistant IA répond aussi à vos questions courantes 24 h/24.',
    },
  ];

  readonly learningSteps: LearningStep[] = [
    { label: 'Budget', icon: 'savings' },
    { label: 'Taux', icon: 'percent' },
    { label: 'Assurance emprunteur', icon: 'health_and_safety' },
  ];

  readonly ratePercent = computed(() =>
    computeIndicativeRatePercent(this.amount(), this.durationMonths(), this.purpose())
  );

  readonly simulation = computed(() =>
    calculateLoanSimulation(this.amount(), this.durationMonths(), this.purpose())
  );

  readonly showExcellentRate = computed(() => this.ratePercent() <= 4);

  readonly selectedPurposeLabel = computed(
    () => this.simulatorPurposes.find((p) => p.value === this.purpose())?.label ?? ''
  );

  readonly selectedPurposeIcon = computed(
    () => this.simulatorPurposes.find((p) => p.value === this.purpose())?.icon ?? 'help_outline'
  );

  readonly formattedAmount = computed(() =>
    new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(
      this.amount()
    )
  );

  constructor() {
    effect(() => {
      this.amount();
      this.durationMonths();
      this.purpose();
      untracked(() => {
        this.resultAnimating.set(true);
        window.setTimeout(() => this.resultAnimating.set(false), 450);
      });
    });
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.navbarScrolled.set(window.scrollY > 8);
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  scrollToAnchor(anchor: string): void {
    this.closeMobileNav();
    const el = document.getElementById(anchor);
    el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  toggleFaq(id: string): void {
    this.expandedFaqId.update((current) => (current === id ? null : id));
  }

  onAmountInput(value: string): void {
    const parsed = Number(value.replace(/\s/g, ''));
    if (!Number.isFinite(parsed)) return;
    this.amount.set(this.clampAmount(parsed));
  }

  formatSliderLabel(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  formatDurationLabel(value: number): string {
    return `${value} mois`;
  }

  private clampAmount(value: number): number {
    return Math.min(this.amountMax, Math.max(this.amountMin, value));
  }
}
