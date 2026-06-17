# Frontend — Prêt & Paiements

[← Module](./README.md)

Documentation des **écrans et routes** pour les trois rôles (structure cible).

## Arborescence (cible)

```
features/loans/repayment/
├── client/
│   ├── my-loans/              # /mes-prets
│   ├── payments-schedule/     # /paiements
│   └── mandate-setup/         # modal ou sous-route
├── advisor/
│   ├── loan-list/             # /conseiller/prets
│   └── loan-detail/           # /conseiller/prets/:id
├── admin/
│   ├── loan-dashboard/        # /admin/prets (KPI)
│   └── loan-list/             # liste complète
└── shared/
    ├── installment-table/
    ├── payment-transaction-list/
    ├── loan-status-chip/
    └── mandate-status-banner/

core/loans/repayment/
├── services/repayment-api.service.ts
├── models/
└── constants/
```

## Client (`ROLE_CLIENT`)

Shell : `client-shell` · Guards : `authGuard`, `clientAreaGuard`.

| Route | Composant | Rôle |
|-------|-----------|------|
| `/mes-prets` | `MyLoansComponent` | Synthèse : solde, prochaine échéance, statut mandat |
| `/mes-prets/:id` | `MyLoanDetailComponent` | Détail prêt + CTA configurer mandat si `PENDING_MANDATE` |
| `/mes-prets/:id/mandat` | `MandateSetupComponent` | Saisie IBAN + confirmation mandat |
| `/paiements` | `PaymentsScheduleComponent` | Échéancier + historique prélèvements |

### Écran `/mes-prets`

- Cartes prêt : référence, montant initial, solde restant, mensualité
- Badge statut : `PENDING_MANDATE`, `ACTIVE`, `DEFAULTED`, `CLOSED`
- Alerte si mandat non configuré : bannière orange + CTA « Configurer le prélèvement »
- Lien vers `/paiements` pour le détail échéancier

### Écran `/paiements`

- Sélecteur de prêt (si plusieurs)
- Tableau échéances : n°, date, montant, capital/intérêts, statut chip
- Section « Historique des prélèvements » : date, montant, statut SUCCESS/FAILED, motif échec
- Alerte rouge si échéance `OVERDUE` ou `BLOCKED`

### Écran mandat

- Formulaire : titulaire du compte, IBAN (validation format)
- Checkbox consentement prélèvement récurrent (texte légal simplifié)
- Bouton primaire « Activer le mandat »
- IBAN masqué après enregistrement

**Aucun bouton « Payer maintenant »** — le prélèvement est automatique.

## Conseiller (`ROLE_CONSEILLER`)

Shell : `advisor-shell` · Guards : `authGuard`, `roleGuard`.

| Route | Composant | Rôle |
|-------|-----------|------|
| `/conseiller/prets` | `AdvisorLoanListComponent` | Liste prêts actifs (clients affectés) |
| `/conseiller/prets/:id` | `AdvisorLoanDetailComponent` | Détail lecture seule |

### Filtres inbox conseiller

- Statut prêt : `ACTIVE`, `DEFAULTED`, `CLOSED`
- Case « En retard uniquement »
- Recherche : référence, nom client

### Détail conseiller

- Résumé prêt + client
- Tableau échéances (lecture seule)
- Dernières transactions
- **Pas de bouton** « Enregistrer paiement » / « Valider »

## Admin (`ROLE_ADMIN`)

Shell : `admin-shell` · Guards : `authGuard`, `roleGuard`.

| Route | Composant | Rôle |
|-------|-----------|------|
| `/admin/prets` | `AdminLoanDashboardComponent` | KPI + liste |
| `/admin/prets/:id` | `AdminLoanDetailComponent` | Détail lecture seule |

### KPI dashboard admin

| Carte | Métrique |
|-------|----------|
| Prêts actifs | count |
| Prêts soldés | count |
| Encours total | € |
| Encaissé ce mois | € |
| Taux d’échec | % |
| Échéances en retard | count |

## Service API partagé (cible)

`RepaymentApiService` (`core/loans/repayment/services/repayment-api.service.ts`)

| Méthode | Usage |
|---------|--------|
| `getMyLoans()` | Client — `/mes-prets` |
| `getLoan(id)` | Détail |
| `getInstallments(loanId)` | Échéancier |
| `getTransactions(loanId)` | Historique |
| `setupPaymentMethod(loanId, dto)` | IBAN |
| `activateMandate(loanId)` | Mandat |
| `getAdvisorLoans(query)` | Conseiller |
| `getAdminKpi()` | Admin |

## Composants partagés

| Composant | Usage |
|-----------|--------|
| `installment-table` | Table échéances (client, conseiller, admin) |
| `payment-transaction-list` | Historique prélèvements |
| `loan-status-chip` | Badge `ACTIVE`, `OVERDUE`, etc. |
| `mandate-status-banner` | Alerte mandat manquant / révoqué |

## Navigation inter-écrans

| Contexte | Comportement |
|----------|--------------|
| Demande `APPROVED` sans mandat | Client voit alerte sur `/mes-demandes/:id` → lien `/mes-prets` |
| Mandat `PENDING` | CTA vers `/mes-prets/:id/mandat` |
| Prêt `DEFAULTED` | Bannière rouge client + visible conseiller/admin |

## Maquettes UXMagic

Prompts prêts à copier : [UXMAGIC-PROMPTS.md](./UXMAGIC-PROMPTS.md).
