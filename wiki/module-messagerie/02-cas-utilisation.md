# Messagerie — Cas d'utilisation

[← Module messagerie](./README.md)

```mermaid
flowchart TB
  subgraph acteurs [Acteurs]
    C[Client]
    CO[Conseiller]
  end
  subgraph msg [Messagerie]
    UC1[Voir ses conversations]
    UC2[Ouvrir une conversation]
    UC3[Envoyer un message texte]
    UC4[Envoyer une pièce jointe]
    UC5[Recevoir un message en temps réel]
    UC6[Télécharger une pièce jointe]
  end
  C --> UC1
  C --> UC2
  C --> UC3
  C --> UC4
  C --> UC5
  C --> UC6
  CO --> UC1
  CO --> UC2
  CO --> UC3
  CO --> UC4
  CO --> UC5
  CO --> UC6
  UC3 -.->|requiert| UC2
  UC4 -.->|requiert| UC2
  UC5 -.->|déclenché par| UC3
  UC5 -.->|déclenché par| UC4
```

| UC | Acteurs | Statut |
|----|---------|--------|
| Voir ses conversations | Client, Conseiller | ✅ `GET /api/messaging/conversations` |
| Ouvrir une conversation | Client, Conseiller | ✅ `GET /api/messaging/conversations/{id}/messages` |
| Envoyer un message texte | Client, Conseiller | ✅ STOMP `SEND /app/chat.send` |
| Envoyer une pièce jointe | Client, Conseiller | ✅ `POST /messages/upload` (multipart) |
| Recevoir en temps réel | Client, Conseiller | ✅ STOMP push `/user/queue/messages` |
| Télécharger une pièce jointe | Client, Conseiller | ✅ URL dans `attachment_url` |
