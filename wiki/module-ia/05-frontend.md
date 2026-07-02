# IA — Frontend

[← Module IA](./README.md)

## Structure

```
frontend/src/app/
├── core/
│   ├── ai-advisor/
│   │   ├── models/ai-chat.model.ts       # AiMessage { role, content }
│   │   └── services/ai-advisor.service.ts # POST /api/ai/chat
│   └── ai-learning/
│       ├── models/learning.model.ts       # LearningSession, LearningStep, SessionStatus
│       └── services/ai-learning.service.ts # Tous les appels /api/ai/learning/*
└── features/
    ├── ai-advisor/
    │   ├── ai-advisor.component.ts        # Chat conversationnel
    │   ├── ai-advisor.component.html
    │   └── ai-advisor.component.scss
    └── ai-learning/
        ├── ai-learning.component.ts       # 3 états : goal-input | roadmap | step-detail
        ├── ai-learning.component.html
        └── ai-learning.component.scss
```

## Conseiller IA — Composant

**États gérés via signals Angular :**

| Signal | Type | Rôle |
|--------|------|------|
| `messages` | `AiMessage[]` | Historique du chat |
| `isTyping` | `boolean` | Affiche le loader "..." |
| `messageCtrl` | `FormControl` | Textarea de saisie |

L'historique complet est envoyé à chaque requête — Claude reçoit tout le contexte.

Le `FormControl` est désactivé (`messageCtrl.disable()`) pendant l'appel API et réactivé à la réponse.

## Formation sur mesure — Composant

**3 vues pilotées par `view = signal<ViewState>('goal-input')` :**

```
goal-input  →  roadmap  →  step-detail
     ↑_____________↑____________↑
         (backToGoal / backToRoadmap)
```

**Flux utilisateur :**

1. `goal-input` : saisie de l'objectif + suggestions prédéfinies → `generateRoadmap()`
2. `roadmap` : liste des étapes avec progression → `openStep(index)`
3. `step-detail` : contenu markdown de l'étape → `completeStep()`

**Reprise de session :** les sessions passées sont chargées au `ngOnInit()` via `getSessions()` et affichées sur la vue `goal-input` → `resumeSession(session)`.

## Rendu Markdown

Le contenu généré par Claude est en Markdown. Il est converti en HTML via `mdToHtml()`, une fonction inline sans dépendance externe :

```typescript
formatContent(content: string): string {
  return mdToHtml(content);
}
// Template
[innerHTML]="formatContent(stepContent()!)"
```

`mdToHtml()` gère : titres (`#` → `<h1>`…`<h4>`), gras, italique, listes, blocs de code, citations, `<hr>`, paragraphes.

Les styles sont appliqués via `::ng-deep` dans le SCSS du composant.

## Routes

```typescript
// Shell client (et conseiller, admin)
{ path: 'conseiller-ia', loadComponent: () => import('./features/ai-advisor/…') }
{ path: 'formation',     loadComponent: () => import('./features/ai-learning/…') }
```

Navigation disponible dans les 3 shells (client, conseiller, admin) via l'icône `psychology` (conseiller IA) et `school` (formation).
