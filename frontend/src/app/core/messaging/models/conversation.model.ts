export interface Conversation {
  id: number;
  otherUserId: number;
  otherUserFirstName: string;
  otherUserLastName: string;
  otherUserRole: 'ROLE_CLIENT' | 'ROLE_CONSEILLER' | 'ROLE_ADMIN';
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
}

export interface Contact {
  id: number;
  firstName: string;
  lastName: string;
  role: 'ROLE_CLIENT' | 'ROLE_CONSEILLER' | 'ROLE_ADMIN';
}
