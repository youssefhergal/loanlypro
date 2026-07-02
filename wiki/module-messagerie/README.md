# Module Messagerie

[← Wiki](../README.md)

## Sommaire

| # | Document | Contenu |
|---|----------|---------|
| 1 | [01-vue-ensemble.md](./01-vue-ensemble.md) | Architecture WebSocket, flux temps réel, pièces jointes |
| 2 | [02-cas-utilisation.md](./02-cas-utilisation.md) | Cas d'utilisation — client, conseiller, acteurs |
| 3 | [03-sequences.md](./03-sequences.md) | Diagrammes de séquence — connexion WS, envoi, upload, historique |
| 4 | [05-api-backend.md](./05-api-backend.md) | Endpoints REST + STOMP, entités, configuration |
| 5 | [05-frontend.md](./05-frontend.md) | Composants Angular, service STOMP, upload fichier |

## Code source

| Couche | Chemin |
|--------|--------|
| Entités | `backend/.../entity/Conversation.java`, `Message.java`, `MessageAttachment.java` |
| Repository | `backend/.../repository/ConversationRepository.java`, `MessageRepository.java` |
| Service | `backend/.../service/MessagingService.java` |
| WebSocket config | `backend/.../config/WebSocketConfig.java` |
| API REST | `backend/.../web/controller/MessagingController.java` |
| Stockage fichiers | `backend/.../service/StorageService.java` |
| Frontend service | `frontend/src/app/core/messaging/services/` |
| Frontend composant | `frontend/src/app/features/messaging/` |

## Comptes de test

| Email | Rôle | Notes |
|-------|------|-------|
| `client@test.com` | Client | Peut ouvrir une conversation avec son conseiller |
| `conseiller@test.com` | Conseiller | Voit toutes les conversations de ses clients |
