# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Java/Gradle)

```bash
# Start MongoDB (required for all apps)
docker-compose up -d mongodb

# Build all modules
./gradlew build

# Build a specific module
./gradlew :claims-app:build
./gradlew :insurance-request-app:build
./gradlew :insurance-response-app:build

# Run individual apps
./gradlew :claims-app:bootRun          # port 8080
./gradlew :insurance-request-app:bootRun   # port 8081
./gradlew :insurance-response-app:bootRun  # port 8082

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :claims-app:test
./gradlew :insurance-request-app:test
./gradlew :insurance-response-app:test

# Run a single test class
./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837ServiceTest"

# Clean build
./gradlew clean build
```

### Frontend (Next.js)

```bash
# Install dependencies
cd frontend && npm install

# Run dev server (port 3000)
cd frontend && npm run dev

# Build for production
cd frontend && npm run build

# Lint
cd frontend && npm run lint
```

### Full Stack Local Dev

```bash
# 1. Start MongoDB
docker-compose up -d mongodb

# 2. Start backend services (each in its own terminal)
./gradlew :claims-app:bootRun          # port 8080
./gradlew :insurance-request-app:bootRun   # port 8081
./gradlew :insurance-response-app:bootRun  # port 8082

# 3. Start frontend
cd frontend && npm run dev             # port 3000
```

## Swagger UI

Each app exposes Swagger UI when running:
- Claims: http://localhost:8080/swagger-ui.html
- Insurance Request: http://localhost:8081/swagger-ui.html
- Insurance Response: http://localhost:8082/swagger-ui.html

## Architecture

Monorepo with `backend/` (Java) and `frontend/` (Next.js) directories. Spring Boot 4.0.4, Java 21, MongoDB.

### Project Layout

```
backend/           # Java modules (Gradle build at root)
  common/          # Shared entities and repositories
  claims-app/      # EDI 837 claims generation (port 8080)
  insurance-request-app/   # EDI 270 eligibility requests (port 8081)
  insurance-response-app/  # EDI 271 response parsing (port 8082)
frontend/          # Next.js app with BFF API routes (port 3000)
```

Gradle config (`settings.gradle`, `build.gradle`) stays at the repo root. Module names are preserved via `projectDir` remapping — use `:claims-app:build` (not `:backend:claims-app:build`).

### Backend Modules

- **common** — Shared `@Document` entities (Patient, Visit, Company, PlaceOfService) and `MongoRepository` interfaces. `java-library` plugin (plain JAR, not a Spring Boot app).
- **claims-app** (port 8080) — Generates EDI 837P professional claims. POST `/api/claims/generate` with `{encounterIds}` → downloadable `.edi` file.
- **insurance-request-app** (port 8081) — Generates EDI 270 eligibility inquiries. POST `/api/insurance/eligibility-request` with `{patientIds}` → downloadable `.edi` file.
- **insurance-response-app** (port 8082) — Parses EDI 271 eligibility responses. POST `/api/insurance/eligibility-response` with multipart file → JSON.

### Frontend

Next.js 16 app with TypeScript and Tailwind CSS. Uses BFF (Backend-For-Frontend) pattern — API routes in `frontend/src/app/api/` proxy to backend services. The browser never calls backend ports directly.

### Layer Structure (per app)

```
Controller → Service → Repository (common) + EDI Service
```

- Controllers handle HTTP, validation (`@Valid`), and response formatting
- Services orchestrate repository lookups and EDI generation/parsing
- EDI services (`EDI837Service`, `EDI270Service`, `EDI271Service`) are pure business logic — no repository access
- `@EnableMongoRepositories` lives in each app's `config/MongoConfig.java` (separated from `@SpringBootApplication` for clean `@WebMvcTest` slicing)

## Conventions

- Constructor injection only (no `@Autowired` on fields)
- Record types for DTOs (`ClaimsRequest`, `InsuranceRequestDTO`, `VerificationResult`)
- `MongoRepository` interfaces in common, not JPA
- Each app scans both its own package and `com.example.edi.common` via `scanBasePackages`
- Test naming: `*Test.java` for unit tests, `*IT.java` for integration tests

## Specs & Plans

All specs, plans, and related assets (mockup images, ERD diagrams, etc.) live under `specs/` at the project root. Each spec gets its own sequentially numbered folder:

```
specs/
├── 001-init-project/
│   ├── design.md
│   ├── plan.md
│   └── erd.html
├── 002-claims-generator/
│   ├── design.md
│   ├── plan.md
│   ├── erd.html
│   └── mockup.png
└── 003-eligibility-request/
    └── ...
```

- Folder names: `NNN-short-description` (e.g., `002-claims-generator`)
- All files for a spec (design doc, implementation plan, diagrams, mockups) go in the same folder
- Never create specs outside of `specs/`

## Spring Boot 4.x Notes

- `@WebMvcTest` import: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (moved from `boot.test.autoconfigure.web.servlet`)
- Requires `spring-boot-starter-webmvc-test` as test dependency
- Use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito` (not the deprecated `@MockBean`)
- MongoDB auto-config packages: `org.springframework.boot.mongodb.autoconfigure` and `org.springframework.boot.data.mongodb.autoconfigure`
