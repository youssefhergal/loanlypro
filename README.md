# Projet Fil Rouge

Application de gestion des prêts (demandes, suivi, remboursements) — client et conseiller.

## Documentation wiki (modulaire)

Wiki par **dossiers** (`auth/`, `demande-pret-client/`, …) : [`wiki/README.md`](wiki/README.md).

Audit pré-livraison (parcours client) : [`wiki/demande-pret-client/08-audit-pre-livraison.md`](wiki/demande-pret-client/08-audit-pre-livraison.md).

## Comptes et données de démonstration

Le profil **`dev` seul** ne crée que les rôles. Pour un jeu de données complet, utilisez le **seed bulk** (voir ci-dessous).

### Jeu de données bulk (staging / prod / local)

Pour charger **25 clients**, **4 conseillers**, **2 admins**, demandes variées, prêts, justificatifs réels et historique :

1. Les fichiers justificatifs doivent être dans `backend/src/main/resources/seed/documents/` (5 fichiers PDF/PNG).
2. Au démarrage du backend, activer le profil `seed` et le flag one-shot :

```bash
# Local (après docker compose up -d db)
cd backend
set SPRING_PROFILES_ACTIVE=dev,seed
set SEED_BULK_ENABLED=true
.\mvnw.cmd spring-boot:run
```

```bash
# Production (Cloud Run) — une seule fois après wipe BDD + bucket GCS
SPRING_PROFILES_ACTIVE=prod,seed
SEED_BULK_ENABLED=true
```

3. **Remettre `SEED_BULK_ENABLED=false`** (ou retirer le profil `seed`) après le premier démarrage réussi. Le seeder est idempotent : il skip si `client.seed.01@loanlypro.fr` existe déjà.

| Rôle | Emails | Mot de passe |
|------|--------|--------------|
| Client | `client.seed.01@loanlypro.fr` … `client.seed.25@loanlypro.fr` | `LoanlyPro2026!` (ou `SEED_BULK_PASSWORD`) |
| Conseiller | `conseiller.seed.01@loanlypro.fr` … `conseiller.seed.04@loanlypro.fr` | idem |
| Admin | `admin.seed.01@loanlypro.fr`, `admin.seed.02@loanlypro.fr` | idem |

Références dossiers : `LF-SEED-C01-A01`, etc.

### Réinitialiser les données (avant un nouveau seed)

**Local :**

```powershell
docker compose down -v
docker compose up -d db
Remove-Item -Recurse -Force backend\uploads\loan-documents -ErrorAction SilentlyContinue
```

**Staging / prod :** vider la base Cloud SQL + le bucket GCS (`loan-management-docs-dev` ou `-prod`), puis relancer le seed one-shot (voir commandes `gcloud` dans la doc équipe).

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

**Redémarrage propre du backend (après gros changements Java)**

Si vous voyez des erreurs du type `ClassNotFoundException`, `NoClassDefFoundError` ou un comportement incohérent après modification du code :

1. Arrêter le serveur Spring Boot (`Ctrl+C` dans le terminal).
2. Recompiler proprement :

```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

Sous Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Évitez de relancer `spring-boot:run` sans `clean compile` après des refactors importants : des classes compilées obsolètes peuvent rester dans `target/`.

**Erreur « insufficient memory » / metaspace au démarrage**

Fermez les autres processus Java (anciens `spring-boot:run`, IDE qui compile en parallèle). Le projet configure déjà Maven via `backend/.mvn/jvm.config` (heap + metaspace). Si le problème persiste :

```powershell
cd backend
$env:MAVEN_OPTS="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=384m"
.\mvnw.cmd spring-boot:run
```

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
