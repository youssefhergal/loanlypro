# Messagerie — API Backend

[← Module messagerie](./README.md)

## Endpoints REST

### Conversations

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| `GET` | `/api/messaging/conversations` | Liste des conversations de l'utilisateur connecté | JWT |
| `GET` | `/api/messaging/conversations/{id}/messages` | Messages d'une conversation (paginés) | JWT |

### Messages

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| `POST` | `/api/messaging/conversations/{id}/messages/upload` | Envoyer un fichier joint | JWT |

### WebSocket STOMP

| Opération | Destination | Description |
|-----------|-------------|-------------|
| Connexion | `ws://localhost:8080/ws` | Handshake SockJS + STOMP |
| Envoi | `/app/chat.send` | Envoyer un message texte |
| Réception | `/user/queue/messages` | Recevoir les messages en temps réel |

#### Payload envoi (STOMP)

```json
{
  "conversationId": 1,
  "content": "Bonjour, j'ai une question sur mon dossier."
}
```

#### Payload réception (STOMP push)

```json
{
  "id": 42,
  "conversationId": 1,
  "senderId": 3,
  "senderName": "Jean Dupont",
  "content": "Bonjour, j'ai une question sur mon dossier.",
  "attachmentUrl": null,
  "attachmentName": null,
  "attachmentType": null,
  "sentAt": "2026-07-01T18:30:00Z"
}
```

#### Upload fichier (REST multipart)

```
POST /api/messaging/conversations/1/messages/upload
Content-Type: multipart/form-data
Authorization: Bearer <jwt>

file=<binary>
```

Réponse : même format que le payload de message STOMP.

## Configuration WebSocket

```java
// WebSocketConfig.java
registry.addEndpoint("/ws").withSockJS();
config.enableSimpleBroker("/user", "/topic");
config.setApplicationDestinationPrefixes("/app");
config.setUserDestinationPrefix("/user");
```

## Stockage fichiers

Contrôlé par `STORAGE_TYPE` :

| Valeur | Description |
|--------|-------------|
| `local` | Dossier `uploads/` local (dev) |
| `gcs` | Google Cloud Storage bucket `GCS_BUCKET` (prod) |

## Erreurs communes

| Code | Cause |
|------|-------|
| `403` | Tentative d'accès à une conversation dont on ne fait pas partie |
| `413` | Fichier > 10 MB (`spring.servlet.multipart.max-request-size`) |
| `400` | Fichier manquant dans le multipart |
