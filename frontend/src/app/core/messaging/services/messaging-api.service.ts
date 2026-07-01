import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Conversation, Contact } from '../models/conversation.model';
import type { Message } from '../models/message.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MessagingApiService {
  private readonly base = `${environment.apiUrl}/messaging`;

  constructor(private readonly http: HttpClient) {}

  getContacts(): Observable<Contact[]> {
    return this.http.get<Contact[]>(`${this.base}/contacts`);
  }

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.base}/conversations`);
  }

  startConversation(targetUserId: number, initialMessage: string): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.base}/conversations`, {
      targetUserId,
      initialMessage,
    });
  }

  getMessages(conversationId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.base}/conversations/${conversationId}/messages`);
  }

  sendMessage(conversationId: number, content: string): Observable<Message> {
    return this.http.post<Message>(`${this.base}/conversations/${conversationId}/messages`, {
      content,
    });
  }

  sendMessageWithAttachment(conversationId: number, content: string, file: File): Observable<Message> {
    const form = new FormData();
    form.append('content', content);
    form.append('file', file);
    return this.http.post<Message>(
      `${this.base}/conversations/${conversationId}/messages/upload`,
      form
    );
  }

  markRead(conversationId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/conversations/${conversationId}/read`, {});
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/unread-count`);
  }
}
