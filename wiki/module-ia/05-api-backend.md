# IA — API Backend

[← Module IA](./README.md)

## Conseiller IA

### Endpoint

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| `POST` | `/api/ai/chat` | Envoyer un message + historique, obtenir la réponse d'Alex | JWT |

### Requête

```json
{
  "messages": [
    { "role": "user",      "content": "Je veux acheter une voiture à 15 000 €." },
    { "role": "assistant", "content": "Avez-vous déjà un apport personnel ?" },
    { "role": "user",      "content": "Oui, 3 000 €." }
  ]
}
```

### Réponse

```json
{
  "content": "Avec 3 000 € d'apport sur 15 000 €, voici ce que je vous conseille…"
}
```

---

## Formation sur mesure

### Endpoints

| Méthode | URL | Description | Auth |
|---------|-----|-------------|------|
| `POST` | `/api/ai/learning/roadmap` | Générer un nouveau parcours | JWT |
| `GET` | `/api/ai/learning/sessions` | Lister les parcours de l'utilisateur | JWT |
| `GET` | `/api/ai/learning/sessions/{id}` | Détail d'un parcours | JWT |
| `POST` | `/api/ai/learning/sessions/{id}/steps/{index}/content` | Générer le contenu d'une étape | JWT |
| `PATCH` | `/api/ai/learning/sessions/{id}/steps/{index}/complete` | Marquer l'étape comme terminée | JWT |

### POST /api/ai/learning/roadmap

**Requête :**
```json
{ "objective": "Comprendre comment épargner chaque mois" }
```

**Réponse :**
```json
{
  "id": 1,
  "objective": "Comprendre comment épargner chaque mois",
  "steps": [
    { "index": 0, "title": "Analyser ses revenus et dépenses", "description": "…", "estimatedMinutes": 15 },
    { "index": 1, "title": "Définir un budget mensuel",         "description": "…", "estimatedMinutes": 20 }
  ],
  "completedSteps": [],
  "completedCount": 0,
  "totalSteps": 6,
  "status": "IN_PROGRESS",
  "createdAt": "2026-07-01T18:00:00Z",
  "updatedAt": "2026-07-01T18:00:00Z"
}
```

### POST /sessions/{id}/steps/{index}/content

Pas de body. Génère à la volée le contenu pédagogique de l'étape via Claude.

**Réponse :**
```json
{ "content": "**Introduction**\n\nComprendre vos revenus est la première étape…" }
```

Le contenu est en **Markdown** — le frontend le convertit en HTML.

### PATCH /sessions/{id}/steps/{index}/complete

Pas de body. Marque l'étape comme complétée et retourne la session mise à jour.
Si toutes les étapes sont complétées → `status: "COMPLETED"`.

---

## Entité LearningSession

```
learning_sessions
 ├── id (BIGINT, PK)
 ├── user_id (FK → users)
 ├── objective (VARCHAR 500)
 ├── roadmap_json (TEXT)         ← tableau JSON des étapes
 ├── completed_steps (VARCHAR 50) ← ex: "0,2,3" (séparés par virgule)
 ├── status (ENUM: IN_PROGRESS | COMPLETED)
 ├── created_at
 └── updated_at
```

Index sur `user_id` pour les requêtes par utilisateur.

---

## Configuration

```yaml
# application.yml
app:
  ai:
    anthropic-api-key: ${ANTHROPIC_API_KEY:}
    model: ${AI_MODEL:claude-haiku-4-5-20251001}
```

Pour la prod, ajouter `ANTHROPIC_API_KEY` dans les variables CI/CD GitLab (masked + protected).
