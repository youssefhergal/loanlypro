import type { HelpFaqCategory } from '../models/help-faq.model';

export function filterHelpFaqCategories(
  categories: HelpFaqCategory[],
  query: string
): HelpFaqCategory[] {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return categories;
  }

  return categories
    .map((category) => ({
      ...category,
      items: category.items.filter(
        (item) =>
          item.question.toLowerCase().includes(normalized) ||
          item.answer.toLowerCase().includes(normalized) ||
          category.label.toLowerCase().includes(normalized)
      ),
    }))
    .filter((category) => category.items.length > 0);
}

export function countHelpFaqItems(categories: HelpFaqCategory[]): number {
  return categories.reduce((total, category) => total + category.items.length, 0);
}

export function isHelpFaqPath(path: string): boolean {
  return path === '/aide' || path === '/conseiller/aide' || path === '/admin/aide';
}
