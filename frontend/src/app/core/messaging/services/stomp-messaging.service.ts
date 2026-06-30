import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import { Subject, Subscription } from 'rxjs';
import SockJS from 'sockjs-client';
import type { Message } from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class StompMessagingService implements OnDestroy {
  private readonly stomp = new RxStomp();
  readonly incomingMessage$ = new Subject<Message>();
  private watchSubscription: Subscription | null = null;

  connect(token: string): void {
    // Guard against double subscription — connected() returns false while still
    // connecting, so the previous check caused a race condition where watch()
    // was called twice, resulting in duplicate messages.
    if (this.watchSubscription) return;

    const config: RxStompConfig = {
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
    };

    this.stomp.configure(config);
    this.stomp.activate();

    this.watchSubscription = this.stomp.watch('/user/queue/messages').subscribe((frame) => {
      try {
        const msg: Message = JSON.parse(frame.body);
        this.incomingMessage$.next(msg);
      } catch {
        // malformed frame — ignore
      }
    });
  }

  disconnect(): void {
    this.watchSubscription?.unsubscribe();
    this.watchSubscription = null;
    this.stomp.deactivate();
  }

  get connected(): boolean {
    return this.stomp.connected();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
