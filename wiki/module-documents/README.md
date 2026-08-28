# Module Documents — GED client (Sprint 6 / EPIC-8)

[← Wiki](../README.md)

Espace client `/documents` regroupant en 3 onglets tous les documents liés aux prêts :
justificatifs déposés, documents crédit émis par LoanlyPro, et exports PDF
échéancier / prélèvements.

## Sommaire

- [Vue d'ensemble](./01-vue-ensemble.md)
- [API backend](./05-api-backend.md)
- [Frontend](./05-frontend.md)

## Périmètre MVP

- Rôle `ROLE_CLIENT` uniquement (chaque endpoint est restreint au propriétaire).
- Consultation + téléchargement (pas de re-upload sur `/documents`).
- 3 types de documents (voir diagramme ci-dessous).

## Les 3 types de documents

```
                        ┌──────────────────────────┐
                        │      /documents (client)  │
                        └────────────┬─────────────┘
            ┌────────────────────────┼────────────────────────┐
            ▼                        ▼                        ▼
 ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────────────┐
 │ Mes justificatifs  │  │     Mon crédit     │  │ Échéancier & prélèvements │
 ├────────────────────┤  ├────────────────────┤  ├──────────────────────────┤
 │ LoanDocument       │  │ Récap / Offre /    │  │ PDF générés à la volée    │
 │ (pièces wizard)    │  │ Contrat / Mandat   │  │ depuis le prêt actif      │
 │ + statut review    │  │ SEPA (PDF générés) │  │ (échéances + transactions)│
 └────────────────────┘  └────────────────────┘  └──────────────────────────┘
```

## Génération PDF — sans dépendance

Les PDF (documents crédit, échéancier, relevé) sont produits par un writer maison
`SimplePdfDocument` (aucune librairie iText/PDFBox ajoutée au projet). Le moteur de
rendu `DefaultLoanDocumentPdfGenerator` reçoit des modèles déjà préparés par les
services et renvoie des octets PDF. Aucun stockage : tout est généré en mémoire.

## Stockage

Le téléchargement des justificatifs réutilise l'abstraction existante
`LoanDocumentStorageBackend` (local en dev, GCS en prod) : aucune configuration
supplémentaire requise pour ce module.
