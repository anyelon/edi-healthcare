# EDI 837P Claim Generation — Design Spec

## Context

The claims-app currently generates EDI 837P files using hardcoded string concatenation in `EDI837Service`, pulling from loosely structured entities (`Company`, `Patient`, `Visit`, `PlaceOfService`). This design has several problems:

- No separation between clinical data concerns (diagnoses vs procedures are embedded in `Visit`)
- Insurance data is embedded in `Patient` instead of being its own entity
- No distinction between billing provider (organization) and rendering provider (practitioner)
- EDI generation logic, data mapping, and formatting are all tangled in one method
- ISA envelope values are hardcoded rather than configurable

This redesign introduces a proper data model aligned with 837P requirements, a two-layer architecture (DB entities + 837 loop classes), and a centralized generator.

## Architecture

```
POST /api/claims/generate {patientId, dateOfService}
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
    │   ProviderRepository, PayerRepository, PlaceOfServiceRepository
    │
    ▼
EDI837Mapper (DB entities → loop classes)
    │
    ▼
EDI837Generator (loop classes → EDI string)
    │
    ▼
Response: downloadable .edi file
```

## Layer 1: Database Entities

All `@Document` classes in `com.example.edi.common.document`, repositories in `com.example.edi.common.repository`.

### Reference Data

#### Practice (replaces Company)

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

#### PlaceOfService (existing, unchanged)

Collection: `places_of_service`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| code | String | POS code (e.g., "11") |
| description | String | Description (e.g., "Office") |

### Patient Data

#### Patient (simplified — insurance fields removed)

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

#### Encounter (replaces Visit)

Collection: `encounters`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| patientId | String | FK to Patient |
| providerId | String | FK to Provider (rendering) |
| practiceId | String | FK to Practice (billing) |
| placeOfServiceCode | String | POS code (e.g., "11") |
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

**Note:** `Company`, `Visit`, and `Patient` (with insurance fields) are also used by insurance-request-app and insurance-response-app. Those apps will need to be updated to use the new entities in a follow-up effort. This spec covers claims-app only.

### Entities to Modify

- `Patient` — remove insurance fields (`insurancePayerId`, `insurancePayerName`, `insuranceGroupNumber`, `memberId`). Add `phone`.

## Layer 2: 837 Segment Loop Classes

Transient Java classes in `com.example.edi.claims.domain.loop`. These are **pure data holders** — no EDI formatting logic, no database access. Implemented as Java records.

### EDI837Claim (root)

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

### InterchangeEnvelope → ISA/IEA

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

### FunctionalGroup → GS/GE

```java
public record FunctionalGroup(
    String senderId,         // GS02
    String receiverId,       // GS03
    String date,             // GS04
    String time,             // GS05
    String controlNumber     // GS06
) {}
```

### TransactionHeader → ST/BHT

```java
public record TransactionHeader(
    String transactionSetControlNumber,  // ST02
    String referenceId,                  // BHT03
    String creationDate,                 // BHT04
    String creationTime                  // BHT05
) {}
```

### Submitter → NM1*41, PER

```java
public record Submitter(
    String name,           // NM1*41 org name
    String identifier,     // NM1*41 identifier
    String contactPhone    // PER*IC phone
) {}
```

### Receiver → NM1*40

```java
public record Receiver(
    String name,        // NM1*40 org name
    String identifier   // NM1*40 identifier
) {}
```

### BillingProviderLoop → HL*20, NM1*85, N3, N4, REF*EI

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

### SubscriberLoop → HL*22, SBR, NM1*IL, N3, N4, DMG, NM1*PR

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

### ClaimLoop → CLM, HI

```java
public record ClaimLoop(
    String claimId,                    // CLM01 (patient control number)
    BigDecimal totalCharge,            // CLM02
    String placeOfServiceCode,         // CLM05 composite
    List<DiagnosisEntry> diagnoses,    // HI segment
    List<ServiceLineLoop> serviceLines // LX/SV1/DTP loops
) {}
```

### DiagnosisEntry

```java
public record DiagnosisEntry(
    int rank,              // 1=primary (ABK), 2+=secondary (ABF)
    String diagnosisCode   // ICD-10
) {}
```

### ServiceLineLoop → LX, SV1, DTP

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
        PlaceOfService, InterchangeProperties (from YAML config)

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

Responsibility: Convert an `EDI837Claim` into a valid EDI 837P 5010 string.

```
Input:  EDI837Claim
Output: String (EDI content)
```

Key logic:
- Manages segment separators (`*`), sub-element separators (`:`), terminators (`~`)
- Pads ISA fields to fixed lengths
- Tracks segment count for SE trailer
- Produces segments in correct 837P order

### ClaimsService

Location: `com.example.edi.claims.service.ClaimsService`

Responsibility: Orchestrate the full claim generation flow.

```
Input:  patientId, dateOfService
Flow:   1. Look up Patient by ID
        2. Look up active PatientInsurance for patient
        3. Look up Payer from PatientInsurance.payerId
        4. Look up Encounter by patientId + dateOfService
        5. Look up EncounterDiagnosis records by encounterId
        6. Look up EncounterProcedure records by encounterId
        7. Look up Practice from encounter.practiceId
        8. Look up Provider from encounter.providerId
        9. Look up PlaceOfService by encounter.placeOfServiceCode
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
- 3 PlaceOfService: 11 (Office), 22 (Outpatient Hospital), 23 (Emergency Room)
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

### Test Dependencies

```groovy
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:mongodb'
testImplementation 'org.testcontainers:junit-jupiter'
```

## Files to Create

### common module
- `common/src/main/java/com/example/edi/common/document/Practice.java`
- `common/src/main/java/com/example/edi/common/document/Provider.java`
- `common/src/main/java/com/example/edi/common/document/Payer.java`
- `common/src/main/java/com/example/edi/common/document/PatientInsurance.java`
- `common/src/main/java/com/example/edi/common/document/Encounter.java`
- `common/src/main/java/com/example/edi/common/document/EncounterDiagnosis.java`
- `common/src/main/java/com/example/edi/common/document/EncounterProcedure.java`
- `common/src/main/java/com/example/edi/common/repository/PracticeRepository.java`
- `common/src/main/java/com/example/edi/common/repository/ProviderRepository.java`
- `common/src/main/java/com/example/edi/common/repository/PayerRepository.java`
- `common/src/main/java/com/example/edi/common/repository/PatientInsuranceRepository.java`
- `common/src/main/java/com/example/edi/common/repository/EncounterRepository.java`
- `common/src/main/java/com/example/edi/common/repository/EncounterDiagnosisRepository.java`
- `common/src/main/java/com/example/edi/common/repository/EncounterProcedureRepository.java`

### claims-app
- `claims-app/src/main/java/com/example/edi/claims/config/InterchangeProperties.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/EDI837Claim.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/InterchangeEnvelope.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/FunctionalGroup.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/TransactionHeader.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/Submitter.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/Receiver.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/BillingProviderLoop.java`
- `claims-app/src/main/java/com/example/edi/claims/domain/loop/SubscriberLoop.java`
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
- `common/src/main/java/com/example/edi/common/document/Patient.java` — remove insurance fields, add phone

### Files to Remove
- `common/src/main/java/com/example/edi/common/document/Company.java`
- `common/src/main/java/com/example/edi/common/document/Visit.java`
- `common/src/main/java/com/example/edi/common/repository/CompanyRepository.java`
- `common/src/main/java/com/example/edi/common/repository/VisitRepository.java`
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
5. `curl -X POST http://localhost:8080/api/claims/generate -H "Content-Type: application/json" -d '{"patientId":"<id>","dateOfService":"2026-03-15"}'` — returns downloadable .edi file
6. Validate the .edi output contains correct ISA, GS, ST, CLM, HI, SV1, SE, GE, IEA segments
