# Sprint 6 — Module Documents : comment appliquer

Ce zip se dézippe **à la racine de ton projet** (`projet-fil-rouge/`) sur ta branche
`feature/module-documents`. Il écrase les fichiers squelette par leur version
implémentée et ajoute les nouveaux fichiers + le wiki.

## Appliquer

```bash
# depuis le dossier qui CONTIENT projet-fil-rouge/, ou directement dans projet-fil-rouge/
cd projet-fil-rouge
git checkout feature/module-documents

unzip -o sprint6-module-documents.zip      # -o = écrase sans demander

# (optionnel) supprimer l'ancien stub désormais inutile
git rm -f backend/src/main/java/com/projetfilrouge/loanmanagement/service/documents/StubLoanDocumentPdfGenerator.java 2>/dev/null || true
```

> Le stub `StubLoanDocumentPdfGenerator` a été **neutralisé** (plus de `@Component`),
> donc même si tu ne le supprimes pas, il n'y a pas de conflit de bean. Le supprimer
> est juste plus propre.

## Lancer en local (avec données démo)

Le seed démo (US-6.4) et le prêt `LF-DEMO-0001` ne s'activent qu'avec le profil `demo`.

```bash
docker compose up -d db

cd backend
SPRING_PROFILES_ACTIVE=dev,demo ./mvnw spring-boot:run
# (PowerShell : $env:SPRING_PROFILES_ACTIVE="dev,demo"; .\mvnw.cmd spring-boot:run)

# autre terminal
cd frontend && npm ci && npm start
```

Connexion : `client@test.com` / `password` → menu **Documents**.

## Tester

```bash
cd backend && ./mvnw test     # tests unitaires (profil test = H2, pas de MySQL requis)
```

## Faut-il toucher à Google Storage ?

Non. En local le profil `dev` utilise `STORAGE_TYPE=local` (dossier `uploads/`). Le
téléchargement des justificatifs réutilise l'abstraction `LoanDocumentStorageBackend`
existante (local OU GCS, sans changement de code). Les PDF crédit / échéancier /
prélèvements sont générés **en mémoire**, sans aucun stockage. GCS ne concerne que le
déploiement cloud, pas ce sprint.
