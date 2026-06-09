# Séquences — Demande de prêt

[← Module](./README.md)

## 1. Client — Wizard et soumission

```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant W as LoanWizard
  participant API as LoanController
  participant S as LoanService

  C->>W: Étapes 1 à 4
  alt Nouveau dossier
    W->>API: POST /loan-applications
    API->>S: createApplication DRAFT
  else Reprise
    W->>API: PATCH /{id}
  end
  W->>API: POST /{id}/documents
  W->>API: POST /{id}/submit
  API->>S: SUBMITTED + historique
  W->>C: /demande-soumise/:id
```

## 2. Client — Suivi et historique

```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant D as LoanDetail
  participant API as LoanController

  C->>D: /mes-demandes/:id
  D->>API: GET /{id}
  D->>API: GET /documents
  D->>API: GET /history
  D->>C: Timeline + documents
```

## 3. Client — Annulation

```mermaid
sequenceDiagram
  actor C as Client
  participant D as LoanDetail
  participant API as LoanController

  C->>D: Annuler la demande
  D->>API: POST /{id}/cancel
  API-->>D: CANCELLED
  D->>C: Redirect /mes-demandes
```

## 4. Conseiller — Instruction et décision

```mermaid
sequenceDiagram
  autonumber
  actor CO as Conseiller
  participant UI as LoanAdvisorDetail
  participant API as LoanController
  participant S as LoanService

  CO->>UI: Ouvre dossier SUBMITTED
  CO->>API: POST /{id}/start-review
  API->>S: UNDER_REVIEW
  CO->>API: PUT /{id}/submitted
  Note over API: assignedAdvisor, montants, taux
  opt Pièce à corriger
    CO->>API: POST /documents/reject
  end
  alt Approbation
    CO->>API: POST /{id}/approve
    API->>S: APPROVED
  else Rejet
    CO->>API: POST /{id}/reject
    API->>S: REJECTED
  end
```

## 5. Admin — Affectation

```mermaid
sequenceDiagram
  actor AD as Admin
  participant L as LoanListAll
  participant API as LoanController

  AD->>L: GET toutes les demandes
  L->>API: GET /loan-applications
  AD->>API: PUT /{id}/submitted
  Note over API: Affecte conseiller + offre
```

## 6. Client — Complément (pendant analyse)

```mermaid
sequenceDiagram
  actor C as Client
  participant D as LoanDetail
  participant API as LoanController

  Note over D: Statut UNDER_REVIEW
  C->>D: Remplacer / ajouter pièce
  D->>API: POST /documents/complement
  API-->>D: 200 + événement historique
```
