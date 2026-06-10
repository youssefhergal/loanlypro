# CI/CD — Projet Fil Rouge

## Architecture

```
GitLab CI/CD
│
├── push develop ──► DEV  (automatique)
│   ├── Frontend → Firebase Hosting  https://pfr-dev.web.app
│   ├── Backend  → Cloud Run         backend-dev
│   └── DB       → Cloud SQL         loan_dev
│
└── push main ────► PROD (automatique)
    ├── Frontend → Firebase Hosting  https://pfr-prod.web.app
    ├── Backend  → Cloud Run         backend-prod
    └── DB       → Cloud SQL         loan_prod
```

## Pipeline

| Stage | Jobs | Déclencheur |
|---|---|---|
| `test` | `test-backend`, `test-frontend` | develop, main, MR |
| `build` | `build-backend` (Docker), `build-frontend` (Angular) | develop, main |
| `deploy-dev` | `deploy-backend-dev`, `deploy-frontend-dev` | develop |
| `deploy-prod` | `deploy-backend-prod`, `deploy-frontend-prod` | main |

## Infrastructure GCP

| Ressource | Nom | Détail |
|---|---|---|
| Projet GCP/Firebase | `projet-fil-rouge-app` | Région : `europe-west1` |
| Artifact Registry | `loan-management` | Images Docker du backend |
| Cloud Run DEV | `backend-dev` | min 0, max 5 instances |
| Cloud Run PROD | `backend-prod` | min 1, max 20 instances |
| Cloud SQL | `loan-management-sql` | MySQL 8.0, db-f1-micro |
| Base DEV | `loan_dev` | Schéma auto-créé par Hibernate |
| Base PROD | `loan_prod` | Schéma validé par Hibernate |
| Firebase Hosting DEV | `pfr-dev` | https://pfr-dev.web.app |
| Firebase Hosting PROD | `pfr-prod` | https://pfr-prod.web.app |
| Cloud Storage DEV | `loan-management-docs-dev` | Documents prêt (upload API) |
| Cloud Storage PROD | `loan-management-docs-prod` | Documents prêt (upload API) |

## Secrets (Google Secret Manager)

| Secret | Usage |
|---|---|
| `db-url-dev` | URL JDBC Cloud SQL dev |
| `db-password-dev` | Mot de passe DB dev |
| `db-url-prod` | URL JDBC Cloud SQL prod |
| `db-password-prod` | Mot de passe DB prod |
| `jwt-secret-prod` | Clé JWT production |

## Variables GitLab CI/CD

À configurer dans **GitLab → Settings → CI/CD → Variables** :

| Variable | Description | Masked |
|---|---|---|
| `GCP_PROJECT_ID` | ID du projet GCP (`projet-fil-rouge-app`) | Non |
| `GCP_SERVICE_ACCOUNT_KEY` | Clé JSON du compte de service (encodée en base64) | Oui |
| `FIREBASE_PROJECT_ID` | ID du projet Firebase (`projet-fil-rouge-app`) | Non |
| `FIREBASE_TOKEN` | Token CI Firebase (`firebase login:ci`) | Oui |

## Connexion Firebase → Cloud Run

Firebase Hosting redirige `/api/**` vers le service Cloud Run correspondant (défini dans `firebase.json`). Cela permet à Angular d'appeler `/api/...` sans CORS puisque tout passe par le même domaine Firebase.

```
pfr-dev.web.app/api/**  →  backend-dev (Cloud Run)
pfr-dev.web.app/**      →  Angular SPA (index.html)
```

## Profils Spring Boot

| Profil | Activé sur | Comportement |
|---|---|---|
| `dev` | Cloud Run dev + local | `ddl-auto: update`, logs SQL actifs |
| `prod` | Cloud Run prod | `ddl-auto: validate`, logs SQL désactivés |
| `test` | Pipeline CI | `ddl-auto: create-drop`, MySQL service GitLab, stockage local |
| Stockage fichiers | Cloud Run dev/prod | `STORAGE_TYPE=gcs`, bucket GCS dédié |

## Ajouter un nouveau secret

```bash
# 1. Créer le secret dans Secret Manager
echo -n "valeur" | gcloud secrets create nom-du-secret \
  --data-file=- --project projet-fil-rouge-app

# 2. Ajouter au deploy Cloud Run dans .gitlab-ci.yml
--set-secrets="MA_VAR=nom-du-secret:latest"
```

## Régénérer la clé du compte de service

```bash
gcloud iam service-accounts keys create /tmp/gitlab-ci-key.json \
  --iam-account=gitlab-ci@projet-fil-rouge-app.iam.gserviceaccount.com \
  --project projet-fil-rouge-app

base64 -i /tmp/gitlab-ci-key.json | tr -d '\n'
# → Coller la valeur dans GitLab → GCP_SERVICE_ACCOUNT_KEY
```

## Désactiver le projet GCP

```bash
gcloud projects delete projet-fil-rouge-app
```
