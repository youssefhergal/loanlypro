export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  senderFirstName: string;
  senderLastName: string;
  content: string;
  sentAt: string;
  readAt: string | null;
  own: boolean;
  attachmentName?: string | null;
  attachmentContentType?: string | null;
  attachmentDownloadUrl?: string | null;
}
