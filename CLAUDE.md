# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Start MongoDB (required for all apps)
docker-compose up -d

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

## Swagger UI

Each app exposes Swagger UI when running:
- Claims: http://localhost:8080/swagger-ui.html
- Insurance Request: http://localhost:8081/swagger-ui.html
- Insurance Response: http://localhost:8082/swagger-ui.html

## Architecture

Gradle multi-module monorepo for EDI healthcare transactions. Spring Boot 4.0.4, Java 21, MongoDB.

### Modules

- **common** — Shared `@Document` entities (Patient, Visit, Company, PlaceOfService) and `MongoRepository` interfaces. `java-library` plugin (plain JAR, not a Spring Boot app).
- **claims-app** (port 8080) — Generates EDI 837P professional claims. POST `/api/claims/generate` with `{patientId, dateOfService}` → downloadable `.edi` file.
- **insurance-request-app** (port 8081) — Generates EDI 270 eligibility inquiries. POST `/api/insurance/eligibility-request` with `{patientId}` → downloadable `.edi` file.
- **insurance-response-app** (port 8082) — Parses EDI 271 eligibility responses. POST `/api/insurance/eligibility-response` with multipart file → JSON `{patientId, verificationStatus, verificationMessage}`.

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
