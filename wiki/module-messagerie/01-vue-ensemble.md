# Messagerie — Vue d'ensemble

[← Module messagerie](./README.md)

## Objectif

Permettre une messagerie temps réel entre un **client** et son **conseiller**, avec support des pièces jointes (images, PDF, documents).

## Architecture

```
Client Angular                Backend Spring Boot
─────────────────             ──────────────────────────────
HTTP REST        →  GET /conversations, GET /messages
                 →  POST /messages/upload (fichier)
                 ←  JSON

WebSocket STOMP  →  CONNECT /ws
                 →  SUBSCRIBE /user/queue/messages
                 →  SEND /app/chat.send
                 ←  MESSAGE (push temps réel)
```

## Flux d'une conversation

```
1. Client ouvre la messagerie → GET /api/messaging/conversations
2. Sélectionne une conversation → GET /api/messaging/conversations/{id}/messages
3. Envoie un message texte → STOMP SEND /app/chat.send
4. Backend persiste + broadcast → STOMP push sur /user/queue/messages
5. Les deux participants reçoivent le message instantanément
```

## Pièces jointes

```
1. Utilisateur sélectionne un fichier
2. Frontend → POST /api/messaging/conversations/{id}/messages/upload (multipart/form-data)
3. Backend stocke le fichier (local ou GCS selon STORAGE_TYPE)
4. Backend crée le Message avec attachment_url
5. Réponse JSON avec le message complet → affiché dans la bulle
```

Taille maximale : **10 MB** par fichier (`spring.servlet.multipart.max-file-size`).

## Modèle de données

```
Conversation
 ├── id (Long)
 ├── client_id → User
 ├── conseiller_id → User
 ├── created_at
 └── messages[] → Message
       ├── id
       ├── conversation_id
       ├── sender_id → User
       ├── content (TEXT)
       ├── attachment_url (nullable)
       ├── attachment_name (nullable)
       ├── attachment_type (nullable)
       └── sent_at
```

## Sécurité

- Chaque utilisateur ne voit que ses propres conversations (filtré par `sender_id` ou participation).
- Le backend valide que l'expéditeur STOMP correspond au JWT reçu à la connexion WebSocket.
- Les fichiers sont servis depuis le stockage sécurisé, pas exposés publiquement.

## Hors périmètre (v1)

- Notifications push mobile
- Indicateur "en train d'écrire"
- Messages lus/non lus par message (uniquement compteur global)
- Groupe de conversation (multi-participants)
