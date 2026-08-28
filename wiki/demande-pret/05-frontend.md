# Frontend — Demande de prêt

[← Module](./README.md)

Documentation des **écrans et routes** pour les trois rôles (structure cible du module).

## Arborescence

```
features/loans/
├── applicant/          # Client
│   ├── loan-wizard/
│   ├── loan-list/
│   ├── loan-detail/
│   └── loan-submitted/
├── advisor/            # Conseiller
│   ├── loan-inbox/
│   └── loan-detail/
└── admin/              # Administrateur
    ├── loan-list-all/
    └── loan-detail/

features/layout/
├── client-shell/
├── advisor-shell/
└── admin-shell/

core/loans/
├── services/loan-api.service.ts
├── models/
└── utils/
```

## Client (`ROLE_CLIENT`)

Shell : `client-shell` · Guards : `authGuard`, `clientAreaGuard`.

| Route | Composant | Rôle |
|-------|-----------|------|
| `/dashboard` | `DashboardComponent` | Accueil |
| `/nouvelle-demande` | `LoanWizardComponent` | Création |
| `/nouvelle-demande/:id` | `LoanWizardComponent` | Reprise brouillon |
| `/demande-soumise/:id` | `LoanSubmittedComponent` | Confirmation submit |
| `/mes-demandes` | `LoanListComponent` | Liste |
| `/mes-demandes/:id` | `LoanDetailComponent` | Détail, historique, annulation, complément |

**Wizard** : 4 étapes (`step-need`, `step-situation`, `step-documents`, `step-summary`).

## Conseiller (`ROLE_CONSEILLER`)

Shell : `advisor-shell` · Guards : `authGuard`, `roleGuard` (`ROLE_CONSEILLER`).

| Route | Composant | Rôle |
|-------|-----------|------|
| `/conseiller/dashboard` | `AdvisorDashboardComponent` | Accueil |
| `/conseiller/dossiers` | `LoanInboxComponent` | Liste dossiers **affectés** |
| `/conseiller/dossiers/:id` | `LoanAdvisorDetailComponent` | Instruction : analyse, offre, rejet pièce, décision |
| `/conseiller/prets` | *(module prêts — à venir)* | Prêts accordés |

**Actions fiche conseiller** : `start-review`, `PUT submitted`, `approve`, `reject`, `documents/reject`, téléchargement pièces, lecture historique.

## Administrateur (`ROLE_ADMIN`)

Shell : `admin-shell` · Guards : `authGuard`, `roleGuard` (`ROLE_ADMIN`).

| Route | Composant | Rôle |
|-------|-----------|------|
| `/admin/dashboard` | `AdminDashboardComponent` | Accueil |
| `/admin/demandes` | `LoanListAllComponent` | **Toutes** les demandes |
| `/admin/demandes/:id` | `LoanAdminDetailComponent` | Supervision, affectation conseiller, offre |
| `/admin/prets` | *(module prêts — à venir)* | Vue globale prêts |

L’admin **n’appelle pas** `approve` / `reject` (réservé conseiller).

## Service API partagé

`LoanApiService` (`core/loans/services/loan-api.service.ts`) centralise les appels REST pour les trois espaces.

| Méthode service | Usage typique |
|-----------------|---------------|
| `create`, `update` | Wizard client |
| `submit`, `cancel` | Client |
| `getApplications`, `getById` | Listes / détails |
| `getHistory`, `getDocuments` | Détail |
| `uploadDocument`, `uploadComplement` | Pièces |
| `startReview`, `updateSubmitted`, `approve`, `reject` | Conseiller / admin |

## Navigation inter-écrans (client)

| Contexte | Comportement |
|----------|--------------|
| Carte `DRAFT` | → wizard |
| Autre statut | → détail |
| Détail `DRAFT` | → redirect wizard |

## Composants partagés

- `features/loans/shared/` — styles sections
- `shared/confirm-dialog/` — confirmations (submit, delete, cancel)
- `document-upload-slot` — upload par type de document
