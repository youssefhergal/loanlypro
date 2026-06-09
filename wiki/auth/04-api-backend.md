# Auth — API backend

[← Module auth](./README.md)

## Endpoints

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `POST` | `/api/auth/login` | Non | Retourne JWT + utilisateur |
| `POST` | `/api/auth/register` | Non | Crée un compte |

Contrôleur : `AuthController.java` — préfixe `/api/auth`.

## Sécurité HTTP

`SecurityConfig.java` :

- `SessionCreationPolicy.STATELESS`
- CSRF désactivé (API REST + JWT)
- CORS : origine `http://localhost:4200`
- `PasswordEncoder` : BCrypt

## JWT

| Composant | Rôle |
|-----------|------|
| `JwtService` | Création / validation du token |
| `JwtAuthenticationFilter` | Filtre avant `UsernamePasswordAuthenticationFilter` |
| `AppUserDetailsService` | Charge l’utilisateur par email |

Le claim principal pour les contrôleurs : `Authentication.getName()` → **email**.

## Lien avec les prêts

Les endpoints `/api/v1/loan-applications/**` utilisent le même JWT. Les **rôles** déterminent la visibilité des dossiers (voir [demande-pret/05-api-backend.md](../demande-pret/05-api-backend.md)).
