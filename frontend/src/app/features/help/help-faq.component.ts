import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { ROLES } from '../../core/auth/constants/auth.constants';
import { AuthService } from '../../core/auth/services/auth.service';
import {
  ADMIN_HELP_FAQ,
  ADMIN_QUICK_LINKS,
  ADVISOR_HELP_FAQ,
  ADVISOR_QUICK_LINKS,
  CLIENT_HELP_FAQ,
  CLIENT_QUICK_LINKS,
  HELP_CONTACT,
} from '../../core/help/constants/help-faq.constants';
import type { HelpFaqAudience } from '../../core/help/models/help-faq.model';
import { HelpFaqSearchService } from '../../core/help/services/help-faq-search.service';
import { countHelpFaqItems, filterHelpFaqCategories } from '../../core/help/utils/help-faq.util';

@Component({
  selector: 'app-help-faq',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
  ],
  templateUrl: './help-faq.component.html',
  styleUrl: './help-faq.component.scss',
})
export class HelpFaqComponent {
  private readonly auth = inject(AuthService);
  readonly helpFaqSearch = inject(HelpFaqSearchService);

  readonly contact = HELP_CONTACT;

  readonly audience = computed<HelpFaqAudience>(() => {
    if (this.auth.hasRole([ROLES.ADMIN])) {
      return 'admin';
    }
    if (this.auth.hasRole([ROLES.CONSEILLER])) {
      return 'advisor';
    }
    return 'client';
  });

  readonly sourceCategories = computed(() => {
    switch (this.audience()) {
      case 'admin':
        return ADMIN_HELP_FAQ;
      case 'advisor':
        return ADVISOR_HELP_FAQ;
      default:
        return CLIENT_HELP_FAQ;
    }
  });

  readonly quickLinks = computed(() => {
    switch (this.audience()) {
      case 'admin':
        return ADMIN_QUICK_LINKS;
      case 'advisor':
        return ADVISOR_QUICK_LINKS;
      default:
        return CLIENT_QUICK_LINKS;
    }
  });

  readonly messagesLink = computed(() => {
    switch (this.audience()) {
      case 'admin':
        return ['/admin/messages'];
      case 'advisor':
        return ['/conseiller/messages'];
      default:
        return ['/messages'];
    }
  });

  readonly filteredCategories = computed(() =>
    filterHelpFaqCategories(this.sourceCategories(), this.helpFaqSearch.query())
  );

  readonly totalQuestions = computed(() => countHelpFaqItems(this.sourceCategories()));

  readonly hasResults = computed(() => this.filteredCategories().length > 0);

  clearSearch(): void {
    this.helpFaqSearch.clear();
  }
}
