# Auth — Frontend

[← Module auth](./README.md)

## Structure `core/auth/`

| Fichier | Rôle |
|---------|------|
| `services/auth.service.ts` | login, register, logout, token, `hasRole()` |
| `interceptors/auth.interceptor.ts` | Header `Authorization: Bearer` |
| `guards/auth.guard.ts` | Bloque si non connecté |
| `guards/role.guard.ts` | Vérifie `data.roles` sur la route |
| `guards/client-area.guard.ts` | Réserve le shell client aux `ROLE_CLIENT` |
| `constants/auth.constants.ts` | `ROLES`, clés localStorage |

## Stockage navigateur

| Clé | Contenu |
|-----|---------|
| `auth_token` | JWT |
| `auth_user` | JSON `User` (id, email, roles, …) |

## Routes (`app.routes.ts`)

| Route | Guard | Rôle |
|-------|-------|------|
| `/login` | — | Public |
| `/register` | — | Public |
| `/verify-email` | — | Public |
| `''` (shell client) | `authGuard` + `clientAreaGuard` | Client |
| `/conseiller` | `authGuard` + `roleGuard` | `ROLE_CONSEILLER` |
| `/admin` | `authGuard` + `roleGuard` | `ROLE_ADMIN` |

## Exemple — protection d’une route

```typescript
canActivate: [authGuard, roleGuard],
data: { roles: [ROLES.CONSEILLER] },
```

## Lien module suivant

Une fois connecté → [demande-pret/05-frontend.md](../demande-pret/05-frontend.md).
