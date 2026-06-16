# API backend — Prêt & Paiements

[← Module](./README.md)

Base : `/api/v1/loans` · JWT requis · Swagger tag **Loan Repayment**.

> **Statut :** implémenté (profils `dev` / `demo`). IBAN stocké chiffré via `IbanVaultService` (token `v1:`).

## Endpoints — Client (`ROLE_CLIENT`)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/me` | Liste des prêts du client connecté |
| `GET` | `/{loanId}` | Détail prêt (synthèse + statut mandat) |
| `GET` | `/{loanId}/installments` | Échéancier complet |
| `GET` | `/{loanId}/transactions` | Historique des prélèvements |
| `GET` | `/{loanId}/history` | Historique unifié demande + remboursement |
| `POST` | `/{loanId}/mandate/activate` | IBAN + activation mandat (endpoint unique) |
| `POST` | `/{loanId}/mandate/revoke` | Révoquer le mandat actif |

> L’ancien `POST /{loanId}/payment-method` n’est pas exposé : l’IBAN est saisi via `mandate/activate`.

### `POST /{loanId}/mandate/activate` — body

```json
{
  "iban": "FR7630001007941234567890185",
  "holderName": "Jean Dupont"
}
```

Réponse : IBAN **masqué** uniquement (`ibanMasked`).

## Endpoints — Conseiller (`ROLE_CONSEILLER`, lecture seule)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/advisor/loans` | Prêts des clients affectés |
| `GET` | `/advisor/loans/{loanId}` | Détail + échéances + transactions |
| `GET` | `/advisor/loans/{loanId}/history` | Historique unifié |

Query params : `status`, `overdueOnly`, `page`, `size`.

## Endpoints — Admin (`ROLE_ADMIN`, lecture seule)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/admin/loans/kpi` | KPI recouvrement |
| `GET` | `/admin/loans` | Tous les prêts |
| `GET` | `/admin/loans/{loanId}` | Détail complet |
| `GET` | `/admin/loans/{loanId}/history` | Historique unifié |

## Endpoints — Scheduler interne

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `POST` | `/internal/scheduler/run-due-installments` | Déclencher le job d’échéances |
| `POST` | `/internal/scheduler/run-retries` | Déclencher les retries |

**Sécurité :** `ROLE_ADMIN` **ou** header `X-Scheduler-Token` (valeur configurée, ex. `dev-scheduler-token` en dev).

## Contrôle d’accès

| Rôle | Règle |
|------|-------|
| Client | `loan.borrowerId` = utilisateur connecté |
| Conseiller | `loan.assignedAdvisorId` = utilisateur connecté |
| Admin | Tous les prêts |

## Services backend

| Classe | Rôle |
|--------|------|
| `LoanRepaymentController` | REST client |
| `AdvisorLoanRepaymentController` | REST conseiller |
| `AdminLoanRepaymentController` | REST admin |
| `RepaymentInternalSchedulerController` | Déclenchement manuel job |
| `RepaymentPlanService` | Génération plan à l’approbation |
| `MandateService` | IBAN chiffré + mandat + révocation |
| `DirectDebitExecutionService` | Exécution prélèvement |
| `RepaymentQueryService` | Lectures agrégées |
| `IbanVaultService` | Chiffrement simulé AES-GCM |
| `SchedulerAccessService` | Contrôle accès scheduler |
| `InstallmentScheduler` | Cron `@Scheduled` |
| `FakePaymentProvider` | Simulation PSP |

## Jeu de données démo (`demo`)

- `DemoRepaymentSeeder` : prêt **LF-DEMO-0001** actif, 2 échéances payées, 1 en échec, mandat actif.
- Profil : `SPRING_PROFILES_ACTIVE=dev,demo`

## Configuration

```yaml
app:
  repayment:
    scheduler:
      cron: "0 */5 * * * *"   # dev : toutes les 5 min
      internal-token: dev-scheduler-token
      max-attempts: 3
      retry-days: [0, 3, 7]
  security:
    iban-vault-key: change-me-in-prod
```

## Hook depuis `LoanService.approve`

```java
repaymentPlanService.createLoanFromApprovedApplication(saved);
```
