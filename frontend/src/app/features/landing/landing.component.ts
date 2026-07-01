import { Component, AfterViewInit, NgZone, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent implements AfterViewInit {
  private readonly zone = inject(NgZone);

  readonly advantages = [
    {
      icon: 'psychology',
      tag: 'IA Exclusif',
      title: 'Conseiller Financier IA',
      description:
        "Posez toutes vos questions à Alex, votre conseiller IA disponible 24h/24. Il analyse votre profil et vous guide vers les meilleures décisions financières en temps réel.",
    },
    {
      icon: 'school',
      tag: 'Apprentissage',
      title: 'Formation Sur Mesure',
      description:
        "Un parcours d'apprentissage généré par IA, adapté à vos lacunes et vos objectifs. Progressez étape par étape vers la maîtrise de vos finances personnelles.",
    },
    {
      icon: 'eco',
      tag: "Jusqu’à −30 %",
      title: 'Taux Verts Avantageux',
      description:
        "Voiture électrique, rénovation énergétique, panneaux solaires… Profitez de taux préférentiels pour tous vos projets écologiques et contribuez à un avenir durable.",
    },
    {
      icon: 'pause_circle',
      tag: 'Flexibilité',
      title: 'Pause Remboursement',
      description:
        "La vie réserve des surprises. Mettez votre remboursement en pause temporairement selon votre profil et reprenez sereinement quand vous êtes prêt.",
    },
    {
      icon: 'folder_open',
      tag: 'Tout-en-un',
      title: 'Dossier 100 % Numérique',
      description:
        "Déposez, signez et suivez tous vos documents en ligne. Zéro papier, zéro déplacement. Votre dossier avance en temps réel avec des notifications instantanées.",
    },
    {
      icon: 'shield',
      tag: 'Sécurisé',
      title: 'Données Protégées',
      description:
        "Vos données financières sont chiffrées et hébergées en France. Authentification sécurisée et confidentialité totale garanties.",
    },
  ];

  readonly stats = [
    { value: 2500, suffix: '+', label: 'Clients actifs' },
    { value: 98, suffix: '%', label: 'Satisfaction client' },
    { value: 48, suffix: 'h', label: 'Délai de réponse' },
    { value: 30, suffix: '%', label: 'Taux vert réduit' },
  ];

  readonly steps = [
    { num: '01', icon: 'person_add', title: 'Créez votre compte', desc: 'Inscription en 2 minutes, sans engagement.' },
    { num: '02', icon: 'description', title: 'Déposez votre dossier', desc: "L'IA analyse votre demande instantanément." },
    { num: '03', icon: 'trending_up', title: 'Gérez & progressez', desc: 'Suivez, apprenez et optimisez vos finances.' },
  ];

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      this.initScrollAnimations();
    });
  }

  private initScrollAnimations(): void {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry, i) => {
          if (entry.isIntersecting) {
            const el = entry.target as HTMLElement;
            const delay = el.dataset['delay'] ?? '0';
            setTimeout(() => el.classList.add('visible'), +delay);
            observer.unobserve(el);
          }
        });
      },
      { threshold: 0.12 }
    );
    document.querySelectorAll('.aos').forEach((el) => observer.observe(el));

    // Counter animation
    const counterObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            this.animateCounters();
            counterObserver.disconnect();
          }
        });
      },
      { threshold: 0.5 }
    );
    const statsSection = document.querySelector('.stats-section');
    if (statsSection) counterObserver.observe(statsSection);
  }

  private animateCounters(): void {
    document.querySelectorAll<HTMLElement>('.stat-value').forEach((el) => {
      const target = +(el.dataset['target'] ?? 0);
      const suffix = el.dataset['suffix'] ?? '';
      const duration = 1800;
      const start = performance.now();
      const update = (now: number) => {
        const progress = Math.min((now - start) / duration, 1);
        const ease = 1 - Math.pow(1 - progress, 3);
        const current = target < 10 ? (target * ease).toFixed(1) : Math.round(target * ease);
        el.textContent = `${current}${suffix}`;
        if (progress < 1) requestAnimationFrame(update);
      };
      requestAnimationFrame(update);
    });
  }
}
