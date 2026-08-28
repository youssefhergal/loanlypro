# IA — Cas d'utilisation

[← Module IA](./README.md)

```mermaid
flowchart TB
  subgraph acteurs [Acteurs]
    U[Utilisateur connecté\nClient / Conseiller / Admin]
    CL[Claude API\nAnthropic]
  end
  subgraph ia [Intelligence Artificielle]
    subgraph conseiller [Conseiller IA]
      UC1[Envoyer un message à Alex]
      UC2[Recevoir une réponse financière]
      UC3[Consulter l'historique de chat]
    end
    subgraph formation [Formation sur mesure]
      UC4[Saisir un objectif d'apprentissage]
      UC5[Générer un parcours]
      UC6[Consulter ses parcours]
      UC7[Ouvrir une étape]
      UC8[Générer le contenu d'une étape]
      UC9[Marquer une étape terminée]
      UC10[Reprendre un parcours]
    end
  end
  U --> UC1
  U --> UC3
  U --> UC4
  U --> UC6
  U --> UC10
  UC1 --> UC2
  UC2 -.->|appelle| CL
  UC4 --> UC5
  UC5 -.->|appelle| CL
  UC6 --> UC7
  UC7 --> UC8
  UC8 -.->|appelle| CL
  UC8 --> UC9
  UC10 --> UC7
```

| UC | Fonctionnalité | Statut |
|----|---------------|--------|
| Envoyer un message à Alex | Conseiller IA | ✅ `POST /api/ai/chat` |
| Recevoir une réponse financière | Conseiller IA | ✅ Réponse Claude en temps réel |
| Consulter l'historique de chat | Conseiller IA | ✅ Géré côté frontend (signals) — non persisté |
| Saisir un objectif d'apprentissage | Formation | ✅ Vue `goal-input` |
| Générer un parcours (roadmap) | Formation | ✅ `POST /api/ai/learning/roadmap` |
| Consulter ses parcours | Formation | ✅ `GET /api/ai/learning/sessions` |
| Ouvrir une étape | Formation | ✅ Vue `step-detail` |
| Générer le contenu d'une étape | Formation | ✅ `POST /sessions/{id}/steps/{i}/content` |
| Marquer une étape terminée | Formation | ✅ `PATCH /sessions/{id}/steps/{i}/complete` |
| Reprendre un parcours | Formation | ✅ `resumeSession()` → vue `roadmap` |
