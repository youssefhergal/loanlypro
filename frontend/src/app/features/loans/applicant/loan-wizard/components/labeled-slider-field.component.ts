import { Component, Input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSliderModule } from '@angular/material/slider';

@Component({
  selector: 'app-labeled-slider-field',
  standalone: true,
  imports: [ReactiveFormsModule, MatSliderModule],
  template: `
    <div class="labeled-slider" [formGroup]="form">
      <div class="labeled-slider__row">
        <label class="labeled-slider__label" [for]="inputId">{{ label }}</label>
        <div class="labeled-slider__input-wrap">
          <input
            [id]="inputId"
            class="labeled-slider__input"
            type="number"
            [min]="min"
            [max]="max"
            [step]="inputStep"
            [value]="displayValue"
            (input)="onInput($event)"
            (blur)="onBlur()"
          />
          @if (unit) {
            <span class="labeled-slider__unit">{{ unit }}</span>
          }
        </div>
      </div>
      <mat-slider
        class="labeled-slider__slider"
        [min]="min"
        [max]="max"
        [step]="sliderStep"
        [discrete]="discrete"
        [displayWith]="formatSliderLabel"
      >
        <input
          matSliderThumb
          [value]="displayValue"
          (input)="onSliderInput($event)"
        />
      </mat-slider>
      <div class="labeled-slider__range">
        <span>{{ formatRangeLabel(min) }}</span>
        <span>{{ formatRangeLabel(max) }}</span>
      </div>
    </div>
  `,
  styles: `
    .labeled-slider {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
    }

    .labeled-slider__row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }

    .labeled-slider__label {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--color-text, #1a2e22);
      flex: 1;
    }

    .labeled-slider__input-wrap {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 0.35rem;
      flex-shrink: 0;
      width: 10rem;
      min-height: 2.25rem;
      box-sizing: border-box;
      background: var(--color-surface, #fff);
      border: 1px solid var(--color-border, #d8e8de);
      border-radius: var(--radius-sm, 8px);
      padding: 0.35rem 0.65rem;
    }

    .labeled-slider__input {
      flex: 1;
      min-width: 0;
      width: 100%;
      border: none;
      outline: none;
      font-size: 0.9375rem;
      font-weight: 600;
      text-align: right;
      font-family: inherit;
      color: var(--color-text, #1a2e22);
      background: transparent;
      line-height: 1.3;
    }

    .labeled-slider__input::-webkit-outer-spin-button,
    .labeled-slider__input::-webkit-inner-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }

    .labeled-slider__unit {
      flex-shrink: 0;
      min-width: 2rem;
      text-align: left;
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--color-text-muted, #6b7c72);
      white-space: nowrap;
    }

    .labeled-slider__slider {
      width: 100%;
      margin: 0.2rem 0 0.1rem;
    }

    .labeled-slider ::ng-deep .mat-mdc-slider {
      width: 100%;
      height: 36px;
      padding: 8px 0;
      box-sizing: border-box;
      --mdc-slider-active-track-height: 3px;
      --mdc-slider-inactive-track-height: 3px;
      --mdc-slider-handle-height: 16px;
      --mdc-slider-handle-width: 16px;
    }

    .labeled-slider ::ng-deep .mdc-slider {
      height: 36px;
      margin: 0;
    }

    .labeled-slider ::ng-deep .mdc-slider__track {
      top: 50%;
      transform: translateY(-50%);
    }

    .labeled-slider ::ng-deep .mdc-slider__thumb {
      top: calc(50% - 8px);
      height: 16px;
      width: 16px;
    }

    .labeled-slider ::ng-deep .mdc-slider__thumb-knob {
      width: 14px;
      height: 14px;
      border-width: 2px;
    }

    .labeled-slider ::ng-deep .mdc-slider__value-indicator-text {
      font-size: 0.6875rem;
      font-weight: 700;
      white-space: nowrap;
      line-height: 1.2;
    }

    .labeled-slider__range {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--color-text-muted, #6b7c72);
      padding: 0.05rem 0.1rem 0;
    }
  `,
})
export class LabeledSliderFieldComponent {
  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) controlName!: string;
  @Input() label = '';
  @Input() min = 0;
  @Input() max = 100;
  @Input() sliderStep = 1;
  @Input() inputStep = 1;
  @Input() unit = '';
  @Input() discrete = true;
  /** Format compact pour les bulles du slider (ex. 10k, 84m). */
  @Input() formatValue: (v: number) => string = (v) => String(v);
  /** Format complet pour min / max (ex. 10 000 €, 240 mois). Par défaut = formatValue. */
  @Input() formatRangeValue?: (v: number) => string;

  private static idCounter = 0;
  readonly inputId = `labeled-slider-${++LabeledSliderFieldComponent.idCounter}`;

  get displayValue(): number {
    const raw = this.form.get(this.controlName)?.value;
    const n = Number(raw);
    return Number.isFinite(n) ? this.clamp(n) : this.min;
  }

  formatSliderLabel = (value: number): string => this.formatValue(value);

  formatRangeLabel = (value: number): string =>
    (this.formatRangeValue ?? this.formatValue)(value);

  onInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const parsed = Number(el.value);
    if (!Number.isFinite(parsed)) return;
    this.setValue(this.clamp(parsed));
  }

  onBlur(): void {
    this.setValue(this.displayValue);
  }

  onSliderInput(event: Event): void {
    const el = event.target as HTMLInputElement & { value?: number };
    const raw = el.value ?? el.valueAsNumber;
    const parsed = Number(raw);
    if (!Number.isFinite(parsed)) return;
    this.setValue(this.clamp(parsed));
  }

  private setValue(value: number): void {
    this.form.get(this.controlName)?.setValue(value);
    this.form.get(this.controlName)?.markAsDirty();
  }

  private clamp(value: number): number {
    return Math.min(this.max, Math.max(this.min, Math.round(value)));
  }
}
