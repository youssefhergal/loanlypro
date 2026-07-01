import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { HelpFaqSearchService } from '../../core/help/services/help-faq-search.service';

@Component({
  selector: 'app-help-faq-search-field',
  standalone: true,
  imports: [MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule],
  template: `
    <mat-form-field
      class="help-faq-search-field"
      appearance="outline"
      subscriptSizing="dynamic"
    >
      <mat-label>Rechercher</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input
        matInput
        type="search"
        [value]="search.query()"
        (input)="onInput($any($event.target).value)"
        placeholder="Ex. documents, mandat…"
        aria-label="Rechercher dans la FAQ"
      />
      @if (search.query()) {
        <button
          mat-icon-button
          matSuffix
          type="button"
          (click)="search.clear()"
          aria-label="Effacer la recherche"
        >
          <mat-icon>close</mat-icon>
        </button>
      }
    </mat-form-field>
  `,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
      }

      .help-faq-search-field {
        width: 100%;
        margin: 0;
        font-size: 13px;
      }

      .help-faq-search-field ::ng-deep .mat-mdc-form-field-subscript-wrapper {
        display: none;
      }

      .help-faq-search-field ::ng-deep .mat-mdc-text-field-wrapper {
        height: 40px;
      }

      .help-faq-search-field ::ng-deep .mat-mdc-form-field-infix {
        min-height: 40px;
        padding-top: 8px !important;
        padding-bottom: 8px !important;
      }
    `,
  ],
})
export class HelpFaqSearchFieldComponent {
  readonly search = inject(HelpFaqSearchService);

  onInput(value: string): void {
    this.search.setQuery(value);
  }
}
