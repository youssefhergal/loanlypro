# Checklist tests manuels — Wizard prêt

## Prérequis

- Backend sur `http://localhost:8080` (MySQL démarré)
- Frontend : `npm start` dans `frontend/`
- Compte client `ROLE_CLIENT` connecté

## Parcours principal

- [ ] Créer une demande (étapes 1→4), soumettre → redirection `/mes-demandes?submitted=LOAN-…`
- [ ] « Enregistrer et quitter » après étape 1 → brouillon visible dans la liste
- [ ] « Continuer » sur un brouillon → reprise avec données préremplies
- [ ] Refresh F5 sur étape 2 → données rechargées via API

## Documents

- [ ] Upload PDF < 10 Mo → OK
- [ ] Upload > 10 Mo → message d'erreur
- [ ] Upload .docx → rejeté côté front
- [ ] Submit sans les 5 types obligatoires → erreur API `BUSINESS_RULE`

## Validation

- [ ] Submit sans cocher CGU / certification → bloqué côté front
- [ ] Montant < 1000 € → erreur validation API ou front

## Auth

- [ ] Token expiré / logout → 401 → redirection login

## Simulation

- [ ] Mensualité change quand montant/durée changent (taux 3,85 %)
