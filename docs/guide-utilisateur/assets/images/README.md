# Captures d'écran — guide utilisateur

Organisez les images par rôle :

```
assets/images/
├── commun/          # landing, login, register, verify-email
├── client/          # dashboard, wizard, demandes, prêts, mandat…
├── conseiller/      # dashboard, dossiers, instruction…
└── admin/           # dashboard, demandes, utilisateurs…
```

## Conventions de nommage

| Format | Exemple |
|--------|---------|
| `{numéro}-{écran}.png` | `01-landing.png` |
| `{numéro}-{action}.png` | `04-wizard-documents.png` |

- **Format** : PNG ou WebP, largeur 1280–1440 px
- **Navigateur** : fenêtre propre, sans barre d'outils de dev
- **Données** : comptes seed (`client.seed.01@…`) ou données anonymisées
- **Mode sombre** : choisir un thème et rester cohérent dans tout le guide

Voir [CHECKLIST-CAPTURES.md](../../CHECKLIST-CAPTURES.md) pour la liste complète.
