# Séquences — Prêt & Paiements

[← Module](./README.md)

## 1. Système — Génération du plan à l’approbation

```mermaid
sequenceDiagram
  autonumber
  participant LS as LoanService
  participant RPS as RepaymentPlanService
  participant DB as MySQL

  Note over LS: POST /approve → APPROVED
  LS->>RPS: onApplicationApproved(loanApplicationId)
  RPS->>DB: Créer Loan (PENDING_MANDATE)
  RPS->>DB: Créer RepaymentPlan
  RPS->>DB: Créer Installments (UPCOMING)
  RPS->>DB: Événement LOAN_CREATED
```

## 2. Client — Configuration IBAN et mandat

```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant UI as MandateSetupComponent
  participant API as LoanRepaymentController
  participant MS as MandateService

  C->>UI: /mes-prets → « Configurer le prélèvement »
  UI->>API: POST /loans/{id}/payment-method
  API->>MS: Enregistrer IBAN (chiffré/masqué)
  MS-->>API: PaymentMethod créé
  C->>UI: Confirmer mandat SEPA
  UI->>API: POST /loans/{id}/mandate/activate
  API->>MS: Mandat ACTIVE
  MS-->>API: Loan → ACTIVE (si plan existant)
  API-->>UI: 200 + mandat actif
```

## 3. Système — Prélèvement automatique (échéance du jour)

```mermaid
sequenceDiagram
  autonumber
  participant SCH as InstallmentScheduler
  participant DDES as DirectDebitExecutionService
  participant PSP as PaymentProvider
  participant FAKE as FakePaymentProvider
  participant DB as MySQL

  SCH->>DDES: processDueInstallments(today)
  DDES->>DB: Installments dueDate=today, status=UPCOMING/DUE
  loop Pour chaque échéance
    alt Mandat non ACTIVE
      DDES->>DB: Installment → BLOCKED
    else Mandat ACTIVE
      DDES->>DB: PaymentTransaction PENDING
      DDES->>PSP: debit(request)
      PSP->>FAKE: simulateDebit()
      alt SUCCESS
        FAKE-->>PSP: success + externalRef
        PSP-->>DDES: PaymentResult.SUCCESS
        DDES->>DB: Transaction SUCCESS, Installment PAID
      else FAILED
        FAKE-->>PSP: failureReason
        PSP-->>DDES: PaymentResult.FAILED
        DDES->>DB: Transaction FAILED, planifier retry
      end
    end
  end
```

## 4. Système — Retry et passage en retard

```mermaid
sequenceDiagram
  autonumber
  participant SCH as InstallmentScheduler
  participant ODS as OverdueDetectionService
  participant DB as MySQL

  Note over SCH: J+3 ou J+7 après échec
  SCH->>ODS: processRetries(today)
  ODS->>DB: Installments FAILED, retryDate=today
  ODS->>ODS: Nouvelle tentative via DirectDebitExecutionService
  alt 3e échec
    ODS->>DB: Installment → OVERDUE
    ODS->>DB: Loan → DEFAULTED (si règle atteinte)
  end
```

## 5. Client — Consultation échéancier

```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant UI as PaymentsScheduleComponent
  participant API as LoanRepaymentController
  participant RQS as RepaymentQueryService

  C->>UI: /paiements
  UI->>API: GET /loans/me
  API->>RQS: Prêts du client
  UI->>API: GET /loans/{id}/installments
  API->>RQS: Échéances + transactions
  RQS-->>UI: Échéancier + historique SUCCESS/FAILED
```

## 6. Conseiller — Consultation (lecture seule)

```mermaid
sequenceDiagram
  autonumber
  actor CO as Conseiller
  participant UI as AdvisorLoansComponent
  participant API as LoanRepaymentController

  CO->>UI: /conseiller/prets
  UI->>API: GET /advisor/loans?status=ACTIVE
  API-->>UI: Liste prêts clients affectés
  CO->>UI: Ouvre détail
  UI->>API: GET /advisor/loans/{id}
  API-->>UI: Plan, échéances, dernières transactions
  Note over UI: Aucun bouton « Enregistrer paiement »
```

## 7. Admin — KPI recouvrement

```mermaid
sequenceDiagram
  autonumber
  actor AD as Admin
  participant UI as AdminLoansDashboard
  participant API as LoanRepaymentController

  AD->>UI: /admin/prets
  UI->>API: GET /admin/loans/kpi
  API-->>UI: actifs, soldés, encours, taux échec, retards
  AD->>UI: Liste complète
  UI->>API: GET /admin/loans
  API-->>UI: Tous les prêts (lecture seule)
```

## 8. Clôture automatique du prêt

```mermaid
sequenceDiagram
  participant DDES as DirectDebitExecutionService
  participant RPS as RepaymentPlanService
  participant DB as MySQL

  Note over DDES: Dernière échéance PAID
  DDES->>RPS: checkLoanClosure(loanId)
  RPS->>DB: Toutes installments PAID ?
  RPS->>DB: Loan → CLOSED
  RPS->>DB: Événement LOAN_CLOSED
```
