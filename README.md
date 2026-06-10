# Projet Fil Rouge

Application de gestion des prêts (demandes, suivi, remboursements) — client et conseiller.

## Documentation wiki (modulaire)

Wiki par **dossiers** (`auth/`, `demande-pret-client/`, …) : [`wiki/README.md`](wiki/README.md).

Audit pré-livraison (parcours client) : [`wiki/demande-pret-client/08-audit-pre-livraison.md`](wiki/demande-pret-client/08-audit-pre-livraison.md).

## Comptes de démonstration

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| Client | `client@test.com` | `password` |
| Conseiller | `conseiller@test.com` | `password` |
| Admin | `admin@test.com` | `password` |

Les demandes de prêt ne sont pas pré-remplies : créez-les via l'application (wizard client).

## Comment lancer le projet (BDD, backend, frontend)

**Prérequis :** Docker, Java 17, Node 20.

**Aligner les versions (équipe) :** Backend → Java 17 (fichier `backend/.java-version`), toujours utiliser `./mvnw` dans `backend/`. Frontend → Node 20 (fichier `frontend/.nvmrc`, faire `nvm use` ou `fnm use` dans `frontend/`), puis `npm ci` (pas `npm install`) pour respecter `package-lock.json`.

---

### Option 1 : Dev en local (recommandé en phase de développement)

BDD dans Docker, backend et frontend lancés sur ta machine (hot reload, debug facile).

**1. Lancer la base de données**

À la racine du projet :

```bash
docker compose up -d db
```

**2. Lancer le backend**

```bash
cd backend
./mvnw spring-boot:run
```

(Utiliser `mvnw.cmd` sur Windows si besoin.)

**3. Lancer le frontend**

Dans un autre terminal :

```bash
cd frontend
npm ci
npm start
```

**4. Accéder à l'application**

| Service   | URL                    |
|-----------|------------------------|
| Frontend  | http://localhost:4200  |
| Backend   | http://localhost:8080  |
| MySQL     | localhost:3306 (user `app` / mot de passe `app`, base `loan_management`) |

---

### Option 2 : Tout en Docker (démo, onboarding)

Tout tourne dans des conteneurs (BDD + backend + frontend). Même environnement pour toute l'équipe sans installer Java ni Node.

À la racine du projet :

```bash
docker compose up -d
```

Au premier lancement, les images backend et frontend sont construites (quelques minutes). Ensuite :

| Service   | URL                    |
|-----------|------------------------|
| Frontend  | http://localhost:4200  |
| Backend   | http://localhost:8080  |
| MySQL     | localhost:3306         |

**Commandes utiles :**

```bash
# Reconstruire après modification du code
docker compose up -d --build

# Voir les logs
docker compose logs -f

# Arrêter tout
docker compose down
```

---

## CI/CD & Environnements

| Environnement | Frontend | Backend | Déclencheur |
|---|---|---|---|
| **Dev** | https://pfr-dev.web.app | Cloud Run `backend-dev` | push `develop` |
| **Prod** | https://pfr-prod.web.app | Cloud Run `backend-prod` | push `main` |

Pipeline GitLab CI/CD : `test → build → deploy` (automatique sur `develop` et `main`).

Documentation complète : [`docs/CICD.md`](docs/CICD.md)
