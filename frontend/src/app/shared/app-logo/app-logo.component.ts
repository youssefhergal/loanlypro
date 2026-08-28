import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LOANLY_PRO_BRAND_NAME,
  LOANLY_PRO_LOGO_FULL_SRC,
  LOANLY_PRO_LOGO_ICON_SRC,
} from '../../core/branding/app-branding.constants';

@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './app-logo.component.html',
  styleUrl: './app-logo.component.scss',
})
export class AppLogoComponent {
  readonly brandName = LOANLY_PRO_BRAND_NAME;

  readonly routerLink = input<string | any[]>('/');
  readonly linkClass = input('logo');
  /** true = logo avec texte ; false = icône seule */
  readonly showText = input(true);
  readonly iconSize = input(32);
  readonly fullLogoHeight = input(88);

  readonly logoClick = output<void>();

  readonly usesFullLogo = computed(() => this.showText());
  readonly activeLogoSrc = computed(() =>
    this.showText() ? LOANLY_PRO_LOGO_FULL_SRC : LOANLY_PRO_LOGO_ICON_SRC,
  );

  onClick(): void {
    this.logoClick.emit();
  }
}
