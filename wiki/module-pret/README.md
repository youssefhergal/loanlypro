# Module — Prêt & Paiements (remboursement automatique)

[← Wiki](../README.md) · Demande de prêt : [demande-pret/README.md](../demande-pret/README.md) · Classes : [projet/diagrams/classes-domaine.puml](../projet/diagrams/classes-domaine.puml)

Feature **post-approbation** : génération du plan d’amortissement, mandat de prélèvement client, exécution automatique des échéances via un `PaymentProvider` (implémentation **fake** en projet fil rouge, architecture prête pour un vrai PSP).

| # | Document |
|---|----------|
| 1 | [Vue d’ensemble](./01-vue-ensemble.md) — Périmètre, acteurs, statuts, lien avec `demande-pret` |
| 2 | [Cas d’utilisation](./02-cas-utilisation.md) — Acteurs, UC, diagramme PlantUML |
| 3 | [Séquences](./03-sequences.md) — Génération plan, mandat, prélèvement auto, échecs |
| 4 | [Entités & modèle](./04-entites-modele.md) — Domaine, enums, `PaymentProvider` |
| 5 | [API backend](./05-api-backend.md) — Endpoints REST implémentés |
| 6 | [Frontend](./06-frontend.md) — Routes, shells, composants |
| 7 | [User stories Jira](./07-user-stories-jira.md) — Backlog Sprint 4 révisé |
| — | [Prompts UXMagic](./UXMAGIC-PROMPTS.md) — Maquettes UI client / conseiller / admin |

## Principe directeur

| Acteur | Rôle sur les remboursements |
|--------|------------------------------|
| **Client** | Configure IBAN + mandat une fois ; consulte prêts et échéancier |
| **Système** | Exécute les prélèvements automatiquement (`InstallmentScheduler` + `PaymentProvider`) |
| **Conseiller** | **Lecture seule** — état du prêt, échéances, échecs |
| **Admin** | **KPI + consultation** — pas d’action sur les paiements |

Le conseiller **n’enregistre jamais** un paiement manuellement.

## Comptes test (profil `dev`)

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| Client | `client@test.com` | `password` |
| Conseiller | `conseiller@test.com` | `password` |
| Admin | `admin@test.com` | `password` |

## Code source (cible — à implémenter)

| Couche | Chemin prévu |
|--------|----------------|
| API | `backend/.../web/controller/LoanRepaymentController.java` |
| Métier | `backend/.../service/repayment/*` |
| Scheduler | `backend/.../config/InstallmentScheduler.java` |
| PSP | `backend/.../payment/PaymentProvider.java`, `FakePaymentProvider.java` |
| Routes | `frontend/src/app/app.routes.ts` (`/mes-prets`, `/paiements`, …) |
| Client | `frontend/.../features/loans/repayment/client/` |
| Conseiller | `frontend/.../features/loans/repayment/advisor/` |
| Admin | `frontend/.../features/loans/repayment/admin/` |

Swagger (cible) : tag **Loan Repayment** · `http://localhost:8080/swagger-ui.html`
