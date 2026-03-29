# Plan: Restructure Monorepo with Backend + Frontend

## Context

The project is a Gradle multi-module Spring Boot monorepo with four modules (`common`, `claims-app`, `insurance-request-app`, `insurance-response-app`) all at the repo root. The goal is to reorganize into `backend/` and `frontend/` top-level directories, moving all Java modules under `backend/` and creating a new Next.js frontend with a BFF (Backend-For-Frontend) API layer that proxies to the three backend services.

## Decisions

- **Monorepo**: Single repo with `backend/` and `frontend/` directories
- **Shared files at root**: `gradlew`, `gradle/`, `settings.gradle`, `build.gradle`, `docker-compose.yml` stay at root
- **Gradle module names preserved**: Use `project(':common').projectDir = file('backend/common')` so all existing `:common`, `:claims-app` references work unchanged
- **BFF pattern**: Next.js API routes proxy to backend services (no direct browser-to-backend calls)
- **Frontend scope**: Full UI for all three services (claims, eligibility request, eligibility response)

---

## Phase 1: Move Backend Modules

### 1.1 Move directories

```bash
mkdir backend
git mv common/ backend/common/
git mv claims-app/ backend/claims-app/
git mv insurance-request-app/ backend/insurance-request-app/
git mv insurance-response-app/ backend/insurance-response-app/
```

### 1.2 Update `settings.gradle`

```groovy
rootProject.name = 'edi-healthcare'

include 'common'
include 'claims-app'
include 'insurance-request-app'
include 'insurance-response-app'

project(':common').projectDir = file('backend/common')
project(':claims-app').projectDir = file('backend/claims-app')
project(':insurance-request-app').projectDir = file('backend/insurance-request-app')
project(':insurance-response-app').projectDir = file('backend/insurance-response-app')
```

No changes needed to any module's `build.gradle` — all `project(':common')` references continue to work.

### 1.3 Update Dockerfiles (all three apps)

All `COPY` paths need `backend/` prefix. Example for `backend/claims-app/Dockerfile`:

```dockerfile
COPY backend/common/build.gradle backend/common/build.gradle
COPY backend/claims-app/build.gradle backend/claims-app/build.gradle
COPY backend/insurance-request-app/build.gradle backend/insurance-request-app/build.gradle
COPY backend/insurance-response-app/build.gradle backend/insurance-response-app/build.gradle

COPY backend/common/src backend/common/src
COPY backend/claims-app/src backend/claims-app/src

RUN ./gradlew :claims-app:bootJar --no-daemon -x test

COPY --from=build /workspace/backend/claims-app/build/libs/*.jar app.jar
```

Same pattern for `insurance-request-app/Dockerfile` and `insurance-response-app/Dockerfile`.

### 1.4 Update `.dockerignore`

Add `backend/*/build/` and frontend entries:

```
backend/*/build/
frontend/node_modules/
frontend/.next/
```

### 1.5 Update CI workflow (`.github/workflows/ci.yml`)

- **Change detection** (line 110): `^common/` → `^backend/common/`, `^claims-app/` → `^backend/claims-app/`, etc.
- **Docker build** (line 171): `-f ${{ matrix.app }}/Dockerfile` → `-f backend/${{ matrix.app }}/Dockerfile`
- **workflow_dispatch options**: Add `frontend` option

### 1.6 Verify backend build

```bash
./gradlew clean build
./gradlew test
```

**Files modified:**
- `settings.gradle`
- `backend/claims-app/Dockerfile`
- `backend/insurance-request-app/Dockerfile`
- `backend/insurance-response-app/Dockerfile`
- `.dockerignore`
- `.github/workflows/ci.yml`

---

## Phase 2: Create Next.js Frontend

### 2.1 Initialize project

```bash
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir --import-alias "@/*"
cd frontend && npx shadcn@latest init
```

### 2.2 Directory structure

```
frontend/
  src/
    app/
      layout.tsx                         # Root layout with navigation
      page.tsx                           # Dashboard with links to all 3 workflows
      claims/
        page.tsx                         # Claims generation form + EDI download
      eligibility-request/
        page.tsx                         # Eligibility inquiry form + EDI download
      eligibility-response/
        page.tsx                         # File upload + parsed response display
      api/
        claims/generate/route.ts         # BFF → localhost:8080
        insurance/
          eligibility-request/route.ts   # BFF → localhost:8081
          eligibility-response/route.ts  # BFF → localhost:8082
        dev/seed/route.ts                # BFF → localhost:8080/api/dev/seed
    lib/
      api-client.ts                      # Shared fetch helpers
      constants.ts                       # Backend URLs from env
    components/
      ui/                                # shadcn/ui components
      layout/
        sidebar.tsx
        header.tsx
      claims/
        claims-form.tsx
        edi-preview.tsx
      eligibility/
        request-form.tsx
        response-upload.tsx
        response-viewer.tsx
    types/
      index.ts                           # TS interfaces matching backend DTOs
  .env.local
  next.config.ts
```

### 2.3 BFF API routes

Each route proxies to the corresponding backend service. Environment variables (server-side only, no `NEXT_PUBLIC_` prefix):

```
CLAIMS_API_URL=http://localhost:8080
REQUEST_API_URL=http://localhost:8081
RESPONSE_API_URL=http://localhost:8082
```

Key patterns:
- Claims + eligibility-request routes: Forward JSON body, return file download (EDI)
- Eligibility-response route: Forward multipart FormData, return parsed JSON
- Dev seed route: Simple POST proxy, return JSON

### 2.4 Page implementations

- **Dashboard**: Cards linking to each workflow + "Seed Database" button
- **Claims**: Multi-select encounters → POST → download `.edi` file + optional preview
- **Eligibility Request**: Multi-select patients → POST → download `.edi` file
- **Eligibility Response**: Drag-and-drop `.edi` file upload → display parsed response (payer info, subscriber, benefits table)

### 2.5 TypeScript types

Mirror backend DTOs: `ClaimsRequest`, `InsuranceRequestDTO`, `EligibilityResponse`, `BenefitDetail`, `SeedResult`

---

## Phase 3: Update Root Config Files

### 3.1 `.gitignore` — append frontend entries

```
### Frontend (Next.js) ###
frontend/node_modules/
frontend/.next/
frontend/out/
frontend/.env*.local
!frontend/.env.example
```

### 3.2 `docker-compose.yml` — add optional frontend service

```yaml
frontend:
  build: ./frontend
  container_name: edi-frontend
  ports:
    - "3000:3000"
  environment:
    - CLAIMS_API_URL=http://host.docker.internal:8080
    - REQUEST_API_URL=http://host.docker.internal:8081
    - RESPONSE_API_URL=http://host.docker.internal:8082
```

### 3.3 Add frontend CI job to `.github/workflows/ci.yml`

```yaml
test-frontend:
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: frontend
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json
    - run: npm ci
    - run: npm run lint
    - run: npm run build
```

### 3.4 Update `CLAUDE.md` and `README.md`

Add frontend commands, full-stack local dev instructions, updated architecture diagram.

---

## Verification

1. `./gradlew clean build` — all backend modules compile successfully
2. `./gradlew test` — all existing tests pass
3. `./gradlew :claims-app:bootRun` — module names work without `:backend:` prefix
4. `cd frontend && npm run dev` — frontend starts on port 3000
5. Start all 3 backend services + seed data, then test each frontend page:
   - Dashboard seed button works
   - Claims form generates and downloads EDI file
   - Eligibility request generates and downloads EDI file
   - Eligibility response parses uploaded EDI and displays results
6. `docker build -f backend/claims-app/Dockerfile .` — Docker builds work from root
