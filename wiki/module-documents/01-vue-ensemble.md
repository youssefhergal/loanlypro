# Documents — Vue d'ensemble

[← Module Documents](./README.md)

## Objectif

Centraliser, pour le client, tous les documents liés à ses prêts sur un seul écran
`/documents`, structuré en 3 onglets Material.

## Découpage fonctionnel

| Onglet UI | Type métier | Source de données |
|-----------|-------------|-------------------|
| **Mes justificatifs** | Pièces déposées au wizard | `LoanDocument` (par dossier) + statut `LoanDocumentReview` |
| **Mon crédit** | Documents émis par la plateforme | Récap demande, offre, contrat, mandat SEPA (PDF générés) |
| **Échéancier & prélèvements** | Exports PDF remboursement | Échéances + transactions du prêt actif (PDF générés) |

## Disponibilité des documents crédit

| Type | Disponible quand |
|------|------------------|
| `APPLICATION_RECAP` | dossier `SUBMITTED` ou plus avancé |
| `OFFER` | dossier `OFFER_PENDING` ou `APPROVED` |
| `LOAN_CONTRACT` | dossier `APPROVED` |
| `SEPA_MANDATE` | mandat `ACTIVE` sur le prêt |

Un document non disponible est renvoyé avec `available=false` + un motif, et la carte
correspondante est grisée côté UI.

## Contrôle d'accès

Chaque endpoint utilise l'e-mail authentifié (`Authentication.getName()`) :
- justificatif : propriétaire du dossier ;
- document crédit : propriétaire du dossier (ou du prêt pour le mandat) ;
- exports PDF : emprunteur du prêt uniquement.

Tout accès non propriétaire renvoie `403 FORBIDDEN`.

## Seed démo (US-6.4)

`DemoIssuedDocumentSeeder` (`@Profile("demo")`, `@Order(50)`) crée des entrées
`IssuedDocument` pour `LF-DEMO-0001` afin d'afficher des dates d'émission réalistes.
Idempotent (skip si déjà présent).
