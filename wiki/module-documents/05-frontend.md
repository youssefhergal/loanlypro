# Documents — Frontend

[← Module Documents](./README.md)

Route `/documents` → `DocumentsComponent` (`features/documents/`), shell client.

## Structure

```
core/documents/
  models/        justificatif.model.ts, credit-document.model.ts, document.enums.ts
  services/      documents-api.service.ts   (appels /api/v1/documents)
features/documents/
  documents.component.*        mat-tab-group (3 onglets)
  tabs/
    justificatifs-tab.*        onglet « Mes justificatifs »
    credit-documents-tab.*     onglet « Mon crédit »
    repayment-exports-tab.*    onglet « Échéancier & prélèvements »
```

## Comportement

- Chaque onglet gère ses états : `loading` (spinner Material), vide (empty state), erreur (snackbar).
- Téléchargement via `responseType: 'blob'` puis création d'un lien `<a download>`.
- L'onglet exports réutilise `RepaymentApiService.getMyLoans()` pour le sélecteur de prêt.
- Documents crédit non disponibles : carte grisée + motif renvoyé par l'API.

## États & UX

| Onglet | Vide | Action |
|--------|------|--------|
| Justificatifs | « Aucun justificatif déposé » | bouton Télécharger par pièce |
| Mon crédit | « Aucun document crédit » | bouton Télécharger (désactivé si indisponible) |
| Échéancier | « Aucun crédit actif » | sélecteur de prêt + 2 exports PDF |
