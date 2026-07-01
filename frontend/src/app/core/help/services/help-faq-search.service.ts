import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class HelpFaqSearchService {
  readonly query = signal('');

  setQuery(value: string): void {
    this.query.set(value);
  }

  clear(): void {
    this.query.set('');
  }
}
