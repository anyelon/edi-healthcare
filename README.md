# EDI Healthcare

A Spring Boot multi-module monorepo for processing EDI (Electronic Data Interchange) healthcare transactions. Supports generating EDI 837P professional claims, EDI 270 eligibility inquiries, and parsing EDI 271 eligibility responses.

## Tech Stack

- **Java 21** / **Spring Boot 4.0.4**
- **MongoDB 7** (via Docker)
- **StAEDI 1.25.3** — EDI X12 generation and parsing
- **Gradle** — multi-module build
- **SpringDoc OpenAPI 3.0.2** — Swagger UI
- **Testcontainers** — integration testing

## Modules

| Module | Port | Description |
|--------|------|-------------|
| **common** | — | Shared MongoDB entities, repositories, and EDI loop records |
| **claims-app** | 8080 | Generates EDI 837P professional claims |
| **insurance-request-app** | 8081 | Generates EDI 270 eligibility inquiries |
| **insurance-response-app** | 8082 | Parses EDI 271 eligibility responses |

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose

### Start MongoDB

```bash
docker-compose up -d
```

### Build

```bash
./gradlew build
```

### Run

```bash
# Run individual apps
./gradlew :claims-app:bootRun              # port 8080
./gradlew :insurance-request-app:bootRun   # port 8081
./gradlew :insurance-response-app:bootRun  # port 8082
```

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

{"patientId": "patient_id"}
```

Returns a downloadable `270_inquiry.edi` file.

### Insurance Response App (port 8082)

**Parse EDI 271 eligibility response**

```
POST /api/insurance/eligibility-response
Content-Type: multipart/form-data

file: <271_response.edi>
```

Returns:

```json
{
  "patientId": "MEMBER001",
  "status": "ACTIVE",
  "verificationMessage": "ELIGIBLE"
}
```

### Swagger UI

Each running app exposes interactive API docs:

- Claims: http://localhost:8080/swagger-ui.html
- Insurance Request: http://localhost:8081/swagger-ui.html
- Insurance Response: http://localhost:8082/swagger-ui.html

## Architecture

```
Controller → Service → Repository (common) + EDI Service
```

- **Controllers** handle HTTP, validation, and response formatting
- **Services** orchestrate repository lookups and EDI generation/parsing
- **EDI Services** (`EDI837Generator`, `EDI270Service`, `EDI271Service`) contain pure business logic with no repository access
- **Common module** provides shared MongoDB `@Document` entities and `MongoRepository` interfaces

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

## Testing

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :claims-app:test
./gradlew :insurance-request-app:test
./gradlew :insurance-response-app:test

# Run a single test class
./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837ServiceTest"
```

Integration tests use Testcontainers to spin up a MongoDB instance automatically.

## Project Conventions

- Constructor injection only (no `@Autowired` on fields)
- Record types for DTOs
- Test naming: `*Test.java` for unit tests, `*IT.java` for integration tests
- Design specs and implementation plans live under `specs/`

## License

Private repository — all rights reserved.
