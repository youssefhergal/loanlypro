export type HelpFaqAudience = 'client' | 'advisor' | 'admin';

export interface HelpFaqItem {
  id: string;
  question: string;
  answer: string;
}

export interface HelpFaqCategory {
  id: string;
  label: string;
  icon: string;
  items: HelpFaqItem[];
}

export interface HelpQuickLink {
  label: string;
  icon: string;
  routerLink: string[];
}

export interface HelpContactInfo {
  supportEmail: string;
  dpoEmail: string;
  supportHours: string;
  messagesLink: string[];
}
