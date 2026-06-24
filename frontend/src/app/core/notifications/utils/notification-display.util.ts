import { NotificationDto } from '../models/notification.model';

export type NotificationVisualTone =
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'payment'
  | 'default';

export interface NotificationVisual {
  icon: string;
  tone: NotificationVisualTone;
}

export type NotificationPeriodGroup = 'today' | 'yesterday' | 'this_week' | 'older';

export interface NotificationGroup {
  key: NotificationPeriodGroup;
  label: string;
  items: NotificationDto[];
}

const PERIOD_LABELS: Record<NotificationPeriodGroup, string> = {
  today: "Aujourd'hui",
  yesterday: 'Hier',
  this_week: 'Cette semaine',
  older: 'Plus ancien',
};

const PERIOD_ORDER: NotificationPeriodGroup[] = ['today', 'yesterday', 'this_week', 'older'];

const EVENT_VISUALS: Record<string, NotificationVisual> = {
  APPLICATION_CREATED: { icon: 'note_add', tone: 'info' },
  APPLICATION_SUBMITTED: { icon: 'send', tone: 'info' },
  REVIEW_STARTED: { icon: 'hourglass_top', tone: 'info' },
  DOCUMENT_UPLOADED: { icon: 'upload_file', tone: 'info' },
  ADVISOR_ASSIGNED: { icon: 'support_agent', tone: 'info' },
  DOCUMENT_REJECTED: { icon: 'description_off', tone: 'warning' },
  DOCUMENT_VALIDATED: { icon: 'task_alt', tone: 'success' },
  OFFER_PROPOSED: { icon: 'local_offer', tone: 'info' },
  OFFER_ACCEPTED: { icon: 'thumb_up', tone: 'success' },
  OFFER_REJECTED: { icon: 'thumb_down', tone: 'warning' },
  APPLICATION_APPROVED: { icon: 'check_circle', tone: 'success' },
  APPLICATION_REJECTED: { icon: 'cancel', tone: 'danger' },
  APPLICATION_CANCELLED: { icon: 'block', tone: 'default' },
  FUNDS_RELEASED: { icon: 'account_balance_wallet', tone: 'success' },
  LOAN_CREATED: { icon: 'account_balance', tone: 'success' },
  MANDATE_ACTIVATED: { icon: 'verified_user', tone: 'success' },
  MANDATE_REVOKED: { icon: 'gpp_bad', tone: 'warning' },
  PAYMENT_SUCCEEDED: { icon: 'payments', tone: 'payment' },
  PAYMENT_FAILED: { icon: 'money_off', tone: 'danger' },
  INSTALLMENT_OVERDUE: { icon: 'schedule', tone: 'danger' },
  LOAN_CLOSED: { icon: 'celebration', tone: 'success' },
  LOAN_DEFAULTED: { icon: 'warning', tone: 'danger' },
};

const DEFAULT_VISUAL: NotificationVisual = { icon: 'notifications', tone: 'default' };

export function getNotificationVisual(eventType: string): NotificationVisual {
  return EVENT_VISUALS[eventType] ?? DEFAULT_VISUAL;
}

export function isRecentNotification(iso: string, hours = 24): boolean {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return false;
  }
  return Date.now() - date.getTime() < hours * 60 * 60 * 1000;
}

export function formatRelativeNotificationDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60_000);

  if (diffMin < 1) {
    return "À l'instant";
  }
  if (diffMin < 60) {
    return `Il y a ${diffMin} min`;
  }

  const diffHours = Math.floor(diffMin / 60);
  if (isSameCalendarDay(date, now) && diffHours < 24) {
    return `Il y a ${diffHours} h`;
  }

  const time = formatTime(date);
  if (isYesterday(date, now)) {
    return `Hier à ${time}`;
  }

  const weekStart = startOfWeek(now);
  if (date >= weekStart) {
    return `${formatWeekday(date)} à ${time}`;
  }

  return `${formatDate(date)} à ${time}`;
}

export function groupNotificationsByPeriod(items: NotificationDto[]): NotificationGroup[] {
  const buckets = new Map<NotificationPeriodGroup, NotificationDto[]>();

  for (const item of items) {
    const group = getPeriodGroup(new Date(item.createdAt), new Date());
    const list = buckets.get(group) ?? [];
    list.push(item);
    buckets.set(group, list);
  }

  return PERIOD_ORDER.filter((key) => buckets.has(key)).map((key) => ({
    key,
    label: PERIOD_LABELS[key],
    items: buckets.get(key) ?? [],
  }));
}

function getPeriodGroup(date: Date, now: Date): NotificationPeriodGroup {
  if (Number.isNaN(date.getTime())) {
    return 'older';
  }
  if (isSameCalendarDay(date, now)) {
    return 'today';
  }
  if (isYesterday(date, now)) {
    return 'yesterday';
  }
  if (date >= startOfWeek(now)) {
    return 'this_week';
  }
  return 'older';
}

function isSameCalendarDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function isYesterday(date: Date, now: Date): boolean {
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  return isSameCalendarDay(date, yesterday);
}

function startOfWeek(date: Date): Date {
  const start = new Date(date);
  const day = start.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  start.setDate(start.getDate() + diff);
  start.setHours(0, 0, 0, 0);
  return start;
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function formatDate(date: Date): string {
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function formatWeekday(date: Date): string {
  return date.toLocaleDateString('fr-FR', { weekday: 'short' }).replace('.', '');
}
