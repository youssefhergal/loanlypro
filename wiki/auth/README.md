# Module — Authentification (`auth`)

[← Retour au wiki](../README.md)

Tout ce qui concerne **l’accès à l’application** : inscription, connexion, JWT, rôles, protection des routes et des API.

| # | Document |
|---|----------|
| 1 | [Vue d’ensemble](./01-vue-ensemble.md) |
| 2 | [Cas d’utilisation](./02-cas-utilisation.md) |
| 3 | [Diagrammes de séquence](./03-sequences.md) |
| 4 | [API backend](./04-api-backend.md) |
| 5 | [Frontend (guards, interceptor)](./05-frontend.md) |

## Fichiers source

| Couche | Chemin |
|--------|--------|
| API | `backend/.../web/controller/AuthController.java` |
| Service | `backend/.../service/AuthService.java` |
| Sécurité | `backend/.../config/SecurityConfig.java` |
| JWT | `backend/.../security/JwtService.java`, `JwtAuthenticationFilter.java` |
| Angular | `frontend/src/app/core/auth/` |
| Écrans | `frontend/src/app/features/auth/` |
