# 005 — Cloud Deployment

## Overview

Containerize the three Spring Boot apps (claims-app, insurance-request-app, insurance-response-app) and deploy them to GCP Cloud Run with a GitHub Actions CI/CD pipeline. Auto-deploy to dev on merge to main; manually promote to prod on demand.

## Architecture

```
GitHub (push to main)
  └─> GitHub Actions CI Workflow
        ├── test (./gradlew test)
        ├── detect-changes (git diff path-based)
        ├── build-and-push (matrix over affected apps)
        │     └─> Google Artifact Registry
        └── deploy-dev (matrix, Cloud Run)

GitHub (workflow_dispatch)
  └─> GitHub Actions CI Workflow
        ├── test
        ├── build-and-push (selected apps)
        └── deploy-prod (matrix, Cloud Run, approval gate)
```

## Dockerfiles

Each app gets an identical multi-stage Dockerfile at its module root (`claims-app/Dockerfile`, etc.).

**Stage 1 — Build:**
- Base: `eclipse-temurin:21-jdk-alpine`
- Copies full Gradle project
- Runs `./gradlew :APP:bootJar`

**Stage 2 — Runtime:**
- Base: `eclipse-temurin:21-jre-alpine` (~180MB)
- Copies only the fat JAR from stage 1
- Runs as non-root user
- Exposes port 8080

All apps use port 8080 inside the container. Cloud Run expects this by default and routes to each service independently. The existing 8080/8081/8082 split remains for local development only.

## Spring Profiles & Environment Config

### New file: `application-cloud.yml` (per app)

```yaml
server:
  port: ${PORT:8080}

spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
```

- **Local dev:** No profile active. Uses existing `application.yml` with `mongodb://localhost:27017/edi_healthcare`. No changes.
- **Cloud Run:** Sets `SPRING_PROFILES_ACTIVE=cloud` and `MONGODB_URI` (Atlas connection string from GCP Secret Manager).

## GitHub Actions Workflow

Single file: `.github/workflows/ci.yml`

### Triggers

- `push` to `main` — auto-build and deploy to dev
- `workflow_dispatch` — manual deploy to prod

### `workflow_dispatch` Inputs

| Input | Type | Options |
|-------|------|---------|
| `app` | choice (required) | `claims-app`, `insurance-request-app`, `insurance-response-app`, `all` |
| `environment` | choice (required) | `prod` |

### Jobs

#### Job 1: `test`
- Runs on every trigger
- Starts MongoDB via `docker-compose up -d`
- Runs `./gradlew test`
- Pipeline stops if tests fail

#### Job 2: `detect-changes`
- Runs in parallel with `test`
- On `push`: `git diff` against previous commit to determine affected app directories
- On `workflow_dispatch`: uses the manually selected app(s)
- Outputs a JSON array (e.g., `["claims-app", "insurance-response-app"]`)

#### Job 3: `build-and-push` (matrix)
- Depends on `test` (passing) and `detect-changes`
- Matrix over affected apps
- Authenticates to GCP via Workload Identity Federation
- Builds Docker image, tags with commit SHA + environment-specific latest tag
- Pushes to Artifact Registry

#### Job 4: `deploy-dev`
- Runs only on `push` to `main`
- Depends on `build-and-push`
- Matrix over affected apps
- Deploys to Cloud Run dev services

#### Job 5: `deploy-prod`
- Runs only on `workflow_dispatch`
- Depends on `build-and-push`
- Matrix over selected apps
- GitHub Environment `prod` with protection rules (approval gate)
- Deploys to Cloud Run prod services

### Change Detection Rules

| Path changed | Apps rebuilt |
|---|---|
| `claims-app/**` | claims-app |
| `insurance-request-app/**` | insurance-request-app |
| `insurance-response-app/**` | insurance-response-app |
| `common/**` | all three |
| `build.gradle` | all three |
| `settings.gradle` | all three |
| `gradle/**` | all three |
| `gradle.properties` | all three |
| `.github/workflows/**` | all three |
| Everything else (specs, docs, README, .claude) | none — tests run, build skipped |

If only non-app files changed, `detect-changes` outputs an empty array and `build-and-push` is skipped entirely.

## Docker Image Tagging

| Tag | Description |
|---|---|
| `:<git-sha>` | Immutable. Every build gets one. |
| `:latest-dev` | Mutable. Updated on each dev deploy. |
| `:latest-prod` | Mutable. Updated on each prod deploy. |

Artifact Registry lifecycle policy deletes images older than 90 days that are not tagged `latest-dev` or `latest-prod`.

## GCP Infrastructure

### Artifact Registry
- Repository: `edi-healthcare` (Docker format)
- Three images: `claims-app`, `insurance-request-app`, `insurance-response-app`
- Region: same as Cloud Run

### Cloud Run (per environment)
- Six services total: `claims-app-dev`, `claims-app-prod`, etc.
- Resources: 256MB–512MB memory, 1 vCPU
- Scaling: min 0 (scale to zero), max 3 instances
- Environment variables:
  - `SPRING_PROFILES_ACTIVE=cloud`
  - `MONGODB_URI` (from Secret Manager)

### GCP Secret Manager
- `mongodb-uri-dev` — Atlas connection string for dev
- `mongodb-uri-prod` — Atlas connection string for prod
- Cloud Run service accounts granted `secretmanager.secretAccessor`

### Workload Identity Federation
- GitHub Actions authenticates to GCP without service account keys
- One WIF pool + provider per GCP project, scoped to this GitHub repository

## MongoDB Atlas

- Two databases: `edi-healthcare-dev`, `edi-healthcare-prod`
- Network access: Cloud Run egress IPs allowlisted
- Connection strings stored in GCP Secret Manager (not in code or workflow files)

## Files Created/Modified

### New files
- `claims-app/Dockerfile`
- `insurance-request-app/Dockerfile`
- `insurance-response-app/Dockerfile`
- `claims-app/src/main/resources/application-cloud.yml`
- `insurance-request-app/src/main/resources/application-cloud.yml`
- `insurance-response-app/src/main/resources/application-cloud.yml`
- `.github/workflows/ci.yml`
- `.dockerignore`

### Existing files unchanged
- All `application.yml` files remain as-is (local dev unaffected)
- `docker-compose.yml` remains as-is (local MongoDB)
- All Gradle build files remain as-is
