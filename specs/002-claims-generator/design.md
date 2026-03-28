# EDI 837P Claim Generation — Design Spec

## Context

Build a service that generates EDI 837P 5010 professional claims from patient encounter data stored in MongoDB. The service will:

- Model healthcare billing domain concepts (practices, providers, facilities, encounters, diagnoses, procedures, insurance coverage) as MongoDB documents
- Map database entities into transient 837 segment loop classes that represent the EDI structure
- Use a centralized generator to produce valid 837P files from those loop classes
- Support configurable ISA envelope values for clearinghouse integration
- Be tested using TDD with Testcontainers for MongoDB integration tests

## ADR-001: Use StAEDI for EDI generation and parsing

**Status:** Accepted

**Context:** The service needs to generate structurally valid EDI X12 837P files and (in future specs) parse 270/271 files. We evaluated several approaches:

| Option | Parse | Generate | License | Maintained | Notes |
|--------|-------|----------|---------|------------|-------|
| **Hand-built StringBuilder** | No | Yes | N/A | N/A | Fragile, no validation, must manage separators/padding/segment counts manually |
| **StAEDI (`io.xlate:staedi`)** | Yes | Yes | Apache 2.0 | Active (Mar 2026) | Streaming StAX-like API, handles ISA padding/separators/terminators |
| **Stedi** | Yes | Yes | N/A | N/A | Cloud API only, no Java SDK, TypeScript-focused |
| **Smooks EDI** | Yes | Yes | Apache 2.0 | Jan 2025 | Requires custom DFDL schemas for X12, heavyweight framework |
| **IMSWeb x12-parser** | Yes | No | BSD | Jan 2023 | Parse-only, built-in 837/270/271 schemas |
| **BerryWorks EDIReader** | Yes | Premium only | GPL v3 | Sep 2022 | GPL license, generation requires paid license |

**Decision:** Use **StAEDI (`io.xlate:staedi:1.25.3`)** for EDI generation and parsing.

**Rationale:**
1. **Correctness** — StAEDI handles ISA fixed-width padding, segment terminators, element separators, and segment counting (SE) automatically. Eliminates an entire class of formatting bugs.
2. **Reuse across apps** — The same library serves generation (837, 270) and parsing (271). One dependency, consistent API.
3. **Loop records as business validation layer** — We keep our transient Java record classes (EDI837Claim, ClaimLoop, etc.) as an intermediate layer between DB entities and EDI output. This lets us run custom business validations (e.g., "claim must have at least one diagnosis", "charge must be positive", "coverage must be active") that are not part of the official EDI specification, before handing data to StAEDI for formatting.
4. **Apache 2.0 license** — No commercial restrictions.
5. **Active maintenance** — 51 releases, latest March 2026.

**Consequences:**
- The `EDI837Generator` uses `EDIStreamWriter` instead of `StringBuilder`. The writer is opened against a `ByteArrayOutputStream`, and the generator calls `writer.writeStartSegment("CLM")`, `writer.writeElement(...)`, `writer.writeEndSegment()` etc.
- StAEDI is added as a dependency in the `common` module (shared by all apps).
- Developers need to understand the StAEDI streaming API (similar to StAX for XML).

```groovy
// common/build.gradle
implementation 'io.xlate:staedi:1.25.3'
```

## Architecture

```
POST /api/claims/generate {encounterId}
    │
    ▼
ClaimsController
    │
    ▼
ClaimsService (orchestrates DB lookups)
    │
    ├── Repositories: PatientRepository, PatientInsuranceRepository,
    │   EncounterRepository, EncounterDiagnosisRepository,
    │   EncounterProcedureRepository, PracticeRepository,
    │   ProviderRepository, PayerRepository, FacilityRepository
    │
    ▼
EDI837Mapper (DB entities → loop records)
    │
    ▼
Loop Records (EDI837Claim) ← custom business validation runs here
    │
    ▼
EDI837Generator (loop records → StAEDI EDIStreamWriter → EDI string)
    │
    ▼
Response: downloadable .edi file
```

## Layer 1: Database Entities

All `@Document` classes in `com.example.edi.common.document`, repositories in `com.example.edi.common.repository`.

### Reference Data

See [erd.html](002-claims-generator/erd.html) — open in a browser to view the interactive diagram.

#### Practice

Collection: `practices`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| name | String | Organization name |
| npi | String | 10-digit NPI |
| taxId | String | EIN (REF*EI in 837) |
| taxonomyCode | String | Provider taxonomy |
| address | String | Street address |
| city | String | City |
| state | String | 2-letter state code |
| zipCode | String | ZIP code |
| contactPhone | String | Contact number |

#### Provider

Collection: `providers`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| firstName | String | First name |
| lastName | String | Last name |
| npi | String | Individual NPI |
| taxonomyCode | String | Provider taxonomy |
| practiceId | String | FK to Practice |

#### Payer

Collection: `payers`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| name | String | Payer name (e.g., "Blue Cross Blue Shield") |
| payerId | String | Payer identifier (used in NM1*PR, ISA08) |
| address | String | Street address |
| city | String | City |
| state | String | 2-letter state code |
| zipCode | String | ZIP code |

#### Facility

Collection: `facilities`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| name | String | Location name (e.g., "Sunshine Health - Downtown") |
| practiceId | String | FK to Practice |
| placeOfServiceCode | String | CMS POS code (e.g., "11") |
| address | String | Street address |
| city | String | City |
| state | String | 2-letter state code |
| zipCode | String | ZIP code |
| phone | String | Facility phone |
| npi | String | Facility NPI (optional — some facilities have their own) |

### Patient Data

#### Patient

Collection: `patients`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| firstName | String | First name |
| lastName | String | Last name |
| dateOfBirth | LocalDate | Date of birth |
| gender | String | M/F |
| address | String | Street address |
| city | String | City |
| state | String | 2-letter state code |
| zipCode | String | ZIP code |
| phone | String | Phone number |

#### PatientInsurance

Collection: `patient_insurances`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| patientId | String | FK to Patient |
| payerId | String | FK to Payer |
| memberId | String | Insurance member ID |
| groupNumber | String | Group number |
| policyType | String | MC (Medicaid), HM (HMO), BL (BCBS), etc. |
| subscriberRelationship | String | self, spouse, child |
| effectiveDate | LocalDate | Coverage start |
| terminationDate | LocalDate | Coverage end (null if active) |

### Clinical Data

#### Encounter

Collection: `encounters`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| patientId | String | FK to Patient |
| providerId | String | FK to Provider (rendering) |
| practiceId | String | FK to Practice (billing) |
| facilityId | String | FK to Facility (where service was rendered) |
| dateOfService | LocalDate | Service date |
| authorizationNumber | String | Prior auth number (optional) |

#### EncounterDiagnosis

Collection: `encounter_diagnoses`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| encounterId | String | FK to Encounter |
| rank | int | 1 = primary (ABK), 2+ = secondary (ABF) |
| diagnosisCode | String | ICD-10 code (e.g., "J06.9") |
| description | String | Human-readable description |

#### EncounterProcedure

Collection: `encounter_procedures`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| encounterId | String | FK to Encounter |
| lineNumber | int | Service line sequence (1-based) |
| procedureCode | String | CPT code (e.g., "99213") |
| modifiers | List\<String\> | CPT modifiers (e.g., ["25", "59"]) |
| chargeAmount | BigDecimal | Line charge |
| units | int | Number of units |
| unitType | String | Unit type (e.g., "UN") |
| diagnosisPointers | List\<Integer\> | References EncounterDiagnosis.rank values |

### Entities to Remove

- `Company` — replaced by `Practice`
- `Visit` — replaced by `Encounter` + `EncounterDiagnosis` + `EncounterProcedure`
- `PlaceOfService` — replaced by `Facility`

**Note:** `Company`, `Visit`, `PlaceOfService`, and `Patient` (with insurance fields) are also used by insurance-request-app and insurance-response-app. Those apps will need to be updated to use the new entities in a follow-up effort. This spec covers claims-app only.

### Entities to Modify

- `Patient` — remove insurance fields (`insurancePayerId`, `insurancePayerName`, `insuranceGroupNumber`, `memberId`). Add `phone`.

## Layer 2: EDI Segment Loop Records

Pure data holders — no EDI formatting logic, no database access. Implemented as Java records.

### Shared Records (common module)

Package: `com.example.edi.common.edi.loop`

These records are reused across 837, 270, and 271 transactions.

#### InterchangeEnvelope → ISA/IEA

```java
public record InterchangeEnvelope(
    String senderIdQualifier,    // ISA05
    String senderId,             // ISA06
    String receiverIdQualifier,  // ISA07
    String receiverId,           // ISA08
    String date,                 // ISA09 (YYMMDD)
    String time,                 // ISA10 (HHMM)
    String controlNumber,        // ISA13
    String ackRequested,         // ISA14
    String usageIndicator        // ISA15
) {}
```

#### FunctionalGroup → GS/GE

```java
public record FunctionalGroup(
    String senderId,         // GS02
    String receiverId,       // GS03
    String date,             // GS04
    String time,             // GS05
    String controlNumber     // GS06
) {}
```

#### TransactionHeader → ST/BHT

```java
public record TransactionHeader(
    String transactionSetControlNumber,  // ST02
    String referenceId,                  // BHT03
    String creationDate,                 // BHT04
    String creationTime                  // BHT05
) {}
```

#### Submitter → NM1*41, PER

```java
public record Submitter(
    String name,           // NM1*41 org name
    String identifier,     // NM1*41 identifier
    String contactPhone    // PER*IC phone
) {}
```

#### Receiver → NM1*40

```java
public record Receiver(
    String name,        // NM1*40 org name
    String identifier   // NM1*40 identifier
) {}
```

#### BillingProviderLoop → HL*20, NM1*85, N3, N4, REF*EI

```java
public record BillingProviderLoop(
    String name,
    String npi,
    String taxId,
    String address,
    String city,
    String state,
    String zipCode
) {}
```

#### SubscriberLoop → HL*22, SBR, NM1*IL, N3, N4, DMG, NM1*PR

```java
public record SubscriberLoop(
    // SBR
    String subscriberRelationship,  // P=primary
    String groupNumber,
    String policyType,              // MC, HM, BL, etc.
    // NM1*IL - Subscriber
    String lastName,
    String firstName,
    String memberId,
    String address,
    String city,
    String state,
    String zipCode,
    // DMG
    String dateOfBirth,   // YYYYMMDD
    String genderCode,    // M/F/U
    // NM1*PR - Payer
    String payerName,
    String payerId
) {}
```

### 837-Specific Records (claims-app)

Package: `com.example.edi.claims.domain.loop`

#### EDI837Claim (root)

```java
public record EDI837Claim(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    Submitter submitter,
    Receiver receiver,
    BillingProviderLoop billingProvider,
    SubscriberLoop subscriber,
    ClaimLoop claim
) {}
```

#### ClaimLoop → CLM, HI

```java
public record ClaimLoop(
    String claimId,                    // CLM01 (patient control number)
    BigDecimal totalCharge,            // CLM02
    String placeOfServiceCode,         // CLM05 composite
    List<DiagnosisEntry> diagnoses,    // HI segment
    List<ServiceLineLoop> serviceLines // LX/SV1/DTP loops
) {}
```

#### DiagnosisEntry

```java
public record DiagnosisEntry(
    int rank,              // 1=primary (ABK), 2+=secondary (ABF)
    String diagnosisCode   // ICD-10
) {}
```

#### ServiceLineLoop → LX, SV1, DTP

```java
public record ServiceLineLoop(
    int lineNumber,               // LX01
    String procedureCode,         // SV1-1 CPT
    List<String> modifiers,       // SV1-1 composite modifiers
    BigDecimal chargeAmount,      // SV1-2
    int units,                    // SV1-4
    String unitType,              // SV1-3 (UN)
    List<Integer> diagnosisPointers,  // SV1-7 composite
    String dateOfService          // DTP*472 (YYYYMMDD)
) {}
```

## Layer 3: Services

### EDI837Mapper

Location: `com.example.edi.claims.service.EDI837Mapper`

Responsibility: Convert DB entities into an `EDI837Claim` object.

```
Input:  Practice, Provider, Patient, PatientInsurance, Payer,
        Encounter, List<EncounterDiagnosis>, List<EncounterProcedure>,
        Facility, InterchangeProperties (from YAML config)

Output: EDI837Claim
```

Key mapping logic:
- Generates control numbers (ISA13, GS06, ST02)
- Maps `Patient.gender` to EDI gender code (M/F/U)
- Formats dates to YYYYMMDD / YYMMDD
- Calculates `ClaimLoop.totalCharge` by summing `EncounterProcedure.chargeAmount`
- Maps `EncounterDiagnosis.rank` to `DiagnosisEntry`
- Maps `EncounterProcedure` to `ServiceLineLoop`

### EDI837Generator

Location: `com.example.edi.claims.service.EDI837Generator`

Responsibility: Convert an `EDI837Claim` into a valid EDI 837P 5010 string using StAEDI's `EDIStreamWriter`.

```
Input:  EDI837Claim
Output: String (EDI content)
```

Key logic:
- Uses `EDIOutputFactory.newFactory()` to create an `EDIStreamWriter` backed by a `ByteArrayOutputStream`
- Writes the interchange envelope via `writer.writeStartInterchange()` — StAEDI handles ISA fixed-width padding, separators, and terminators automatically
- Writes functional group, transaction set, and segments using `writer.writeStartSegment("CLM")`, `writer.writeElement(value)`, `writer.writeEndSegment()`
- StAEDI tracks segment count for SE and control numbers for GE/IEA automatically
- Produces segments in correct 837P order

### ClaimsService

Location: `com.example.edi.claims.service.ClaimsService`

Responsibility: Orchestrate the full claim generation flow.

```
Input:  encounterId
Flow:   1. Look up Encounter by ID
        2. Look up Patient from encounter.patientId
        3. Look up active PatientInsurance for patient
        4. Look up Payer from PatientInsurance.payerId
        5. Look up EncounterDiagnosis records by encounterId
        6. Look up EncounterProcedure records by encounterId
        7. Look up Practice from encounter.practiceId
        8. Look up Provider from encounter.providerId
        9. Look up Facility from encounter.facilityId
       10. Call EDI837Mapper to build EDI837Claim
       11. Call EDI837Generator to produce EDI string
Output: EDI string (returned as downloadable .edi file)
```

## Application Configuration

### application.yml (claims-app)

```yaml
edi:
  interchange:
    sender-id-qualifier: ZZ
    sender-id: CLEARINGHOUSE01
    receiver-id-qualifier: ZZ
    receiver-id: PAYERID001
    ack-requested: "0"
    usage-indicator: T
```

### InterchangeProperties

Location: `com.example.edi.claims.config.InterchangeProperties`

```java
@ConfigurationProperties(prefix = "edi.interchange")
public record InterchangeProperties(
    String senderIdQualifier,
    String senderId,
    String receiverIdQualifier,
    String receiverId,
    String ackRequested,
    String usageIndicator
) {}
```

Enabled via `@EnableConfigurationProperties(InterchangeProperties.class)` in the app or config class.

## Seed Endpoint

### POST /api/dev/seed

Location: `com.example.edi.claims.controller.DevSeedController`

Creates sample data:
- 1 Practice: "Sunshine Health Clinic", NPI 1234567890, Tax ID 59-1234567
- 2 Providers: Dr. Sarah Johnson (Internal Medicine), Dr. Michael Chen (Family Medicine)
- 1 Payer: Blue Cross Blue Shield, payer ID BCBS12345
- 3 Facilities: "Main Office" (POS 11), "Outpatient Center" (POS 22), "City Hospital ER" (POS 23)
- 2 Patients with active PatientInsurance records
- 2 Encounters, each with 2 EncounterDiagnosis and 2 EncounterProcedure records

Returns JSON summary of created entity IDs. Checks for existing data to remain idempotent.

## Testing Strategy

### TDD Approach — Tests First

All tests written before implementation. Testcontainers for MongoDB in integration tests.

#### Unit Tests

**EDI837GeneratorTest**
- Given a hand-built `EDI837Claim`, verify:
  - ISA segment has correct fixed-width padding
  - GS segment has correct version reference (005010X222A1)
  - ST/SE segment count matches
  - HI segment uses ABK for primary, ABF for secondary diagnoses
  - SV1 composite elements are correctly formatted
  - IEA control number matches ISA
  - Full output matches expected EDI string for a known claim

**EDI837MapperTest**
- Given mock DB entities, verify:
  - Practice maps to BillingProviderLoop fields
  - Patient + PatientInsurance + Payer map to SubscriberLoop
  - EncounterDiagnosis list maps to ClaimLoop.diagnoses with correct ranks
  - EncounterProcedure list maps to ClaimLoop.serviceLines
  - Total charge is calculated correctly
  - Date formatting is correct (YYYYMMDD, YYMMDD)
  - Gender mapping works (M→M, F→F, null→U)

#### Integration Tests (Testcontainers)

**ClaimsGenerationIT**
- Seed MongoDB with test data via repositories
- POST `/api/claims/generate` with `{patientId, dateOfService}`
- Verify response is a downloadable `.edi` file
- Parse the response and verify key segments (ISA, ST, CLM, HI, SV1, SE, IEA)

**DevSeedControllerIT**
- POST `/api/dev/seed`
- Verify response contains created entity IDs
- Verify data exists in MongoDB collections
- Call seed again, verify idempotent (no duplicates)

### Dependencies

```groovy
// common/build.gradle
implementation 'io.xlate:staedi:1.25.3'

// claims-app/build.gradle
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:mongodb'
testImplementation 'org.testcontainers:junit-jupiter'
```

## Files to Create

### common module — documents and repositories
- `common/src/main/java/com/example/edi/common/document/Practice.java`
- `common/src/main/java/com/example/edi/common/document/Provider.java`
- `common/src/main/java/com/example/edi/common/document/Payer.java`
- `common/src/main/java/com/example/edi/common/document/Facility.java`
- `common/src/main/java/com/example/edi/common/document/PatientInsurance.java`
- `common/src/main/java/com/example/edi/common/document/Encounter.java`
- `common/src/main/java/com/example/edi/common/document/EncounterDiagnosis.java`
- `common/src/main/java/com/example/edi/common/document/EncounterProcedure.java`
- `common/src/main/java/com/example/edi/common/repository/PracticeRepository.java`
- `common/src/main/java/com/example/edi/common/repository/ProviderRepository.java`
- `common/src/main/java/com/example/edi/common/repository/PayerRepository.java`
- `common/src/main/java/com/example/edi/common/repository/FacilityRepository.java`
- `common/src/main/java/com/example/edi/common/repository/PatientInsuranceRepository.java`
- `common/src/main/java/com/example/edi/common/repository/EncounterRepository.java`
- `common/src/main/java/com/example/edi/common/repository/EncounterDiagnosisRepository.java`
- `common/src/main/java/com/example/edi/common/repository/EncounterProcedureRepository.java`

### common module — shared EDI loop records
- `common/src/main/java/com/example/edi/common/edi/loop/InterchangeEnvelope.java`
- `common/src/main/java/com/example/edi/common/edi/loop/FunctionalGroup.java`
- `common/src/main/java/com/example/edi/common/edi/loop/TransactionHeader.java`
- `common/src/main/java/com/example/edi/common/edi/loop/Submitter.java`
- `common/src/main/java/com/example/edi/common/edi/loop/Receiver.java`
- `common/src/main/java/com/example/edi/common/edi/loop/BillingProviderLoop.java`
- `common/src/main/java/com/example/edi/common/edi/loop/SubscriberLoop.java`

### claims-app — 837-specific loop records and services
- `claims-app/src/main/java/com/example/edi/claims/config/InterchangeProperties.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/EDI837Claim.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/ClaimLoop.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/DiagnosisEntry.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/ServiceLineLoop.java`
- `claims-app/src/main/java/com/example/edi/claims/service/EDI837Mapper.java`
- `claims-app/src/main/java/com/example/edi/claims/service/EDI837Generator.java`
- `claims-app/src/main/java/com/example/edi/claims/controller/DevSeedController.java`

### Files to Modify
- `claims-app/src/main/java/com/example/edi/claims/service/ClaimsService.java` — rewrite to use new entities and services
- `claims-app/src/main/java/com/example/edi/claims/controller/ClaimsController.java` — update if DTO changes
- `claims-app/src/main/resources/application.yml` — add `edi.interchange` config
- `claims-app/build.gradle` — add Testcontainers dependencies
- `common/build.gradle` — add StAEDI dependency
- `common/src/main/java/com/example/edi/common/document/Patient.java` — remove insurance fields, add phone

### Files to Remove
- `common/src/main/java/com/example/edi/common/document/Company.java`
- `common/src/main/java/com/example/edi/common/document/Visit.java`
- `common/src/main/java/com/example/edi/common/repository/CompanyRepository.java`
- `common/src/main/java/com/example/edi/common/document/PlaceOfService.java`
- `common/src/main/java/com/example/edi/common/repository/VisitRepository.java`
- `common/src/main/java/com/example/edi/common/repository/PlaceOfServiceRepository.java`
- `claims-app/src/main/java/com/example/edi/claims/service/EDI837Service.java` — replaced by EDI837Mapper + EDI837Generator

### Test Files to Create
- `claims-app/src/test/java/com/example/edi/claims/service/EDI837GeneratorTest.java`
- `claims-app/src/test/java/com/example/edi/claims/service/EDI837MapperTest.java`
- `claims-app/src/test/java/com/example/edi/claims/ClaimsGenerationIT.java`
- `claims-app/src/test/java/com/example/edi/claims/DevSeedControllerIT.java`

## Verification

1. `./gradlew :claims-app:test` — all unit and integration tests pass
2. Start MongoDB via `docker-compose up -d`
3. `./gradlew :claims-app:bootRun`
4. `curl -X POST http://localhost:8080/api/dev/seed` — returns JSON summary
5. `curl -X POST http://localhost:8080/api/claims/generate -H "Content-Type: application/json" -d '{"encounterId":"<id>"}'` — returns downloadable .edi file
6. Validate the .edi output contains correct ISA, GS, ST, CLM, HI, SV1, SE, GE, IEA segments
