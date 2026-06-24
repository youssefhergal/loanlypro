# Notifications — API backend

[← Module notifications](./README.md)

## Endpoints Auth (e-mail)

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `POST` | `/api/auth/verify-email` | Non | Valide le code reçu (`email` + `token`) |
| `POST` | `/api/auth/resend-verification` | Non | Renvoie un code (204) |

Corps `verify-email` :

```json
{ "email": "user@example.com", "token": "000000" }
```

## Endpoints Notifications in-app

Préfixe : `/api/v1/notifications` — **JWT requis**.

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/me?page=&size=` | Liste paginée (max 50 / page) |
| `GET` | `/unread-count` | `{ "count": 3 }` |
| `PATCH` | `/{id}/read` | Marque une notification comme lue (204) |
| `PATCH` | `/read-all` | Tout marquer comme lu (204) |

Contrôleur : `NotificationController.java`.

## Services principaux

| Service | Rôle |
|---------|------|
| `EmailVerificationService` | Génération code, expiration, envoi e-mail inscription |
| `TransactionalEmailNotificationService` | E-mails métier dossier / paiement |
| `NotificationService` | CRUD in-app (création, lecture, compteur) |
| `NotificationDispatcher` | Orchestration après `recordEvent` |
| `NotificationPolicy` | Matrice événement → e-mail / in-app client / in-app conseiller |
| `NotificationContentFactory` | Titres et messages par `LoanApplicationEventType` |

## Flux dispatch

```
recordEvent() → save LoanApplicationEvent
             → publish LoanApplicationEventRecorded
             → @TransactionalEventListener(AFTER_COMMIT)
             → NotificationDispatcher.dispatch()
                   ├─ e-mail client (si policy)
                   ├─ notification in-app client
                   └─ notification in-app conseiller
```

## Configuration

Voir [README.md](./README.md#configuration-rapide).

| Propriété | Défaut | Description |
|-----------|--------|-------------|
| `app.mail.provider` | `logging` | `logging` = logs console ; `resend` = API Resend |
| `app.mail.from` | `LoanlyFans <noreply@...>` | Expéditeur |
| `app.mail.resend.api-key` | — | Clé API Resend (obligatoire si provider=resend) |
| `app.mail.verification.expiry-minutes` | `30` | Validité du code |
| `app.mail.verification.dev-fixed-code` | — | Code fixe en dev (`000000`) |

## Entité `Notification`

| Champ | Description |
|-------|-------------|
| `recipient` | Utilisateur destinataire |
| `eventType` | `LoanApplicationEventType` source |
| `title`, `message` | Contenu affiché |
| `referenceType` | `LOAN_APPLICATION` |
| `referenceId` | ID du dossier (lien frontend) |
| `readAt` | `null` = non lu |

## Tests unitaires

| Classe | Couverture |
|--------|------------|
| `EmailVerificationServiceTest` | Code, expiration, envoi |
| `NotificationDispatcherTest` | Politique e-mail / in-app |
| `NotificationControllerTest` | Endpoints REST |
