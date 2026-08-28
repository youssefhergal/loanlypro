# Messagerie — Frontend

[← Module messagerie](./README.md)

## Structure

```
frontend/src/app/
├── core/messaging/
│   ├── models/
│   │   └── message.model.ts          # Conversation, Message, interfaces
│   └── services/
│       ├── messaging.service.ts      # Appels REST (conversations, messages)
│       └── stomp-messaging.service.ts # WebSocket STOMP (connexion, send, subscribe)
└── features/messaging/
    └── messaging.component.ts/.html/.scss  # Vue principale
```

## Service STOMP

`StompMessagingService` gère le cycle de vie WebSocket :

```typescript
// Connexion
connect(token: string): void
  → new Client({ brokerURL: 'ws://…/ws', connectHeaders: { Authorization: 'Bearer …' } })

// Envoi d'un message
sendMessage(conversationId: number, content: string): void
  → client.publish({ destination: '/app/chat.send', body: JSON.stringify({…}) })

// Abonnement aux messages entrants
subscribeToMessages(callback: (msg: Message) => void): void
  → client.subscribe('/user/queue/messages', frame => callback(JSON.parse(frame.body)))

// Déconnexion (OnDestroy)
disconnect(): void
```

**Précaution race condition** : l'envoi est mis en attente si le client n'est pas encore connecté (`client.connected`), puis exécuté dans le callback `onConnect`.

## Upload de fichier

```typescript
// Dans MessagingService
uploadFile(conversationId: number, file: File): Observable<Message> {
  const formData = new FormData();
  formData.append('file', file);
  return this.http.post<Message>(
    `${base}/conversations/${conversationId}/messages/upload`,
    formData
  );
}
```

Le composant affiche le message uploadé directement dans la liste sans attendre le push STOMP (optimistic UI).

## Route

```typescript
// app.routes.ts — shell client
{ path: 'messages', loadComponent: () => import('./features/messaging/messaging.component') }
```

## Rendu markdown dans les bulles

Les réponses de l'IA dans le chat sont converties de Markdown → HTML via `mdToHtml()` (parser inline, sans dépendance externe) puis injectées via `[innerHTML]`.

Les messages utilisateur sont affichés en texte brut (pas de rendu markdown).
