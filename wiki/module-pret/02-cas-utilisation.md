# Cas d’utilisation — Prêt & Paiements

[← Module](./README.md)

## Acteurs

| Acteur | Rôle applicatif |
|--------|-----------------|
| Client | `ROLE_CLIENT` |
| Conseiller | `ROLE_CONSEILLER` |
| Administrateur | `ROLE_ADMIN` |
| Système | Scheduler + `PaymentProvider` (pas d’interface humaine) |

## Diagramme (PlantUML)

Aperçu : extension **PlantUML** (`Alt+D`) ou [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/).  
Fichier source : [diagrams/cas-utilisation.puml](./diagrams/cas-utilisation.puml).

```plantuml
@startuml cas-utilisation-module-pret
title Cas d'utilisation — Prêt & Paiements

left to right direction
skinparam shadowing false
skinparam packageStyle rectangle

actor "Client" as Client
actor "Conseiller" as Conseiller
actor "Administrateur" as Admin
actor "Système" as System

rectangle "Module Prêt & Paiements" {

  package "Espace client" {
    usecase "Configurer IBAN\n+ mandat" as UC_C1
    usecase "Consulter mes prêts\n(synthèse)" as UC_C2
    usecase "Consulter échéancier\n+ historique" as UC_C3
    usecase "Voir alertes\néchec / retard" as UC_C4
    usecase "Révoquer mandat" as UC_C5
  }

  package "Espace conseiller" {
    usecase "Lister prêts actifs\nde ses clients" as UC_O1
    usecase "Consulter détail\nprêt + échéances" as UC_O2
    usecase "Filtrer retards\n/ échecs récents" as UC_O3
  }

  package "Espace administrateur" {
    usecase "Consulter KPI\nrecouvrement" as UC_A1
    usecase "Lister tous\nles prêts" as UC_A2
    usecase "Consulter détail\nprêt (lecture)" as UC_A3
  }

  package "Automatisation système" {
    usecase "Générer plan\nà l'APPROVED" as UC_S1
    usecase "Exécuter prélèvement\nà l'échéance" as UC_S2
    usecase "Retry + marquer\nOVERDUE" as UC_S3
    usecase "Clôturer prêt\nsi soldé" as UC_S4
  }
}

Client --> UC_C1
Client --> UC_C2
Client --> UC_C3
Client --> UC_C4
Client --> UC_C5

Conseiller --> UC_O1
Conseiller --> UC_O2
Conseiller --> UC_O3

Admin --> UC_A1
Admin --> UC_A2
Admin --> UC_A3

System --> UC_S1
System --> UC_S2
System --> UC_S3
System --> UC_S4

UC_S2 ..> UC_C1 : <<precondition>>
UC_C3 ..> UC_C2 : <<extend>>
UC_O2 ..> UC_O1 : <<extend>>

@enduml
```

## Matrice des cas d’utilisation

| UC | Client | Conseiller | Admin | Système |
|----|:------:|:----------:|:-----:|:-------:|
| UC_C1 — Configurer IBAN + mandat | ✅ | — | — | — |
| UC_C2 — Consulter mes prêts (synthèse) | ✅ | — | — | — |
| UC_C3 — Échéancier + historique prélèvements | ✅ | — | — | — |
| UC_C4 — Voir alertes échec / retard | ✅ | — | — | — |
| UC_C5 — Révoquer mandat | ✅ | — | — | — |
| UC_O1 — Lister prêts actifs (clients affectés) | — | ✅ | — | — |
| UC_O2 — Détail prêt + échéances (lecture) | — | ✅ | — | — |
| UC_O3 — Filtrer retards / échecs | — | ✅ | — | — |
| UC_A1 — KPI recouvrement | — | — | ✅ | — |
| UC_A2 — Lister tous les prêts | — | — | ✅ | — |
| UC_A3 — Détail prêt (lecture) | — | — | ✅ | — |
| UC_S1 — Générer plan à l’APPROVED | — | — | — | ✅ |
| UC_S2 — Exécuter prélèvement | — | — | — | ✅ |
| UC_S3 — Retry + OVERDUE | — | — | — | ✅ |
| UC_S4 — Clôturer prêt soldé | — | — | — | ✅ |

## Règles transverses

- **Aucun acteur humain** n’enregistre un paiement manuellement.
- Le conseiller ne voit que les prêts des clients dont il était `assignedAdvisor` sur la demande source.
- L’admin voit **tous** les prêts ; pas d’action de paiement.
- IBAN affiché **masqué** (`FR76 **** **** 1234`) ; jamais en clair dans les logs.
- Un prélèvement = une `PaymentTransaction` ; idempotence par `(installmentId, attemptNumber)`.
- Mandat `ACTIVE` requis pour exécuter un prélèvement ; sinon échéance `BLOCKED`.

## Hors périmètre MVP

| Exclu | Version |
|-------|---------|
| Remboursement anticipé | V2 |
| Vrai PSP + webhooks | V2 (interface prête) |
| Relances email / SMS | V2 |
| Export PDF échéancier | V2 (optionnel) |
