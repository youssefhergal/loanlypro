# Guide — Nouveau module wiki

[← Module projet](./README.md)

## Structure recommandée (ex. `demande-pret/`)

```
wiki/mon-module/
├── README.md           # Sommaire + liens (obligatoire)
├── 01-vue-ensemble.md    # Périmètre, objectifs
├── 02-cas-utilisation.md # UC + diagramme
├── 03-sequences.md       # Diagrammes de séquence
├── 05-api-backend.md     # Endpoints REST (si applicable)
└── 05-frontend.md        # Routes, composants (si applicable)
```

Pas de `04-` imposé. Le **diagramme de classes** reste dans [diagrams/classes-domaine.puml](./diagrams/classes-domaine.puml) ou un fichier dédié sous `projet/diagrams/` si le module touche le domaine global.

## Référence

Module complet documenté : [demande-pret/README.md](../demande-pret/README.md).

## Checklist rédaction

- [ ] README : sommaire numéroté + chemins code + comptes test
- [ ] 01 : qui fait quoi, statuts, hors périmètre
- [ ] 02 : matrice rôles × actions
- [ ] 03 : au moins un flux par acteur principal
- [ ] 05-api : tableau endpoints + services backend
- [ ] 05-front : routes par shell / rôle
