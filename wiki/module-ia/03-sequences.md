# IA — Diagrammes de séquence

[← Module IA](./README.md)

## Conseiller IA — Envoi d'un message

```mermaid
sequenceDiagram
  autonumber
  actor U as Utilisateur
  participant AC as AiAdvisorComponent
  participant AS as AiAdvisorService
  participant CTL as AiAdvisorController
  participant SVC as AiAdvisorService (Spring)
  participant CL as Claude API

  U->>AC: Saisit un message + Envoyer
  AC->>AC: Ajoute message dans messages[]
  AC->>AC: isTyping = true, disable textarea
  AC->>AS: sendMessage(messages[])
  AS->>CTL: POST /api/ai/chat { messages[] }
  CTL->>SVC: chat(request)
  SVC->>CL: POST /v1/messages (system prompt + historique)
  CL-->>SVC: { content: "..." }
  SVC-->>CTL: AiChatResponse
  CTL-->>AS: 200 { content }
  AS-->>AC: réponse texte
  AC->>AC: Ajoute réponse assistant dans messages[]
  AC->>AC: isTyping = false, enable textarea
  AC->>U: Affiche réponse d'Alex
```

## Formation — Génération d'un parcours

```mermaid
sequenceDiagram
  autonumber
  actor U as Utilisateur
  participant LC as AiLearningComponent
  participant LS as AiLearningService
  participant CTL as AiLearningController
  participant SVC as AiLearningService (Spring)
  participant CL as Claude API
  participant DB as Base de données

  U->>LC: Saisit un objectif + Générer
  LC->>LS: generateRoadmap(objective)
  LS->>CTL: POST /api/ai/learning/roadmap { objective }
  CTL->>SVC: generateRoadmap(objective, userId)
  SVC->>CL: POST /v1/messages (prompt roadmap JSON)
  CL-->>SVC: JSON 5–7 étapes
  SVC->>SVC: extractJson() + désérialisation
  SVC->>DB: persist(LearningSession)
  DB-->>SVC: session.id
  SVC-->>CTL: LearningSessionResponse
  CTL-->>LS: 201 session avec steps[]
  LS-->>LC: session
  LC->>LC: view = 'roadmap'
  LC->>U: Affiche le parcours généré
```

## Formation — Génération du contenu d'une étape

```mermaid
sequenceDiagram
  autonumber
  actor U as Utilisateur
  participant LC as AiLearningComponent
  participant LS as AiLearningService
  participant CTL as AiLearningController
  participant SVC as AiLearningService (Spring)
  participant CL as Claude API
  participant DB as Base de données

  U->>LC: Clique sur une étape
  LC->>LS: generateStepContent(sessionId, stepIndex)
  LS->>CTL: POST /api/ai/learning/sessions/{id}/steps/{i}/content
  CTL->>SVC: generateStepContent(sessionId, stepIndex, userId)
  SVC->>DB: getSession(sessionId)
  DB-->>SVC: LearningSession (steps JSON)
  SVC->>CL: POST /v1/messages (prompt contenu étape)
  CL-->>SVC: Contenu pédagogique Markdown
  SVC-->>CTL: { content: "..." }
  CTL-->>LS: 200 { content }
  LS-->>LC: contenu markdown
  LC->>LC: stepContent = content, view = 'step-detail'
  LC->>U: Affiche contenu formaté (mdToHtml)
```

## Formation — Complétion d'une étape

```mermaid
sequenceDiagram
  autonumber
  actor U as Utilisateur
  participant LC as AiLearningComponent
  participant LS as AiLearningService
  participant CTL as AiLearningController
  participant SVC as AiLearningService (Spring)
  participant DB as Base de données

  U->>LC: Clique "Étape terminée"
  LC->>LS: completeStep(sessionId, stepIndex)
  LS->>CTL: PATCH /api/ai/learning/sessions/{id}/steps/{i}/complete
  CTL->>SVC: completeStep(sessionId, stepIndex, userId)
  SVC->>DB: getSession(sessionId)
  SVC->>SVC: Ajoute stepIndex à completedSteps
  alt Toutes les étapes terminées
    SVC->>SVC: status = COMPLETED
  end
  SVC->>DB: save(session)
  SVC-->>CTL: LearningSessionResponse mise à jour
  CTL-->>LS: 200 session
  LS-->>LC: session avec completedSteps mis à jour
  LC->>LC: Mise à jour barre de progression
  LC->>LC: view = 'roadmap'
  LC->>U: Affiche parcours avec progression
```
