# Cloud Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Containerize the three Spring Boot apps and deploy them to GCP Cloud Run via a GitHub Actions CI/CD pipeline with auto-deploy to dev and manual promote to prod.

**Architecture:** Multi-stage Dockerfiles produce slim JRE images. A single GitHub Actions workflow detects which apps changed (git diff path-based), builds only affected images, pushes to Artifact Registry, and deploys to Cloud Run. Spring `cloud` profile overrides MongoDB URI to point at Atlas.

**Tech Stack:** Docker (multi-stage, eclipse-temurin:21), GitHub Actions, GCP Cloud Run, Google Artifact Registry, GCP Secret Manager, Workload Identity Federation, MongoDB Atlas, Spring Boot profiles.

---

## File Structure

### New files
| File | Responsibility |
|------|---------------|
| `.dockerignore` | Exclude build artifacts, IDE files, specs from Docker context |
| `claims-app/Dockerfile` | Multi-stage build for claims-app |
| `insurance-request-app/Dockerfile` | Multi-stage build for insurance-request-app |
| `insurance-response-app/Dockerfile` | Multi-stage build for insurance-response-app |
| `claims-app/src/main/resources/application-cloud.yml` | Cloud profile: MongoDB URI from env, port from Cloud Run |
| `insurance-request-app/src/main/resources/application-cloud.yml` | Cloud profile: MongoDB URI from env, port from Cloud Run |
| `insurance-response-app/src/main/resources/application-cloud.yml` | Cloud profile: MongoDB URI from env, port from Cloud Run |
| `.github/workflows/ci.yml` | Single CI/CD workflow: test, detect changes, build, deploy |

### Existing files unchanged
All existing source code, `application.yml` files, `build.gradle` files, and `docker-compose.yml` remain untouched.

---

### Task 1: Create `.dockerignore`

**Files:**
- Create: `.dockerignore`

- [ ] **Step 1: Create the `.dockerignore` file**

```
# Build outputs
build/
*/build/
.gradle/

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/
.settings/
.classpath
.project
out/

# Documentation and specs
specs/
docs/
*.md
!README.md

# Git
.git/
.gitignore

# Claude
.claude/

# Docker
docker-compose.yml
*/Dockerfile
```

- [ ] **Step 2: Verify the file is in the project root**

Run: `cat .dockerignore | head -5`
Expected: First 5 lines of the file shown.

- [ ] **Step 3: Commit**

```bash
git add .dockerignore
git commit -m "chore: add .dockerignore for Docker builds"
```

---

### Task 2: Create Dockerfile for `claims-app`

**Files:**
- Create: `claims-app/Dockerfile`

- [ ] **Step 1: Create the multi-stage Dockerfile**

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Copy module build files
COPY common/build.gradle common/build.gradle
COPY claims-app/build.gradle claims-app/build.gradle
COPY insurance-request-app/build.gradle insurance-request-app/build.gradle
COPY insurance-response-app/build.gradle insurance-response-app/build.gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY common/src common/src
COPY claims-app/src claims-app/src

# Build the boot JAR
RUN ./gradlew :claims-app:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/claims-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Build the Docker image locally to verify**

Run: `docker build -f claims-app/Dockerfile -t claims-app:local .`
Expected: Build completes successfully. Output ends with something like `Successfully tagged claims-app:local`.

- [ ] **Step 3: Run the image to verify it starts**

Run: `docker run --rm -e SPRING_PROFILES_ACTIVE=cloud -e MONGODB_URI=mongodb://host.docker.internal:27017/edi_healthcare -e PORT=8080 -p 9090:8080 claims-app:local`
Expected: Spring Boot starts on port 8080 inside the container. You'll see the Spring banner and `Started ClaimsApplication`. Press Ctrl+C to stop.

Note: This test requires MongoDB running locally (via `docker-compose up -d`). Port 9090 on host maps to 8080 in the container to avoid conflicts.

- [ ] **Step 4: Commit**

```bash
git add claims-app/Dockerfile
git commit -m "feat: add Dockerfile for claims-app"
```

---

### Task 3: Create Dockerfile for `insurance-request-app`

**Files:**
- Create: `insurance-request-app/Dockerfile`

- [ ] **Step 1: Create the multi-stage Dockerfile**

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Copy module build files
COPY common/build.gradle common/build.gradle
COPY claims-app/build.gradle claims-app/build.gradle
COPY insurance-request-app/build.gradle insurance-request-app/build.gradle
COPY insurance-response-app/build.gradle insurance-response-app/build.gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY common/src common/src
COPY insurance-request-app/src insurance-request-app/src

# Build the boot JAR
RUN ./gradlew :insurance-request-app:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/insurance-request-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Build the Docker image locally to verify**

Run: `docker build -f insurance-request-app/Dockerfile -t insurance-request-app:local .`
Expected: Build completes successfully.

- [ ] **Step 3: Run the image to verify it starts**

Run: `docker run --rm -e SPRING_PROFILES_ACTIVE=cloud -e MONGODB_URI=mongodb://host.docker.internal:27017/edi_healthcare -e PORT=8080 -p 9091:8080 insurance-request-app:local`
Expected: Spring Boot starts. Press Ctrl+C to stop.

- [ ] **Step 4: Commit**

```bash
git add insurance-request-app/Dockerfile
git commit -m "feat: add Dockerfile for insurance-request-app"
```

---

### Task 4: Create Dockerfile for `insurance-response-app`

**Files:**
- Create: `insurance-response-app/Dockerfile`

- [ ] **Step 1: Create the multi-stage Dockerfile**

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Copy module build files
COPY common/build.gradle common/build.gradle
COPY claims-app/build.gradle claims-app/build.gradle
COPY insurance-request-app/build.gradle insurance-request-app/build.gradle
COPY insurance-response-app/build.gradle insurance-response-app/build.gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY common/src common/src
COPY insurance-response-app/src insurance-response-app/src

# Build the boot JAR
RUN ./gradlew :insurance-response-app:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/insurance-response-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Build the Docker image locally to verify**

Run: `docker build -f insurance-response-app/Dockerfile -t insurance-response-app:local .`
Expected: Build completes successfully.

- [ ] **Step 3: Run the image to verify it starts**

Run: `docker run --rm -e SPRING_PROFILES_ACTIVE=cloud -e MONGODB_URI=mongodb://host.docker.internal:27017/edi_healthcare -e PORT=8080 -p 9092:8080 insurance-response-app:local`
Expected: Spring Boot starts. Press Ctrl+C to stop.

- [ ] **Step 4: Commit**

```bash
git add insurance-response-app/Dockerfile
git commit -m "feat: add Dockerfile for insurance-response-app"
```

---

### Task 5: Create `application-cloud.yml` for `claims-app`

**Files:**
- Create: `claims-app/src/main/resources/application-cloud.yml`

- [ ] **Step 1: Create the cloud profile config**

```yaml
server:
  port: ${PORT:8080}

spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:edi_healthcare}
  mongodb:
    uri: ${MONGODB_URI}
```

This overrides the hardcoded `localhost` MongoDB URI and the fixed port from `application.yml`. The `PORT` env var is set by Cloud Run automatically. `MONGODB_URI` comes from GCP Secret Manager.

- [ ] **Step 2: Verify the file is in the correct location**

Run: `ls claims-app/src/main/resources/application*.yml`
Expected: Both `application.yml` and `application-cloud.yml` listed.

- [ ] **Step 3: Commit**

```bash
git add claims-app/src/main/resources/application-cloud.yml
git commit -m "feat: add cloud profile config for claims-app"
```

---

### Task 6: Create `application-cloud.yml` for `insurance-request-app`

**Files:**
- Create: `insurance-request-app/src/main/resources/application-cloud.yml`

- [ ] **Step 1: Create the cloud profile config**

```yaml
server:
  port: ${PORT:8080}

spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:edi_healthcare}
  mongodb:
    uri: ${MONGODB_URI}
```

- [ ] **Step 2: Verify the file is in the correct location**

Run: `ls insurance-request-app/src/main/resources/application*.yml`
Expected: Both `application.yml` and `application-cloud.yml` listed.

- [ ] **Step 3: Commit**

```bash
git add insurance-request-app/src/main/resources/application-cloud.yml
git commit -m "feat: add cloud profile config for insurance-request-app"
```

---

### Task 7: Create `application-cloud.yml` for `insurance-response-app`

**Files:**
- Create: `insurance-response-app/src/main/resources/application-cloud.yml`

- [ ] **Step 1: Create the cloud profile config**

```yaml
server:
  port: ${PORT:8080}

spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:edi_healthcare}
  mongodb:
    uri: ${MONGODB_URI}
```

The multipart config from the base `application.yml` carries over automatically — Spring merges profile configs with the base, so no need to repeat it here.

- [ ] **Step 2: Verify the file is in the correct location**

Run: `ls insurance-response-app/src/main/resources/application*.yml`
Expected: Both `application.yml` and `application-cloud.yml` listed.

- [ ] **Step 3: Commit**

```bash
git add insurance-response-app/src/main/resources/application-cloud.yml
git commit -m "feat: add cloud profile config for insurance-response-app"
```

---

### Task 8: Create GitHub Actions CI/CD Workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create the workflow directory**

Run: `mkdir -p .github/workflows`

- [ ] **Step 2: Create the workflow file**

```yaml
name: CI/CD

on:
  push:
    branches: [main]
  workflow_dispatch:
    inputs:
      app:
        description: 'App to deploy'
        required: true
        type: choice
        options:
          - claims-app
          - insurance-request-app
          - insurance-response-app
          - all
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - prod

env:
  GCP_REGION: us-central1
  GAR_REPOSITORY: edi-healthcare
  GCP_PROJECT_DEV: ${{ vars.GCP_PROJECT_DEV }}
  GCP_PROJECT_PROD: ${{ vars.GCP_PROJECT_PROD }}
  WIF_PROVIDER: ${{ vars.WIF_PROVIDER }}
  WIF_SERVICE_ACCOUNT: ${{ vars.WIF_SERVICE_ACCOUNT }}

jobs:
  # ------------------------------------------------------------------
  # Job 1: Run all tests
  # ------------------------------------------------------------------
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Start MongoDB
        run: docker compose up -d

      - name: Wait for MongoDB
        run: |
          for i in $(seq 1 30); do
            if docker compose exec -T mongodb mongosh --eval "db.runCommand('ping')" > /dev/null 2>&1; then
              echo "MongoDB is ready"
              exit 0
            fi
            sleep 1
          done
          echo "MongoDB failed to start"
          exit 1

      - name: Run tests
        run: ./gradlew test

      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/'
          retention-days: 7

  # ------------------------------------------------------------------
  # Job 2: Detect which apps changed
  # ------------------------------------------------------------------
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      apps: ${{ steps.set-matrix.outputs.apps }}
      has_changes: ${{ steps.set-matrix.outputs.has_changes }}
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 2

      - name: Determine affected apps
        id: set-matrix
        run: |
          if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then
            APP="${{ github.event.inputs.app }}"
            if [[ "$APP" == "all" ]]; then
              echo 'apps=["claims-app","insurance-request-app","insurance-response-app"]' >> "$GITHUB_OUTPUT"
            else
              echo "apps=[\"$APP\"]" >> "$GITHUB_OUTPUT"
            fi
            echo "has_changes=true" >> "$GITHUB_OUTPUT"
            exit 0
          fi

          CHANGED_FILES=$(git diff --name-only HEAD~1 HEAD)
          echo "Changed files:"
          echo "$CHANGED_FILES"

          APPS=()

          # Check if shared files changed (triggers all apps)
          if echo "$CHANGED_FILES" | grep -qE '^(common/|build\.gradle|settings\.gradle|gradle/|gradle\.properties|\.github/workflows/)'; then
            APPS=("claims-app" "insurance-request-app" "insurance-response-app")
          else
            if echo "$CHANGED_FILES" | grep -q '^claims-app/'; then
              APPS+=("claims-app")
            fi
            if echo "$CHANGED_FILES" | grep -q '^insurance-request-app/'; then
              APPS+=("insurance-request-app")
            fi
            if echo "$CHANGED_FILES" | grep -q '^insurance-response-app/'; then
              APPS+=("insurance-response-app")
            fi
          fi

          if [[ ${#APPS[@]} -eq 0 ]]; then
            echo 'apps=[]' >> "$GITHUB_OUTPUT"
            echo "has_changes=false" >> "$GITHUB_OUTPUT"
          else
            JSON=$(printf '%s\n' "${APPS[@]}" | jq -R . | jq -sc .)
            echo "apps=$JSON" >> "$GITHUB_OUTPUT"
            echo "has_changes=true" >> "$GITHUB_OUTPUT"
          fi

  # ------------------------------------------------------------------
  # Job 3: Build and push Docker images
  # ------------------------------------------------------------------
  build-and-push:
    needs: [test, detect-changes]
    if: needs.detect-changes.outputs.has_changes == 'true'
    runs-on: ubuntu-latest
    strategy:
      matrix:
        app: ${{ fromJson(needs.detect-changes.outputs.apps) }}
    permissions:
      contents: read
      id-token: write
    steps:
      - uses: actions/checkout@v4

      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ env.WIF_PROVIDER }}
          service_account: ${{ env.WIF_SERVICE_ACCOUNT }}

      - name: Set up Cloud SDK
        uses: google-github-actions/setup-gcloud@v2

      - name: Configure Docker for Artifact Registry
        run: gcloud auth configure-docker ${{ env.GCP_REGION }}-docker.pkg.dev

      - name: Determine image tag
        id: tag
        run: |
          IMAGE="${{ env.GCP_REGION }}-docker.pkg.dev/${{ env.GCP_PROJECT_DEV }}/${{ env.GAR_REPOSITORY }}/${{ matrix.app }}"
          echo "image=$IMAGE" >> "$GITHUB_OUTPUT"
          echo "sha_tag=$IMAGE:${{ github.sha }}" >> "$GITHUB_OUTPUT"

      - name: Build Docker image
        run: |
          docker build \
            -f ${{ matrix.app }}/Dockerfile \
            -t ${{ steps.tag.outputs.sha_tag }} \
            .

      - name: Push Docker image
        run: |
          docker push ${{ steps.tag.outputs.sha_tag }}

  # ------------------------------------------------------------------
  # Job 4: Deploy to dev (auto on push to main)
  # ------------------------------------------------------------------
  deploy-dev:
    needs: [build-and-push, detect-changes]
    if: github.event_name == 'push' && needs.detect-changes.outputs.has_changes == 'true'
    runs-on: ubuntu-latest
    environment: dev
    strategy:
      matrix:
        app: ${{ fromJson(needs.detect-changes.outputs.apps) }}
    permissions:
      contents: read
      id-token: write
    steps:
      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ env.WIF_PROVIDER }}
          service_account: ${{ env.WIF_SERVICE_ACCOUNT }}

      - name: Set up Cloud SDK
        uses: google-github-actions/setup-gcloud@v2

      - name: Tag image for dev
        run: |
          IMAGE="${{ env.GCP_REGION }}-docker.pkg.dev/${{ env.GCP_PROJECT_DEV }}/${{ env.GAR_REPOSITORY }}/${{ matrix.app }}"
          gcloud artifacts docker tags add \
            "$IMAGE:${{ github.sha }}" \
            "$IMAGE:latest-dev"

      - name: Deploy to Cloud Run (dev)
        run: |
          IMAGE="${{ env.GCP_REGION }}-docker.pkg.dev/${{ env.GCP_PROJECT_DEV }}/${{ env.GAR_REPOSITORY }}/${{ matrix.app }}"
          gcloud run deploy ${{ matrix.app }}-dev \
            --image "$IMAGE:${{ github.sha }}" \
            --region ${{ env.GCP_REGION }} \
            --project ${{ env.GCP_PROJECT_DEV }} \
            --platform managed \
            --set-env-vars "SPRING_PROFILES_ACTIVE=cloud" \
            --set-secrets "MONGODB_URI=mongodb-uri-dev:latest" \
            --memory 512Mi \
            --cpu 1 \
            --min-instances 0 \
            --max-instances 3 \
            --allow-unauthenticated

  # ------------------------------------------------------------------
  # Job 5: Deploy to prod (manual via workflow_dispatch)
  # ------------------------------------------------------------------
  deploy-prod:
    needs: [build-and-push, detect-changes]
    if: github.event_name == 'workflow_dispatch'
    runs-on: ubuntu-latest
    environment: prod
    strategy:
      matrix:
        app: ${{ fromJson(needs.detect-changes.outputs.apps) }}
    permissions:
      contents: read
      id-token: write
    steps:
      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ env.WIF_PROVIDER }}
          service_account: ${{ env.WIF_SERVICE_ACCOUNT }}

      - name: Set up Cloud SDK
        uses: google-github-actions/setup-gcloud@v2

      - name: Tag image for prod
        run: |
          IMAGE="${{ env.GCP_REGION }}-docker.pkg.dev/${{ env.GCP_PROJECT_DEV }}/${{ env.GAR_REPOSITORY }}/${{ matrix.app }}"
          gcloud artifacts docker tags add \
            "$IMAGE:${{ github.sha }}" \
            "$IMAGE:latest-prod"

      - name: Deploy to Cloud Run (prod)
        run: |
          IMAGE="${{ env.GCP_REGION }}-docker.pkg.dev/${{ env.GCP_PROJECT_DEV }}/${{ env.GAR_REPOSITORY }}/${{ matrix.app }}"
          gcloud run deploy ${{ matrix.app }}-prod \
            --image "$IMAGE:${{ github.sha }}" \
            --region ${{ env.GCP_REGION }} \
            --project ${{ env.GCP_PROJECT_PROD }} \
            --platform managed \
            --set-env-vars "SPRING_PROFILES_ACTIVE=cloud" \
            --set-secrets "MONGODB_URI=mongodb-uri-prod:latest" \
            --memory 512Mi \
            --cpu 1 \
            --min-instances 0 \
            --max-instances 3 \
            --allow-unauthenticated
```

- [ ] **Step 3: Validate the workflow YAML syntax**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"`
Expected: No output (no syntax errors). If `yaml` is not installed, run: `pip3 install pyyaml` first.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "feat: add GitHub Actions CI/CD workflow for Cloud Run deployment"
```

---

### Task 9: Verify All Dockerfiles Build Successfully

This is a final integration check to ensure all three images build correctly.

- [ ] **Step 1: Build all three images**

Run each sequentially (they share Docker layer cache):

```bash
docker build -f claims-app/Dockerfile -t claims-app:local .
docker build -f insurance-request-app/Dockerfile -t insurance-request-app:local .
docker build -f insurance-response-app/Dockerfile -t insurance-response-app:local .
```

Expected: All three builds complete successfully.

- [ ] **Step 2: Verify image sizes are reasonable**

Run: `docker images | grep -E '(claims-app|insurance-request-app|insurance-response-app)' | grep local`
Expected: Each image should be roughly 200-350MB (JRE + Spring Boot fat JAR).

- [ ] **Step 3: Run all tests to confirm nothing was broken**

Run: `./gradlew test`
Expected: All tests pass. The new files (Dockerfiles, cloud profiles, workflow) should not affect existing tests.

- [ ] **Step 4: Final commit if any adjustments were needed**

If any fixes were required during verification, commit them:

```bash
git add -A
git commit -m "fix: adjust Docker/config files after integration verification"
```

---

## GCP Setup Checklist (Manual, Not Automated)

These are one-time setup steps performed in the GCP Console or via `gcloud` CLI. They are not part of the codebase and are not automated by this plan.

1. **Create GCP projects:** `edi-healthcare-dev` and `edi-healthcare-prod` (or use existing projects)
2. **Enable APIs** in both projects: Cloud Run, Artifact Registry, Secret Manager, IAM Credentials
3. **Create Artifact Registry repository:** `gcloud artifacts repositories create edi-healthcare --repository-format=docker --location=us-central1 --project=PROJECT_ID`
4. **Create secrets:** `gcloud secrets create mongodb-uri-dev --data-file=-` (pipe the Atlas connection string)
5. **Set up Workload Identity Federation:**
   - Create a workload identity pool: `gcloud iam workload-identity-pools create github-pool --location=global`
   - Create a provider: `gcloud iam workload-identity-pools providers create-oidc github-provider --location=global --workload-identity-pool=github-pool --issuer-uri=https://token.actions.githubusercontent.com --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" --attribute-condition="assertion.repository.startsWith(\"https://github.com/\")"`
   - Grant the service account permission to be impersonated by the GitHub repo
6. **Set GitHub repository variables** (Settings → Secrets and variables → Actions → Variables):
   - `GCP_PROJECT_DEV` — dev GCP project ID
   - `GCP_PROJECT_PROD` — prod GCP project ID
   - `WIF_PROVIDER` — full provider resource name
   - `WIF_SERVICE_ACCOUNT` — service account email
7. **Create GitHub Environments:** `dev` and `prod` (Settings → Environments). Add required reviewers to `prod`.
8. **MongoDB Atlas:** Create clusters/databases, allowlist Cloud Run egress IPs, store connection strings in Secret Manager.
