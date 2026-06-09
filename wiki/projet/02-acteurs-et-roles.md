# Acteurs & rôles

[← Module projet](./README.md) · [← Wiki](../README.md)

## Rôles applicatifs

| Rôle | Constante | Espace UI |
|------|-----------|-----------|
| Client | `ROLE_CLIENT` | Shell client (`/dashboard`, `/mes-demandes`, …) |
| Conseiller | `ROLE_CONSEILLER` | `/conseiller` (placeholder) + API |
| Administrateur | `ROLE_ADMIN` | `/admin` (placeholder) + API |

Définition frontend : `frontend/src/app/core/auth/constants/auth.constants.ts`

## Matrice haute niveau

| Capacité | Client | Conseiller | Admin |
|----------|:------:|:----------:|:-----:|
| S’authentifier | ✅ | ✅ | ✅ |
| Créer / soumettre une demande | ✅ | ❌ | ❌ |
| Voir ses dossiers | ✅ | assignés | tous |
| Approuver / rejeter | ❌ | ✅ | ❌ |

Cas d’utilisation détaillés :

- Demande de prêt → [demande-pret/README.md](../demande-pret/README.md)
- Auth → [auth/02-cas-utilisation.md](../auth/02-cas-utilisation.md)
