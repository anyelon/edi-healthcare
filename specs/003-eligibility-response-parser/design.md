# EDI 271 Eligibility Response Parser — Design Spec

## Context

Build a service that parses uploaded EDI 271 eligibility response files, extracts subscriber identification and benefit details, archives the original file, and stores the parsed results as a self-contained MongoDB document. This replaces the existing boilerplate `EDI271Service` which only extracts 3 fields via manual string splitting.

## ADR-001: Use StAEDI for 271 parsing

**Status:** Accepted

**Decision:** Use StAEDI (`io.xlate:staedi:1.25.3`) for parsing EDI 271 files, consistent with the 837 generator in spec-002.

**Rationale:**
1. StAEDI is already a dependency in the common module
2. `EDIStreamReader` provides streaming segment-by-segment parsing with proper handling of composites, sub-elements, and envelope validation
3. Consistent approach across all EDI transaction types (837 generation, 271 parsing)
4. Replaces fragile manual string splitting

## Architecture

```
POST /api/insurance/eligibility-response (multipart file upload)
    |
    v
EligibilityResponseController
    |
    v
EligibilityResponseService (orchestrates file handling + parsing)
    |
    |-- 1. Copy uploaded file to archive directory
    |
    |-- 2. EDI271Parser (StAEDI EDIStreamReader -> loop records)
    |       |
    |       v
    |    EDI271Response (transient loop records)
    |
    |-- 3. EDI271Mapper (loop records -> EligibilityResponse document)
    |
    +-- 4. EligibilityResponseRepository.save()
    |
    v
Response: JSON EligibilityResponse
```

### Error Flow

On parse failure (malformed file, wrong transaction type, missing required segments):
1. Archive the file (same as success path)
2. Save an `EligibilityResponse` document with `status=ERROR` and `errorMessage` describing the failure
3. Return the ERROR document as JSON (HTTP 200 — the request was processed, the file was just invalid)

## Layer 1: Database Entities

### EligibilityResponse (new, common module)

Collection: `eligibility_responses`

| Field | Type | Description |
|-------|------|-------------|
| id | String | MongoDB ID |
| status | String | COMPLETED or ERROR |
| errorMessage | String | Error details if status=ERROR, null otherwise |
| filePath | String | Path to archived 271 file |
| receivedAt | LocalDateTime | When the file was uploaded |
| payerName | String | Payer name from NM1*PR |
| payerId | String | Payer identifier from NM1*PR |
| subscriberFirstName | String | From NM1*IL |
| subscriberLastName | String | From NM1*IL |
| memberId | String | Member ID from NM1*IL element 9 |
| groupNumber | String | Subscriber group number |
| eligibilityStatus | String | ACTIVE, INACTIVE, UNKNOWN |
| coverageStartDate | LocalDate | From DTP*346 (plan begin) |
| coverageEndDate | LocalDate | From DTP*347 (plan end) |
| benefits | List\<BenefitDetail\> | Embedded benefit entries from EB segments |

### BenefitDetail (embedded object, not a separate collection)

| Field | Type | Description |
|-------|------|-------------|
| benefitType | String | Deductible, Copayment, Coinsurance, Out of Pocket, etc. (mapped from EB01) |
| coverageLevel | String | IND (individual), FAM (family), EMP (employee only) (from EB02) |
| serviceType | String | Health Benefit Plan Coverage, Mental Health, Surgical, etc. (from EB03) |
| inNetwork | Boolean | true if in-network, false if out-of-network (from EB06 or context) |
| amount | BigDecimal | Dollar amount from EB07 (when EB05 indicates currency) |
| percent | BigDecimal | Percentage from EB07 (when EB05 indicates percent) |
| timePeriod | String | Calendar Year, Visit, Service Year, etc. (from EB06) |
| message | String | Additional info from associated MSG segment |

### Repository

```java
public interface EligibilityResponseRepository extends MongoRepository<EligibilityResponse, String> {
    List<EligibilityResponse> findByMemberIdOrderByReceivedAtDesc(String memberId);
}
```

## Layer 2: 271 Loop Records (transient)

### Shared Records (reused from common module)

- `InterchangeEnvelope` — ISA/IEA data
- `FunctionalGroup` — GS/GE data
- `TransactionHeader` — ST/BHT data

### 271-Specific Records (insurance-response-app)

Package: `com.example.edi.insuranceresponse.domain.loop`

#### EDI271Response (root)

```java
public record EDI271Response(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    SubscriberInfo subscriberInfo,
    PayerInfo payerInfo,
    List<BenefitInfo> benefits
) {}
```

#### SubscriberInfo

```java
public record SubscriberInfo(
    String firstName,
    String lastName,
    String memberId,
    String groupNumber,
    String eligibilityStatus
) {}
```

#### PayerInfo

```java
public record PayerInfo(
    String name,
    String payerId
) {}
```

#### BenefitInfo

```java
public record BenefitInfo(
    String benefitType,
    String coverageLevel,
    String serviceType,
    boolean inNetwork,
    BigDecimal amount,
    BigDecimal percent,
    String timePeriod,
    String message,
    LocalDate coverageStartDate,
    LocalDate coverageEndDate
) {}
```

## Layer 3: Services

### EDI271Parser

Location: `com.example.edi.insuranceresponse.service.EDI271Parser`

Responsibility: Parse a 271 file into an `EDI271Response` using StAEDI's `EDIStreamReader`.

```
Input:  Path to 271 file
Output: EDI271Response (loop records)
Throws: RuntimeException on parse failure
```

Key parsing logic:
- Open file with `EDIInputFactory.newFactory().createEDIStreamReader()`
- Read ISA → `InterchangeEnvelope`
- Read GS → `FunctionalGroup`
- Read ST → `TransactionHeader`
- Track current segment context to associate EB/DTP/MSG segments with the correct subscriber
- Extract NM1*IL → `SubscriberInfo` (firstName, lastName, memberId from elements 3, 4, 9)
- Extract NM1*PR → `PayerInfo` (name, payerId from elements 3, 9)
- Extract EB segments → `BenefitInfo` (benefitType from EB01, coverageLevel from EB02, serviceType from EB03, amount/percent from EB07, timePeriod from EB06)
- Extract DTP segments → coverage dates (DTP*346 = plan begin, DTP*347 = plan end)
- Extract MSG segments → associate message text with preceding EB benefit
- Map EB01 eligibility codes: 1 → ACTIVE, 6 → INACTIVE, 8 → INACTIVE, default → UNKNOWN

### EDI271Mapper

Location: `com.example.edi.insuranceresponse.service.EDI271Mapper`

Responsibility: Convert `EDI271Response` loop records into an `EligibilityResponse` MongoDB document.

```
Input:  EDI271Response, filePath, receivedAt
Output: EligibilityResponse (status=COMPLETED)
```

Key mapping:
- Subscriber fields from `SubscriberInfo`
- Payer fields from `PayerInfo`
- Benefits list: map each `BenefitInfo` → `BenefitDetail` embedded object
- Coverage dates from the first benefit's dates (or DTP at subscriber level)
- Set `status = "COMPLETED"`

### EligibilityResponseService

Location: `com.example.edi.insuranceresponse.service.EligibilityResponseService`

Responsibility: Orchestrate the full parse flow.

```
Input:  MultipartFile
Flow:   1. Generate unique filename (timestamp + original name)
        2. Copy file to archive directory (edi.archive.path from config)
        3. Try: parse with EDI271Parser -> map with EDI271Mapper -> save
        4. Catch: save ERROR document with errorMessage, filePath, receivedAt
        5. Return saved EligibilityResponse
Output: EligibilityResponse
```

## Application Configuration

### application.yml (insurance-response-app)

Add to existing config:

```yaml
edi:
  archive:
    path: edi/271/incoming

server:
  tomcat:
    max-http-header-size: 128KB
```

### ArchiveProperties

Location: `com.example.edi.insuranceresponse.config.ArchiveProperties`

```java
@ConfigurationProperties(prefix = "edi.archive")
public record ArchiveProperties(
    String path
) {}
```

## Testing Strategy

### TDD — Tests First

#### Unit Tests

**EDI271ParserTest**
- Given a valid 271 file string with ISA/GS/ST/NM1*IL/NM1*PR/EB/DTP/MSG segments:
  - Extracts correct subscriber info (name, memberId)
  - Extracts correct payer info (name, payerId)
  - Maps EB01 code 1 → ACTIVE eligibility status
  - Maps EB01 code 6 → INACTIVE
  - Extracts benefit details (type, amount, coverage level, service type)
  - Extracts coverage dates from DTP*346/347
  - Associates MSG text with preceding EB benefit
- Given a malformed file: throws RuntimeException

**EDI271MapperTest**
- Given EDI271Response with subscriber, payer, and benefits:
  - Maps to EligibilityResponse with COMPLETED status
  - Subscriber fields match
  - Benefits list maps correctly
  - Coverage dates set
  - filePath and receivedAt set

**EligibilityResponseServiceTest**
- Given valid file upload: archives file, parses, saves COMPLETED document
- Given malformed file: archives file, saves ERROR document with error message

#### Integration Tests (Testcontainers)

**EligibilityResponseIT**
- Upload valid 271 file → 200, response contains COMPLETED status, memberId, benefits
- Upload malformed file → 200, response contains ERROR status, errorMessage
- Verify document stored in MongoDB
- Verify file archived to configured path

### Dependencies

```groovy
// insurance-response-app/build.gradle (add to existing)
implementation 'io.xlate:staedi:1.25.3'

testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:testcontainers-mongodb:2.0.4'
testImplementation 'org.testcontainers:testcontainers-junit-jupiter:2.0.4'
```

## Files to Create

### common module
- `common/src/main/java/com/example/edi/common/document/EligibilityResponse.java`
- `common/src/main/java/com/example/edi/common/document/BenefitDetail.java`
- `common/src/main/java/com/example/edi/common/repository/EligibilityResponseRepository.java`

### insurance-response-app — 271-specific loop records
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/EDI271Response.java`
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/SubscriberInfo.java`
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/PayerInfo.java`
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/BenefitInfo.java`

### insurance-response-app — services and config
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/config/ArchiveProperties.java`
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Parser.java`
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Mapper.java`

### Files to Modify
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EligibilityResponseService.java` — rewrite with archive + parse + map + save flow
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/controller/InsuranceResponseController.java` — return EligibilityResponse instead of VerificationResult
- `insurance-response-app/src/main/resources/application.yml` — add edi.archive.path and tomcat header size
- `insurance-response-app/build.gradle` — add StAEDI and Testcontainers dependencies
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/config/MongoConfig.java` — add @EnableConfigurationProperties

### Files to Remove
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Service.java` — replaced by EDI271Parser
- `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/dto/VerificationResult.java` — replaced by EligibilityResponse entity

### Test Files to Create
- `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271ParserTest.java`
- `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271MapperTest.java`
- `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/EligibilityResponseIT.java`

### Test Files to Modify
- `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/controller/InsuranceResponseControllerTest.java` — update for new return type
- `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/InsuranceResponseServiceTest.java` — rewrite for new flow

### Test Files to Remove
- `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271ServiceTest.java` — replaced by EDI271ParserTest

## Verification

1. `./gradlew :insurance-response-app:test` — all unit and integration tests pass
2. Start MongoDB via `docker-compose up -d`
3. `./gradlew :insurance-response-app:bootRun`
4. Upload a sample 271 file:
   ```bash
   curl -X POST http://localhost:8082/api/insurance/eligibility-response \
     -F "file=@sample-271.edi"
   ```
5. Verify response contains COMPLETED status, subscriber info, and benefits list
6. Verify document stored in MongoDB `eligibility_responses` collection
7. Verify file archived in `edi/271/incoming/` directory
8. Upload a malformed file — verify ERROR response and document stored
