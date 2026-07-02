# IA — Vue d'ensemble

[← Module IA](./README.md)

## Objectif

Intégrer **Claude (Anthropic)** pour offrir deux services à valeur ajoutée :

| Service | Description |
|---------|-------------|
| **Conseiller IA (Alex)** | Chat conversationnel — conseils financiers personnalisés en temps réel |
| **Formation sur mesure** | Génération d'un parcours pédagogique IA + contenu par étape, persisté en BDD |

## Architecture commune

```
Frontend (Angular)
    │
    │  POST /api/ai/chat           ← historique messages JSON
    │  POST /api/ai/learning/*     ← objectif / session / étape
    │  Authorization: Bearer <jwt>
    ▼
Backend (Spring Boot)
    │
    │  RestClient → POST https://api.anthropic.com/v1/messages
    │  x-api-key: ${ANTHROPIC_API_KEY}
    │  anthropic-version: 2023-06-01
    ▼
Claude API (Anthropic)
```

**La clé API ne transite jamais côté frontend.** Elle est lue exclusivement depuis la variable d'environnement `ANTHROPIC_API_KEY` au démarrage du backend.

## Modèle utilisé

```yaml
app.ai.model: ${AI_MODEL:claude-haiku-4-5-20251001}
```

Configurable par variable d'environnement `AI_MODEL`. Par défaut : **Claude Haiku** (rapide et économique). Pour une meilleure qualité, utiliser `claude-sonnet-5`.

## Conseiller IA — Fonctionnement

- Chaque requête envoie **tout l'historique** de la conversation à Claude (contexte complet).
- Le system prompt positionne Claude comme un conseiller financier francophone nommé "Alex".
- Pas de persistance en BDD : l'historique est géré côté frontend (signal Angular).
- Prompt system : ton professionnel, exemples chiffrés, réponses adaptées au profil.

## Formation sur mesure — Fonctionnement

```
1. Utilisateur saisit un objectif (ex: "Gérer mon budget mensuel")
2. Backend → Claude : génère un roadmap JSON de 5–7 étapes
3. Roadmap sauvegardé en BDD (LearningSession)
4. Utilisateur clique sur une étape → Claude génère le contenu pédagogique
5. L'étape complétée est marquée en BDD (completedSteps)
6. Progression persistée → reprise possible depuis n'importe quel appareil
```

## Sécurité de la clé API

| Environnement | Mécanisme |
|--------------|-----------|
| Local dev | Fichier `.env` à la racine (dans `.gitignore`) |
| Docker local | `docker-compose.yml` lit `${ANTHROPIC_API_KEY}` depuis `.env` |
| CI/CD GitLab | Variable `ANTHROPIC_API_KEY` (masked + protected) → injectée dans Cloud Run |
| Cloud Run | Secret Manager ou variable d'environnement Cloud Run |

> ⚠️ Ne jamais mettre la clé dans `application*.yml`, dans le code, ou dans une image Docker.

## Extraction JSON (parsing Claude)

Claude peut encadrer ses réponses JSON de blocs markdown (` ```json … ``` `). Le backend applique `extractJson()` pour nettoyer avant désérialisation :

```java
private String extractJson(String raw) {
    // Retire les fences ```json … ```
    // Extrait le tableau JSON [ … ]
}
```
