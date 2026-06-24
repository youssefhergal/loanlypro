# Notifications — Vue d'ensemble

[← Module notifications](./README.md)

## Objectif

Informer les utilisateurs des événements importants sur leurs dossiers de prêt :

- **E-mail** : vérification d'inscription + changements de statut majeurs + paiements (sans spam sur chaque upload).
- **In-app** : centre de notifications avec badge cloche (client et conseiller).

## Séparation des responsabilités

| Concept | Rôle |
|---------|------|
| `LoanApplicationEvent` | Journal d'audit / timeline du dossier (historique) |
| `Notification` | Alerte UX in-app (lu / non lu) |
| `EmailSender` | Envoi transactionnel (Resend ou logs en dev) |

Le **point d'accroche unique** est `LoanApplicationHistoryService.recordEvent()` qui publie un événement Spring `LoanApplicationEventRecorded`. Le `NotificationDispatcher` réagit **après commit** de la transaction.

## Politique de notification

### E-mail (client uniquement)

Envoyé pour : soumission, analyse, document rejeté, contre-offre, approbation/refus/annulation, mandat, paiements, retard, clôture, défaut.

**Exclu** : `DOCUMENT_UPLOADED`, `DOCUMENT_VALIDATED`, événements internes.

### In-app client

Alertes sur les mêmes événements métier + validation document + assignation conseiller.

### In-app conseiller

Alertes sur : nouvelle demande, document déposé, décision, échec paiement, retard, défaut, assignation.

## Vérification e-mail (auth)

- Déclenchée à l'inscription (`AuthService.register`).
- **Pas** de ligne `Notification` in-app : bannière dans l'espace client si `emailVerified = false`.
- Endpoints publics : `POST /api/auth/verify-email`, `POST /api/auth/resend-verification`.

## Hors périmètre (v1)

- Préférences utilisateur (opt-out par type)
- Notifications push / SMS
- Messagerie temps réel (WebSocket)
