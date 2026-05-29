# API backend — Demande de prêt

[← Module](./README.md)

Base : `/api/v1/loan-applications` · JWT requis · Swagger tag **Loan**.

## Endpoints

| Méthode | Chemin | Client | Conseiller | Admin |
|---------|--------|:------:|:----------:|:-----:|
| `POST` | `/` | ✅ | — | — |
| `GET` | `/` | ses dossiers | affectés | tous |
| `GET` | `/{id}` | ✅ | ✅ | ✅ |
| `PATCH` | `/{id}` | DRAFT | — | — |
| `PUT` | `/{id}` | DRAFT (alias) | — | — |
| `DELETE` | `/{id}` | DRAFT | ✅ | ✅ |
| `POST` | `/{id}/submit` | ✅ | — | — |
| `POST` | `/{id}/cancel` | ✅ | — | — |
| `POST` | `/{id}/start-review` | — | ✅ | — |
| `PUT` | `/{id}/submitted` | — | ✅ | ✅ |
| `POST` | `/{id}/approve` | — | ✅ | — |
| `POST` | `/{id}/reject` | — | ✅ | — |
| `POST` | `/{id}/documents/reject` | — | ✅ | — |
| `GET` | `/{id}/history` | ✅ | ✅ | ✅ |
| `POST` | `/{id}/documents` | DRAFT | — | — |
| `POST` | `/{id}/documents/complement` | UNDER_REVIEW | — | — |
| `GET` | `/{id}/documents` | ✅ | ✅ | ✅ |
| `DELETE` | `/{id}/documents/{docId}` | DRAFT | — | — |
| `GET` | `/{id}/documents/{docId}/download` | ✅ | ✅ | ✅ |

## Contrôle d’accès

- **Admin** : tous les dossiers.
- **Conseiller** : `assignedAdvisor` = utilisateur connecté.
- **Client** : `applicant` = utilisateur connecté.

## Services backend

| Classe | Rôle |
|--------|------|
| `LoanController` | REST |
| `LoanService` | Règles métier, workflow |
| `LoanApplicationHistoryService` | Journal `loan_application_events` |
| `LoanDocumentStorageService` | Fichiers `uploads/loan-documents` |

## DTO principaux

| DTO | Usage |
|-----|--------|
| `LoanRequestDto` | Création / MAJ brouillon |
| `LoanSubmittedUpdateDto` | Affectation + offre (`PUT /submitted`) |
| `CancelLoanRequestDto` | Commentaire optionnel annulation |
| `RejectDocumentRequestDto` | Rejet pièce par conseiller |

## Erreurs API

| Code | Code métier | Exemple |
|------|-------------|---------|
| 400 | `BUSINESS_RULE` | Soumission sans 5 docs, mauvais statut |
| 403 | `FORBIDDEN` | Accès dossier d’un autre client |
| 404 | `NOT_FOUND` | Id inconnu |

## Configuration

| Profil | Fichier | BDD |
|--------|---------|-----|
| `dev` | `application-dev.yml` | MySQL, `ddl-auto: update` |
| `prod` | `application-prod.yml` | `ddl-auto: validate` |

Variables prod : `SPRING_DATASOURCE_*`, `JWT_SECRET`, `LOAN_DOCUMENTS_DIR`.
