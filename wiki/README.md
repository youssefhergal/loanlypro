# Wiki — LoanlyPro

[← README racine](../README.md) · Diagrammes : [DIAGRAMMES.md](./DIAGRAMMES.md)

## Modules

| Dossier | Contenu |
|---------|---------|
| [auth/](./auth/README.md) | Connexion, JWT, guards |
| [**demande-pret/**](./demande-pret/README.md) | Demande de prêt — client, conseiller, admin |
| [**module-pret/**](./module-pret/README.md) | Prêt & paiements — prélèvement auto, échéancier, KPI |
| [**module-notifications/**](./module-notifications/README.md) | E-mails (Resend) + centre de notifications in-app |
| [**module-documents/**](./module-documents/README.md) | GED client — justificatifs, documents crédit, exports PDF |
| [projet/](./projet/README.md) | Architecture + diagramme de classes |

## Modèle de données

Diagrammes de classes :
- [Complet (PlantUML)](./projet/diagrams/classes-domaine.puml) — référence technique
- [Synthèse slide (PlantUML)](./projet/diagrams/classes-domaine-slide.puml)
- [Synthèse slide (Mermaid)](./projet/diagrams/classes-domaine-slide.md) — preview GitLab / VS Code

## Nouveau module wiki

Voir [projet/GUIDE-NOUVEAU-MODULE.md](./projet/GUIDE-NOUVEAU-MODULE.md) (structure type `demande-pret/`).
