# Cas d’utilisation — Demande de prêt

[← Module](./README.md)

## Acteurs

| Acteur | Rôle applicatif |
|--------|-----------------|
| Client | `ROLE_CLIENT` |
| Conseiller | `ROLE_CONSEILLER` |
| Administrateur | `ROLE_ADMIN` |

## Diagramme (PlantUML — cas d’utilisation UML)

Aperçu : extension **PlantUML** (`Alt+D`) sur ce fichier, ou copier le bloc sur [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/).  
Fichier source : [diagrams/cas-utilisation.puml](./diagrams/cas-utilisation.puml).

```plantuml
@startuml cas-utilisation-demande-pret
title Cas d'utilisation — Demande de prêt

left to right direction
skinparam shadowing false
skinparam packageStyle rectangle

actor "Client" as Client
actor "Conseiller" as Conseiller
actor "Administrateur" as Admin

rectangle "Système Demande de prêt" {

  package "Espace client" {
    usecase "Créer / modifier\nbrouillon" as UC_C1
    usecase "Gérer documents\n(brouillon)" as UC_C2
    usecase "Supprimer\nbrouillon" as UC_C3
    usecase "Soumettre\navec pièces" as UC_C4
    usecase "Consulter liste\net détail" as UC_C5
    usecase "Historique" as UC_C6
    usecase "Annuler\ndemande" as UC_C7
    usecase "Complément\ndocumentaire" as UC_C8
  }

  package "Espace conseiller" {
    usecase "Lister dossiers\naffectés" as UC_O1
    usecase "Consulter dossier" as UC_O2
    usecase "Mettre en analyse" as UC_O3
    usecase "Affectation\net offre" as UC_O4
    usecase "Rejeter\nune pièce" as UC_O5
    usecase "Approuver /\nRejeter dossier" as UC_O6
  }

  package "Espace administrateur" {
    usecase "Lister toutes\nles demandes" as UC_A1
    usecase "Consulter dossier" as UC_A2
    usecase "Affecter conseiller\net offre" as UC_A3
  }
}

Client --> UC_C1
Client --> UC_C2
Client --> UC_C3
Client --> UC_C4
Client --> UC_C5
Client --> UC_C6
Client --> UC_C7
Client --> UC_C8

Conseiller --> UC_O1
Conseiller --> UC_O2
Conseiller --> UC_O3
Conseiller --> UC_O4
Conseiller --> UC_O5
Conseiller --> UC_O6

Admin --> UC_A1
Admin --> UC_A2
Admin --> UC_A3

UC_C4 ..> UC_C2 : <<include>>
UC_C4 ..> UC_C1 : <<include>>
UC_C7 ..> UC_C5 : <<extend>>
UC_C8 ..> UC_C5 : <<extend>>
UC_O6 ..> UC_O4 : <<include>>

@enduml
```

## Matrice des cas d’utilisation

| UC | Client | Conseiller | Admin |
|----|:------:|:----------:|:-----:|
| Créer / modifier brouillon (wizard) | ✅ | — | — |
| Gérer documents (brouillon) | ✅ | — | — |
| Supprimer brouillon | ✅ | ✅* | ✅* |
| Soumettre | ✅ | — | — |
| Consulter liste | ses dossiers | affectés | tous |
| Consulter détail + historique | ✅ | ✅ | ✅ |
| Annuler (`SUBMITTED` / `UNDER_REVIEW`) | ✅ | — | — |
| Complément documentaire | ✅ | — | — |
| Mettre en analyse | — | ✅ | — |
| `PUT /submitted` (offre + affectation) | — | ✅ | ✅ |
| Rejeter un document | — | ✅ | — |
| Approuver / Rejeter dossier | — | ✅ | — |

\* Conseiller / admin : suppression hors règle `DRAFT` client (cf. `LoanService.deleteApplication`).

## Règles transverses

- **5 pièces obligatoires** à la soumission : identité, bulletins, avis d’imposition, relevés, domicile.
- Fichiers : PDF / JPG / PNG, max **10 Mo**.
- Un conseiller ne voit que les dossiers où il est `assignedAdvisor` ; l’admin voit tout.
