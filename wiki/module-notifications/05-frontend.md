# Notifications — Frontend

[← Module notifications](./README.md)

## Routes

| Shell | Route | Composant |
|-------|-------|-----------|
| Client | `/notifications` | `NotificationsComponent` |
| Conseiller | `/conseiller/notifications` | `NotificationsComponent` (partagé) |
| Public | `/verify-email?email=` | `VerifyEmailComponent` |

Fichier routes : `frontend/src/app/app.routes.ts`.

## Services

| Service | Fichier | Rôle |
|---------|---------|------|
| `NotificationApiService` | `core/notifications/services/notification-api.service.ts` | Appels REST |
| `NotificationUnreadService` | `core/notifications/services/notification-unread.service.ts` | Signal `count` + refresh |
| `AuthService.verifyEmail` | `core/auth/services/auth.service.ts` | Vérification inscription |

## UX

### Centre de notifications

- Liste des alertes (titre, message, date)
- « Tout marquer comme lu »
- Clic → marque lu + navigation vers le dossier (`/mes-demandes/:id` ou `/conseiller/dossiers/:id`)

### Badge cloche

- `ClientShellComponent` et `AdvisorShellComponent` appellent `notificationUnread.refresh()` à l'init et à chaque navigation
- Badge Material sur l'icône topbar + compteur sidebar si `count > 0`

### Vérification e-mail

1. Inscription → redirect `/verify-email?email=...`
2. Saisie du code (dev : `000000`)
3. Bouton « Renvoyer le code »
4. Succès → redirect `/login`

### Bannière client

Si `user.emailVerified === false` dans l'espace client : bannière bleue avec lien vers `/verify-email`.

## Modèles TypeScript

- `NotificationDto`, `UnreadNotificationCountDto` — `core/notifications/models/notification.model.ts`
- `User.emailVerified` — `core/auth/models/user.model.ts`

## Parcours de test manuel

1. Créer un compte → page verify-email → code `000000` → connexion
2. Se connecter en client → soumettre un dossier → vérifier notification in-app + badge
3. Se connecter en conseiller → vérifier notification « nouvelle demande » ou « document déposé »
