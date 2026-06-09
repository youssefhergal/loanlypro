# Auth — Cas d’utilisation

[← Module auth](./README.md)

```mermaid
flowchart TB
  subgraph acteurs [Acteurs]
    V[Visiteur]
    U[Utilisateur connecté]
  end
  subgraph auth [Authentification]
    UC1[S'inscrire]
    UC2[Se connecter]
    UC3[Se déconnecter]
    UC4[Vérifier e-mail]
    UC5[Route protégée]
    UC6[API métier]
  end
  V --> UC1
  V --> UC2
  V --> UC4
  U --> UC3
  U --> UC5
  U --> UC6
  UC5 -.->|requiert| UC2
  UC6 -.->|requiert| UC2
```

| UC | Statut |
|----|--------|
| Inscription | ✅ API + UI `/register` |
| Connexion | ✅ API + UI `/login` |
| Déconnexion | ✅ `AuthService.logout()` |
| Vérification e-mail | ⚠️ UI `/verify-email` — flux backend à compléter selon projet |
| Protection routes | ✅ `authGuard`, `roleGuard`, `clientAreaGuard` |
