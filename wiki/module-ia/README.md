# Module Intelligence Artificielle

[← Wiki](../README.md)

Regroupe deux fonctionnalités propulsées par **Claude (Anthropic)** :

| Fonctionnalité | Route frontend | Endpoint backend |
|----------------|---------------|-----------------|
| Conseiller IA (Alex) | `/conseiller-ia` | `POST /api/ai/chat` |
| Formation sur mesure | `/formation` | `POST /api/ai/learning/*` |

## Sommaire

| # | Document | Contenu |
|---|----------|---------|
| 1 | [01-vue-ensemble.md](./01-vue-ensemble.md) | Architecture, appel Claude, sécurité clé API |
| 2 | [02-cas-utilisation.md](./02-cas-utilisation.md) | Cas d'utilisation — conseiller IA et formation sur mesure |
| 3 | [03-sequences.md](./03-sequences.md) | Diagrammes de séquence — chat, génération roadmap, étapes |
| 4 | [05-api-backend.md](./05-api-backend.md) | Endpoints, entités, configuration |
| 5 | [05-frontend.md](./05-frontend.md) | Composants Angular, rendu markdown, états |

## Code source

| Couche | Chemin |
|--------|--------|
| Service conseiller IA | `backend/.../service/AiAdvisorService.java` |
| Service formation | `backend/.../service/AiLearningService.java` |
| Entité session | `backend/.../entity/LearningSession.java` |
| Repository | `backend/.../repository/LearningSessionRepository.java` |
| API conseiller | `backend/.../web/controller/AiAdvisorController.java` |
| API formation | `backend/.../web/controller/AiLearningController.java` |
| Frontend conseiller | `frontend/src/app/features/ai-advisor/` |
| Frontend formation | `frontend/src/app/features/ai-learning/` |
| Services frontend | `frontend/src/app/core/ai-advisor/`, `frontend/src/app/core/ai-learning/` |

## Configuration

```yaml
# application.yml — lu depuis variable d'environnement
app:
  ai:
    anthropic-api-key: ${ANTHROPIC_API_KEY:}
    model: ${AI_MODEL:claude-haiku-4-5-20251001}
```

La clé API ne doit **jamais** être committée. Elle est injectée via :
- **Local dev** : fichier `.env` à la racine (dans `.gitignore`)
- **CI/CD** : variable GitLab `ANTHROPIC_API_KEY` (masked + protected)
