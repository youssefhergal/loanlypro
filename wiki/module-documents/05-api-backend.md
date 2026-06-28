# Documents — API backend

[← Module Documents](./README.md)

Base : `/api/v1/documents` — `ClientDocumentsController`. Tous les endpoints requièrent
un JWT (`ROLE_CLIENT`).

## Endpoints

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/me/justificatifs` | Justificatifs groupés par dossier |
| `GET` | `/me/justificatifs/{documentId}/download` | Télécharge un justificatif (propriétaire) |
| `GET` | `/me/credit` | Liste des documents crédit (avec disponibilité) |
| `GET` | `/me/credit/{type}/{referenceId}/download` | PDF d'un document crédit |
| `GET` | `/me/loans/{loanId}/schedule.pdf` | PDF de l'échéancier |
| `GET` | `/me/loans/{loanId}/payments.pdf` | PDF du relevé de prélèvements |

`{type}` ∈ `APPLICATION_RECAP`, `OFFER`, `LOAN_CONTRACT`, `SEPA_MANDATE`.
`{referenceId}` = `loanApplicationId` (récap/offre/contrat) ou `loanId` (mandat SEPA).

## Services

| Service | US | Rôle |
|---------|----|------|
| `ClientDocumentsService` | 6.1 | Agrège les justificatifs + statut review ; téléchargement sécurisé |
| `IssuedDocumentService` | 6.2 | Calcule la disponibilité + génère les PDF crédit |
| `RepaymentDocumentExportService` | 6.3 | Génère les PDF échéancier / prélèvements |
| `DefaultLoanDocumentPdfGenerator` | 6.2/6.3 | Moteur de rendu PDF (sur `SimplePdfDocument`) |

## Codes d'erreur

| Code HTTP | Cas |
|-----------|-----|
| `403 FORBIDDEN` | accès à un document d'un autre utilisateur |
| `404 NOT_FOUND` | dossier / prêt / justificatif introuvable |
| `400 BUSINESS_RULE` | document crédit non disponible pour ce dossier |
