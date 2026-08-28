# Module — Demande de prêt

[← Wiki](../README.md) · Auth : [auth/README.md](../auth/README.md) · Classes : [projet/diagrams/classes-domaine.puml](../projet/diagrams/classes-domaine.puml)

Feature **complète** : dépôt et suivi (client), instruction (conseiller), supervision (admin).  
La documentation UI conseiller / admin décrit le **comportement cible** (écrans à implémenter selon ce modèle).

**Suite du parcours** (après `APPROVED`) : [module-pret](../module-pret/README.md) — prêt, mandat, prélèvement automatique.

| # | Document |
|---|----------|
| 1 | [Vue d’ensemble](./01-vue-ensemble.md) — Périmètre, objectifs, statuts |
| 2 | [Cas d’utilisation](./02-cas-utilisation.md) — Acteurs, UC, diagramme PlantUML |
| 3 | [Séquences](./03-sequences.md) — Diagrammes d’interaction |
| 4 | [API backend](./05-api-backend.md) — Endpoints REST |
| 5 | [Frontend](./05-frontend.md) — Routes, shells, composants |

## Comptes test (profil `dev`)

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| Client | `client@test.com` | `password` |
| Conseiller | `conseiller@test.com` | `password` |
| Admin | `admin@test.com` | `password` |

## Code source (racine)

| Couche | Chemin |
|--------|--------|
| API | `backend/.../web/controller/LoanController.java` |
| Métier | `backend/.../service/LoanService.java` |
| Historique | `backend/.../service/LoanApplicationHistoryService.java` |
| Fichiers | `backend/.../service/LoanDocumentStorageService.java` |
| Routes | `frontend/src/app/app.routes.ts` |
| Client | `frontend/.../features/loans/applicant/` |
| Conseiller | `frontend/.../features/loans/advisor/` |
| Admin | `frontend/.../features/loans/admin/` |

Swagger : `http://localhost:8080/swagger-ui.html` (tag **Loan**).
