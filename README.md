# EDI Healthcare

A monorepo for processing EDI (Electronic Data Interchange) healthcare transactions. Includes a Spring Boot backend for generating EDI 837P professional claims, EDI 270 eligibility inquiries, and parsing EDI 271 eligibility responses, plus a Next.js frontend with a BFF (Backend-For-Frontend) API layer.

## Tech Stack

### Backend
- **Java 21** / **Spring Boot 4.0.4**
- **MongoDB 7** (via Docker)
- **StAEDI 1.25.3** — EDI X12 generation and parsing
- **Gradle** — multi-module build
- **SpringDoc OpenAPI 3.0.2** — Swagger UI
- **Testcontainers** — integration testing

### Frontend
- **Next.js 16** / **React 19** / **TypeScript**
- **Tailwind CSS 4**
- **BFF API routes** — server-side proxies to backend services

## Project Structure

```
backend/                          # Java modules (Gradle build at root)
  common/                         # Shared MongoDB entities and repositories
  claims-app/                     # EDI 837 claims generation (port 8080)
  insurance-request-app/          # EDI 270 eligibility requests (port 8081)
  insurance-response-app/         # EDI 271 response parsing (port 8082)
frontend/                         # Next.js app with BFF API routes (port 3000)
  src/app/                        # Pages and API routes
  src/lib/                        # Shared utilities
  src/types/                      # TypeScript interfaces
```

Gradle config (`settings.gradle`, `build.gradle`) stays at the repo root. Module names are preserved via `projectDir` remapping — use `:claims-app:build` (not `:backend:claims-app:build`).

## Modules

| Module | Port | Description |
|--------|------|-------------|
| **common** | — | Shared MongoDB entities, repositories, and EDI loop records |
| **claims-app** | 8080 | Generates EDI 837P professional claims |
| **insurance-request-app** | 8081 | Generates EDI 270 eligibility inquiries |
| **insurance-response-app** | 8082 | Parses EDI 271 eligibility responses |
| **frontend** | 3000 | Next.js UI with BFF API routes |

## Getting Started

### Prerequisites

- Java 21
- Node.js 20+
- Docker & Docker Compose

### Start MongoDB

```bash
docker-compose up -d mongodb
```

### Build & Run Backend

```bash
# Build all modules
./gradlew build

# Run individual apps
./gradlew :claims-app:bootRun              # port 8080
./gradlew :insurance-request-app:bootRun   # port 8081
./gradlew :insurance-response-app:bootRun  # port 8082
```

### Build & Run Frontend

```bash
cd frontend
npm install
npm run dev    # port 3000
```

### Full Stack Local Dev

```bash
# 1. Start MongoDB
docker-compose up -d mongodb

# 2. Start backend services (each in its own terminal)
./gradlew :claims-app:bootRun              # port 8080
./gradlew :insurance-request-app:bootRun   # port 8081
./gradlew :insurance-response-app:bootRun  # port 8082

# 3. Start frontend
cd frontend && npm run dev                 # port 3000
```

Open http://localhost:3000 to access the dashboard. Use the "Seed Database" button to populate test data.

## Frontend

The Next.js frontend provides a UI for all three EDI workflows:

- **Dashboard** — Overview with seed database button and links to workflows
- **Claims (837)** — Enter encounter IDs to generate and download EDI 837 claim files
- **Eligibility Request (270)** — Enter patient IDs to generate and download EDI 270 inquiry files
- **Eligibility Response (271)** — Upload EDI 271 files to view parsed coverage details, benefits, and subscriber info

The frontend uses the BFF pattern — API routes in `frontend/src/app/api/` proxy requests to the backend services server-side. The browser never calls backend ports directly.

## API Endpoints

### Claims App (port 8080)

**Generate EDI 837P claim**

```
POST /api/claims/generate
Content-Type: application/json

{"encounterIds": ["id1", "id2"]}
```

Returns a downloadable `837_claim.edi` file.

**Seed test data**

```
POST /api/dev/seed
```

Creates sample patients, encounters, diagnoses, procedures, and related entities.

### Insurance Request App (port 8081)

**Generate EDI 270 eligibility inquiry**

```
POST /api/insurance/eligibility-request
Content-Type: application/json

{"patientIds": ["patient_id1", "patient_id2"]}
```

Returns a downloadable `270_inquiry.edi` file.

### Insurance Response App (port 8082)

**Parse EDI 271 eligibility response**

```
POST /api/insurance/eligibility-response
Content-Type: multipart/form-data

file: <271_response.edi>
```

Returns parsed eligibility details including subscriber info, payer, coverage dates, and benefits.

### Swagger UI

Each running backend app exposes interactive API docs:

- Claims: http://localhost:8080/swagger-ui.html
- Insurance Request: http://localhost:8081/swagger-ui.html
- Insurance Response: http://localhost:8082/swagger-ui.html

## Architecture

### Backend Layer Structure

```
Controller → Service → Repository (common) + EDI Service
```

- **Controllers** handle HTTP, validation, and response formatting
- **Services** orchestrate repository lookups and EDI generation/parsing
- **EDI Services** (`EDI837Generator`, `EDI270Service`, `EDI271Service`) contain pure business logic with no repository access
- **Common module** provides shared MongoDB `@Document` entities and `MongoRepository` interfaces

### Frontend Architecture

```
Browser → Next.js Pages → BFF API Routes → Backend Services
```

- **Pages** (`src/app/*/page.tsx`) — client components with forms and result display
- **BFF Routes** (`src/app/api/*/route.ts`) — server-side proxies forwarding requests to backend services
- **API Client** (`src/lib/api-client.ts`) — shared fetch helpers for calling BFF routes

### Data Model

The common module defines these MongoDB collections:

- `patients` — demographics, member ID, insurance info
- `patient_insurances` — insurance policies per patient
- `practices` — medical practices with NPI and tax ID
- `providers` — individual providers linked to practices
- `facilities` — service locations with place-of-service codes
- `encounters` — patient visits linking provider, facility, and authorization
- `encounter_diagnoses` — ICD-10 codes ranked per encounter
- `encounter_procedures` — CPT codes with charges and diagnosis pointers
- `payers` — insurance payer directory
- `companies` — submitter/receiver organizations
- `eligibility_responses` — parsed EDI 271 response data

## Testing

```bash
# Run all backend tests
./gradlew test

# Run tests for a specific module
./gradlew :claims-app:test
./gradlew :insurance-request-app:test
./gradlew :insurance-response-app:test

# Run a single test class
./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837ServiceTest"

# Lint and build frontend
cd frontend && npm run lint && npm run build
```

Integration tests use Testcontainers to spin up a MongoDB instance automatically.

## CI/CD

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs on pushes to `main` and on pull requests:

- **test** — Runs all backend tests with a real MongoDB instance
- **test-frontend** — Lints and builds the frontend
- **detect-changes** — Identifies which apps changed to build/deploy only what's needed
- **build-and-push** — Builds Docker images and pushes to Google Artifact Registry (main branch only)
- **deploy-dev** — Auto-deploys to Cloud Run dev on push to main
- **deploy-prod** — Manual deployment via workflow_dispatch

## Project Conventions

- Constructor injection only (no `@Autowired` on fields)
- Record types for DTOs
- Test naming: `*Test.java` for unit tests, `*IT.java` for integration tests
- Design specs and implementation plans live under `specs/`

## License

Private repository — all rights reserved.
