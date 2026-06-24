export interface NotificationDto {
  id: number;
  eventType: string;
  title: string;
  message: string;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface UnreadNotificationCountDto {
  count: number;
}
