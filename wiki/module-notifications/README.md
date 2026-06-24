# Module Notifications & E-mails

[← Wiki](../README.md)

## Sommaire

| # | Document | Contenu |
|---|----------|---------|
| 1 | [01-vue-ensemble.md](./01-vue-ensemble.md) | Périmètre, stratégie e-mail vs in-app |
| 2 | [05-api-backend.md](./05-api-backend.md) | Endpoints REST, services, configuration |
| 3 | [05-frontend.md](./05-frontend.md) | Routes, composants, badge cloche |

## Code source

| Couche | Chemin |
|--------|--------|
| Entité | `backend/.../entity/Notification.java` |
| E-mail | `backend/.../notification/EmailSender.java`, `ResendEmailSender.java`, `LoggingEmailSender.java` |
| Dispatch | `backend/.../notification/NotificationDispatcher.java`, `NotificationPolicy.java` |
| Vérification e-mail | `backend/.../service/EmailVerificationService.java` |
| API in-app | `backend/.../web/controller/NotificationController.java` |
| Frontend | `frontend/src/app/features/notifications/`, `frontend/src/app/core/notifications/` |

## Comptes de test (dev)

| Email | Mot de passe | Notes |
|-------|--------------|-------|
| `client@test.com` | `password` | E-mail déjà vérifié (seed) |
| `conseiller@test.com` | `password` | E-mail déjà vérifié (seed) |

En dev, le code de vérification est fixe : **`000000`** (`application-dev.yml`).

## Configuration rapide

```yaml
app.mail.provider: logging          # logging | resend
app.mail.from: LoanlyFans <noreply@loanlyfans.fr>
app.mail.resend.api-key: ${RESEND_API_KEY:}
app.mail.verification.expiry-minutes: 30
app.mail.verification.dev-fixed-code: "000000"   # dev uniquement
```

Pour activer Resend en prod : `app.mail.provider=resend` + variable d'environnement `RESEND_API_KEY`.

## Données démo (`dev,demo`)

Au démarrage avec le profil **`demo`**, `DemoNotificationSeeder` crée des notifications in-app :

| Compte | Non lues (approx.) | Exemples |
|--------|-------------------|----------|
| `client@test.com` | 5 | soumission, approbation, mandat, échec prélèvement |
| `conseiller@test.com` | 3 | nouveau dossier, document déposé, échec prélèvement |

Liées au dossier **`LF-DEMO-0001`** (après `DemoRepaymentSeeder`). Idempotent : skip si le client a déjà des notifications.
