import { Component, computed, effect, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { HeroCarouselSlide } from '../../../../core/dashboard/utils/client-dashboard-display.util';

const AUTO_PLAY_INTERVAL_MS = 2000;

@Component({
  selector: 'app-dashboard-hero-carousel',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './dashboard-hero-carousel.component.html',
  styleUrl: './dashboard-hero-carousel.component.scss',
})
export class DashboardHeroCarouselComponent {
  readonly slides = input.required<HeroCarouselSlide[]>();
  readonly activeIndex = input(0);

  readonly indexChange = output<number>();

  private autoPlayTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect((onCleanup) => {
      this.clearAutoPlay();
      if (this.slides().length <= 1) {
        return;
      }
      this.autoPlayTimer = setInterval(() => this.nextSlide(), AUTO_PLAY_INTERVAL_MS);
      onCleanup(() => this.clearAutoPlay());
    });
  }

  readonly activeSlide = computed(() => {
    const items = this.slides();
    const index = this.activeIndex();
    return items[index] ?? items[0] ?? null;
  });

  readonly showControls = computed(() => this.slides().length > 1);

  prevSlide(): void {
    const total = this.slides().length;
    if (total <= 1) {
      return;
    }
    const next = (this.activeIndex() - 1 + total) % total;
    this.indexChange.emit(next);
  }

  nextSlide(): void {
    const total = this.slides().length;
    if (total <= 1) {
      return;
    }
    const next = (this.activeIndex() + 1) % total;
    this.indexChange.emit(next);
  }

  goToSlide(index: number): void {
    this.indexChange.emit(index);
  }

  private clearAutoPlay(): void {
    if (this.autoPlayTimer != null) {
      clearInterval(this.autoPlayTimer);
      this.autoPlayTimer = null;
    }
  }
}
