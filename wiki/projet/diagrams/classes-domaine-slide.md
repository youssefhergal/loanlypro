# LoanlyPro — Diagramme de classes (Mermaid, synthèse slide)

[← Diagrammes](./README.md) · Détail PlantUML : [classes-domaine.puml](./classes-domaine.puml)

Vue **synthèse** du modèle JPA : 17 entités, 2–3 attributs par classe, adaptée aux slides et à la preview Markdown (GitLab, VS Code, Mermaid Live).

**Prévisualisation :** [mermaid.live](https://mermaid.live) — coller le bloc ci-dessous.

```mermaid
classDiagram
    direction TB

    subgraph Identité["Identité"]
        class User {
            Long id
            String email
            String firstName
        }
        class Role {
            Long id
            String name
        }
    end

    subgraph Demande["Demande de prêt"]
        class LoanApplication {
            Long id
            String reference
            LoanApplicationStatus status
        }
        class LoanDocument {
            Long id
            LoanDocumentType documentType
            String storagePath
        }
        class LoanDocumentReview {
            Long id
            LoanDocumentReviewStatus reviewStatus
        }
        class LoanApplicationEvent {
            Long id
            LoanApplicationEventType eventType
            Instant occurredAt
        }
        class IssuedDocument {
            Long id
            IssuedDocumentType documentType
        }
    end

    subgraph Pret["Prêt & remboursement"]
        class Loan {
            Long id
            LoanStatus status
            BigDecimal remainingBalance
        }
        class RepaymentPlan {
            Long id
            BigDecimal monthlyPayment
            Integer installmentCount
        }
        class Installment {
            Long id
            LocalDate dueDate
            InstallmentStatus status
        }
        class PaymentTransaction {
            Long id
            BigDecimal amount
            PaymentTransactionStatus status
        }
        class PaymentMethod {
            Long id
            String ibanMasked
        }
        class DirectDebitMandate {
            Long id
            String mandateReference
            MandateStatus status
        }
    end

    subgraph NotifMsg["Notifications & messagerie"]
        class Notification {
            Long id
            String title
            Instant readAt
        }
        class Conversation {
            Long id
            Instant lastMessageAt
        }
        class Message {
            Long id
            String content
            Instant sentAt
        }
    end

    subgraph IA["IA"]
        class LearningSession {
            Long id
            String objective
            LearningSessionStatus status
        }
    end

    %% Identité
    User "0..*" -- "0..*" Role : user_roles
    User "1" --> "0..*" LoanApplication : applicant
    User "0..1" --> "0..*" LoanApplication : conseiller
    User "1" --> "0..*" Loan : emprunteur
    User "1" --> "0..*" PaymentMethod
    User "1" --> "0..*" Notification
    User "1" --> "0..*" Conversation
    User "1" --> "0..*" Message : envoie
    User "1" --> "0..*" LearningSession

    %% Demande & prêt
    LoanApplication "1" *-- "0..*" LoanDocument
    LoanApplication "1" *-- "0..*" LoanDocumentReview
    LoanApplication "1" *-- "0..*" LoanApplicationEvent
    LoanApplication "1" *-- "0..*" IssuedDocument
    LoanApplication "1" --> "0..1" Loan

    Loan "1" --> "1" RepaymentPlan
    RepaymentPlan "1" *-- "1..*" Installment
    Installment "1" *-- "0..*" PaymentTransaction

    Loan "1" --> "0..1" DirectDebitMandate
    PaymentMethod "1" --> "0..*" DirectDebitMandate

    Conversation "1" *-- "0..*" Message
```

## Export pour slide

1. Ouvrir [mermaid.live](https://mermaid.live) avec le diagramme ci-dessus
2. **Actions → PNG / SVG**
3. Insérer dans PowerPoint, Canva ou Google Slides

> **Astuce :** si le rendu est trop dense, exporter en SVG et zoomer dans la slide, ou utiliser la version PlantUML [classes-domaine-slide.puml](./classes-domaine-slide.puml).
