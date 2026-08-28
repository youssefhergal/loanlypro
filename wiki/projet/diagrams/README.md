# Diagrammes — Vue projet

[← Module projet](../README.md)

| Fichier | Description |
|---------|-------------|
| [classes-domaine.puml](./classes-domaine.puml) | **Complet** — 17 entités, attributs détaillés, énumérations (PlantUML) |
| [classes-domaine-slide.puml](./classes-domaine-slide.puml) | **Synthèse slide** — 2–3 attributs/classe (PlantUML) |
| [classes-domaine-slide.md](./classes-domaine-slide.md) | **Synthèse slide** — même modèle en **Mermaid** (preview Markdown / GitLab) |

**Contenu du diagramme :**

| Package | Entités |
|---------|---------|
| Identité & accès | `User`, `Role` |
| Demande de prêt | `LoanApplication`, `LoanDocument`, `LoanDocumentReview`, `LoanApplicationEvent`, `IssuedDocument` |
| Prêt & remboursement | `Loan`, `RepaymentPlan`, `Installment`, `PaymentTransaction`, `PaymentMethod`, `DirectDebitMandate` |
| Notifications | `Notification` |
| Messagerie | `Conversation`, `Message` |
| IA | `LearningSession` |

Aperçu : [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/) ou extension **PlantUML** (`Alt+D`).

**Export slide (PNG 16:9)** : dans PlantUML, exporter en SVG/PNG puis insérer dans PowerPoint / Canva. Pour un rendu plus large, zoomer sur [plantuml.com](https://www.plantuml.com/plantuml/uml/) avant export.

La doc fonctionnelle est dans [demande-pret/README.md](../../demande-pret/README.md).
