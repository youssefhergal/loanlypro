# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Loan management application (LoanlyFans) — client submits loan requests, assigned conseiller (advisor) instructs them, admin supervises. Three roles: `ROLE_CLIENT`, `ROLE_CONSEILLER`, `ROLE_ADMIN`.

---

## Commands

### Running locally (recommended for dev)

```bash
# 1. Start DB only
docker compose up -d db

# 2. Backend (from backend/)
./mvnw spring-boot:run

# 3. Frontend (from frontend/)
npm ci && npm start
```

URLs: Frontend → http://localhost:4200 | Backend → http://localhost:8080 | MySQL → localhost:3306 (`app`/`app`, db `loan_management`)

Demo accounts: `client@test.com`, `conseiller@test.com`, `admin@test.com` — password `password` for all.

### Full Docker (demo / onboarding)

```bash
docker compose up -d           # first run builds images
docker compose up -d --build   # rebuild after code changes
docker compose logs -f
docker compose down
```

### Backend

```bash
cd backend
./mvnw spring-boot:run                         # run
./mvnw clean compile && ./mvnw spring-boot:run # clean restart after large refactors
./mvnw test                                    # all tests
./mvnw test -Dtest=AuthServiceTest             # single test class
./mvnw verify -Pcoverage                       # tests + JaCoCo coverage
```

### Frontend

```bash
cd frontend
npm start          # dev server on :4200, proxies /api → :8080
npm test           # Karma/Jasmine (headless)
npm run build      # production build
```

---

## Architecture

### Backend (`backend/src/main/java/com/projetfilrouge/loanmanagement/`)

Standard layered Spring Boot 3.5 app (Java 17):

- **`web/controller/`** — REST controllers, no business logic; delegates to services
- **`web/dto/request/` & `response/`** — API contracts separated from entities; use JSR-380 validation annotations
- **`service/`** — All business logic; `@Transactional` boundaries live here
- **`repository/`** — Spring Data JPA; `LoanApplicationRepository` uses `Specification` for dynamic filtering
- **`entity/`** — JPA entities; `User` has `@ManyToMany` to `Role` with `CascadeType.PERSIST,MERGE`
- **`config/`** — `SecurityConfig`, `EmailSenderConfig`, `InitialDataLoader` (dev seed), schedulers
- **`security/`** — `JwtService` (JJWT 0.12.6), `JwtAuthenticationFilter`
- **`notification/`** — `EmailSender` interface; `LoggingEmailSender` (dev) or `ResendEmailSender` (prod) chosen via `app.mail.provider`
- **`payment/`** — Payment provider abstraction (fake by default)
- **`storage/`** — File storage abstraction (local or GCS)
- **`web/exception/`** — `GlobalExceptionHandler` handles all exceptions centrally; **always log** in the catch-all `handleRuntime`

**Key config pattern:** `application.yml` sets base defaults; `application-dev.yml` and `application-prod.yml` override. Active profile: `${SPRING_PROFILES_ACTIVE:dev}`.

**Dev shortcuts:** email verification code is always `000000` (set in `application-dev.yml`). Email is logged to console, not sent.

**Entity gotcha:** never use `Set.of(...)` (immutable) for `@ManyToMany` collections — Hibernate calls `.clear()` during merge. Always use `new HashSet<>(...)`.

### Frontend (`frontend/src/app/`)

Angular 19, standalone components, Angular Material, signals API.

**Structure:**
- **`core/`** — Singleton services, models, guards, interceptors; never import from `features/` here
  - `core/auth/` — `AuthService` (signals + localStorage), `authGuard`, `roleGuard`, `clientAreaGuard`, `authInterceptor` (attaches Bearer token, triggers logout on 401)
  - `core/loans/`, `core/notifications/`, `core/admin/`, `core/profile/` — domain services
- **`features/`** — Lazy-loaded UI components, one folder per feature/role
  - `features/layout/` — Role-specific shells (`admin-shell`, `advisor-shell`, `client-shell`) that wrap child routes with the role's navbar
  - `features/dashboard/` — Per-role dashboards
  - `features/loans/` — Loan CRUD split by role: `applicant/`, `advisor/`, `admin/`
  - `features/loans/repayment/` — Repayment sub-feature, also role-split
- **`shared/`** — Dialog components used across features (approve, reject, confirm, etc.)

**Routing:** `app.routes.ts` — top-level paths are `/login`, `/register`, `/verify-email`, `/conseiller`, `/admin`, `/` (client). Role shells use `canActivate: [authGuard, roleGuard]` with `data: { roles: [...] }`.

**API proxy:** in dev, `proxy.conf.json` maps `/api` → `http://localhost:8080`. In prod, `environment.apiUrl = '/api'` is kept and nginx handles it.

**Colors (CSS variables in `styles.scss`):**
```
--color-primary: #1a7a4a
--color-primary-light: #e8f5ee
--color-primary-dark: #0f5431
--color-accent: #5db885
--color-bg: #f7faf8
--color-surface: #ffffff
--color-text: #1a2e22
--color-text-muted: #6b7c72
--color-border: #d8e8de
--color-error: #d32f2f
```
Font: `DM Sans`. Material theme: `mat.$green-palette`, light mode.

---

## CI/CD

GitLab CI pipeline: `test → build → deploy`.

| Branch | Frontend | Backend |
|---|---|---|
| `develop` | https://pfr-dev.web.app | Cloud Run `backend-dev` |
| `main` | https://pfr-prod.web.app | Cloud Run `backend-prod` |

SonarQube analysis configured via `backend/sonar-project.properties`.
