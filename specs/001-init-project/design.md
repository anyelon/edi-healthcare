# EDI Healthcare Multi-Module Monorepo — Design Spec

## Context

Build a clean architecture REST API system for EDI healthcare transactions (837 Claims, 270 Eligibility Inquiry, 271 Eligibility Response). The system is a Gradle multi-module monorepo with 3 independent Spring Boot apps sharing common domain and MongoDB repositories. Data is stored in local MongoDB via Docker Compose. All endpoints are documented via Swagger UI.

## Module Structure

```
edi-healthcare/
├── common/                      (java-library, shared domain + repos)
├── claims-app/                  (Spring Boot, port 8080)
├── insurance-request-app/       (Spring Boot, port 8081)
└── insurance-response-app/      (Spring Boot, port 8082)
```

### Gradle Configuration

- **Root**: applies `org.springframework.boot` with `apply false`, sets Java 21 toolchain for all subprojects
- **common**: `java-library` plugin, imports Spring Boot BOM, exposes `spring-boot-starter-data-mongodb` and `spring-boot-starter-validation` as `api` dependencies
- **App modules**: `org.springframework.boot` plugin, depend on `project(':common')`, include `spring-boot-starter-web` and `springdoc-openapi-starter-webmvc-ui:2.8.6`

## Data Model (common module)

### Patient (`@Document("patients")`)
| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB _id |
| firstName, lastName | String | Demographics |
| dateOfBirth | LocalDate | DOB |
| gender | String | M/F |
| memberId | String | Insurance member ID |
| insurancePayerId | String | Payer ID (e.g., "12345") |
| insurancePayerName | String | Payer name (e.g., "AETNA") |
| insuranceGroupNumber | String | Group number |
| address, city, state, zipCode | String | Address |

### Visit (`@Document("visits")`)
| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB _id |
| patientId | String | FK to Patient |
| dateOfService | LocalDate | DOS |
| diagnosisCodes | List\<String\> | ICD-10 codes |
| procedureCodes | List\<String\> | CPT codes |
| chargeAmounts | List\<BigDecimal\> | Parallel to procedureCodes |
| placeOfServiceCode | String | POS code (e.g., "11") |
| renderingProviderNpi | String | Rendering provider NPI |

### Company (`@Document("companies")`)
| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB _id |
| name | String | Billing provider name |
| npi | String | 10-digit NPI |
| taxId | String | EIN |
| address, city, state, zipCode | String | Address |
| contactPhone | String | Phone |

### PlaceOfService (`@Document("places_of_service")`)
| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB _id |
| code | String | POS code |
| description | String | POS description |

### Repositories
- `PatientRepository extends MongoRepository<Patient, String>`
- `VisitRepository extends MongoRepository<Visit, String>` — custom: `findByPatientIdAndDateOfService(String, LocalDate)`
- `CompanyRepository extends MongoRepository<Company, String>`
- `PlaceOfServiceRepository extends MongoRepository<PlaceOfService, String>` — custom: `findByCode(String)`

## Application Flows

### Flow 1: Claims (port 8080)

```
POST /api/claims/generate {patientId, dateOfService}
  → ClaimsController (@Valid)
    → ClaimsService
      → VisitRepository.findByPatientIdAndDateOfService()
      → PatientRepository.findById()
      → CompanyRepository (first/default)
      → PlaceOfServiceRepository.findByCode()
      → EDI837Service.to837(company, patient, visit, pos) → String
    → ResponseEntity<byte[]> (Content-Disposition: attachment, filename=837_claim.edi)
```

### Flow 2: Insurance Request 270 (port 8081)

```
POST /api/insurance/eligibility-request {patientId}
  → InsuranceRequestController (@Valid)
    → InsuranceRequestService
      → PatientRepository.findById()
      → CompanyRepository (first/default)
      → EDI270Service.to270(company, patient) → String
    → ResponseEntity<byte[]> (Content-Disposition: attachment, filename=270_inquiry.edi)
```

### Flow 3: Insurance Response 271 (port 8082)

```
POST /api/insurance/eligibility-response (MultipartFile)
  → InsuranceResponseController
    → Save file to temp dir
    → InsuranceResponseService
      → EDI271Service.parse(filePath) → VerificationResult
    → ResponseEntity<VerificationResult> (JSON)

VerificationResult: {patientId, verificationStatus, verificationMessage}
```

## EDI Format Details

### 837P Segments
ISA/GS/ST envelope → BHT*0019*00 → NM1*41 (submitter) → NM1*40 (receiver) → HL*1 (billing provider) → NM1*85 + N3/N4 + REF*EI → HL*2 (subscriber) → SBR → NM1*IL + N3/N4 + DMG → NM1*PR (payer) → CLM → HI*ABK (diagnosis) → LX → SV1 (service) → DTP*472 → SE/GE/IEA

### 270 Segments
ISA/GS*HS/ST*270 → BHT*0022*13 → HL*1 (payer) → NM1*PR → HL*2 (provider) → NM1*1P + REF*EI → HL*3 (subscriber) → NM1*IL + DMG + DTP*291 + EQ*30 → SE/GE/IEA

### 271 Parsing Logic
1. Split by `~` segment terminator
2. `NM1*IL` element 9 → memberId (maps to patientId)
3. `EB` element 1: `1`=ACTIVE, `6`=INACTIVE
4. `MSG` element 1 → verification message

## Infrastructure

### docker-compose.yml
- MongoDB 7, port 27017, database `edi_healthcare`, named volume `mongo-data`

### application.yml (per app)
- `server.port`: 8080/8081/8082
- `spring.data.mongodb.uri`: mongodb://localhost:27017/edi_healthcare
- `springdoc.swagger-ui.path`: /swagger-ui.html

## DTOs (record types)

- `ClaimsRequest(String patientId, LocalDate dateOfService)` — @NotBlank, @NotNull
- `InsuranceRequestDTO(String patientId)` — @NotBlank
- `VerificationResult(String patientId, String verificationStatus, String verificationMessage)`

## Test Strategy

| Layer | Annotation | What it tests | MongoDB |
|-------|-----------|---------------|---------|
| Unit (`*Test`) | None | EDI services (string logic), Services (mocked repos) | Mocked |
| Controller (`*ControllerTest`) | `@WebMvcTest` | Validation, HTTP contract, response format | N/A |
| Integration (`*IT`) | `@SpringBootTest` | End-to-end flow | Testcontainers |

Gradle separates unit/integration: `test` task excludes `*IT`, dedicated `integrationTest` task includes only `*IT`.
