# EDI 271 Eligibility Response Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Parse uploaded EDI 271 files with StAEDI, extract subscriber and benefit details, archive files, and store results in MongoDB.

**Architecture:** File upload → archive to disk → StAEDI streaming parse into loop records → map to MongoDB document → save. Errors stored with ERROR status for audit trail.

**Tech Stack:** Spring Boot 4.0.4, Java 21, MongoDB, StAEDI 1.25.3, Testcontainers, JUnit 5

**Spec:** `specs/003-eligibility-response-parser/design.md`

---

### Task 1: Add dependencies to insurance-response-app

**Files:**
- Modify: `insurance-response-app/build.gradle`

- [ ] **Step 1: Add StAEDI and Testcontainers dependencies**

Add to the dependencies block:

```groovy
implementation 'io.xlate:staedi:1.25.3'

testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:testcontainers-mongodb:2.0.4'
testImplementation 'org.testcontainers:testcontainers-junit-jupiter:2.0.4'
```

Add Rancher Desktop Docker socket detection (same as claims-app):

```groovy
tasks.withType(Test).configureEach {
    def dockerHost = System.getenv('DOCKER_HOST')
    if (dockerHost == null) {
        def rdSocket = file("${System.getenv('HOME')}/.rd/docker.sock")
        if (rdSocket.exists()) {
            environment 'DOCKER_HOST', "unix://${rdSocket.absolutePath}"
            environment 'TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE', rdSocket.absolutePath
            environment 'TESTCONTAINERS_RYUK_DISABLED', 'true'
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :insurance-response-app:compileTestJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add insurance-response-app/build.gradle
git commit -m "build: add StAEDI and Testcontainers to insurance-response-app"
```

---

### Task 2: Create EligibilityResponse and BenefitDetail entities in common module

**Files:**
- Create: `common/src/main/java/com/example/edi/common/document/EligibilityResponse.java`
- Create: `common/src/main/java/com/example/edi/common/document/BenefitDetail.java`
- Create: `common/src/main/java/com/example/edi/common/repository/EligibilityResponseRepository.java`

- [ ] **Step 1: Create BenefitDetail.java (embedded object, no @Document)**

```java
package com.example.edi.common.document;

import java.math.BigDecimal;

public class BenefitDetail {

    private String benefitType;
    private String coverageLevel;
    private String serviceType;
    private Boolean inNetwork;
    private BigDecimal amount;
    private BigDecimal percent;
    private String timePeriod;
    private String message;

    public BenefitDetail() {}

    public String getBenefitType() { return benefitType; }
    public void setBenefitType(String benefitType) { this.benefitType = benefitType; }
    public String getCoverageLevel() { return coverageLevel; }
    public void setCoverageLevel(String coverageLevel) { this.coverageLevel = coverageLevel; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public Boolean getInNetwork() { return inNetwork; }
    public void setInNetwork(Boolean inNetwork) { this.inNetwork = inNetwork; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }
    public String getTimePeriod() { return timePeriod; }
    public void setTimePeriod(String timePeriod) { this.timePeriod = timePeriod; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

- [ ] **Step 2: Create EligibilityResponse.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "eligibility_responses")
public class EligibilityResponse {

    @Id
    private String id;
    private String status;
    private String errorMessage;
    private String filePath;
    private LocalDateTime receivedAt;
    private String payerName;
    private String payerId;
    private String subscriberFirstName;
    private String subscriberLastName;
    private String memberId;
    private String groupNumber;
    private String eligibilityStatus;
    private LocalDate coverageStartDate;
    private LocalDate coverageEndDate;
    private List<BenefitDetail> benefits;

    public EligibilityResponse() {}

    // All getters and setters for each field
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getSubscriberFirstName() { return subscriberFirstName; }
    public void setSubscriberFirstName(String subscriberFirstName) { this.subscriberFirstName = subscriberFirstName; }
    public String getSubscriberLastName() { return subscriberLastName; }
    public void setSubscriberLastName(String subscriberLastName) { this.subscriberLastName = subscriberLastName; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }
    public String getEligibilityStatus() { return eligibilityStatus; }
    public void setEligibilityStatus(String eligibilityStatus) { this.eligibilityStatus = eligibilityStatus; }
    public LocalDate getCoverageStartDate() { return coverageStartDate; }
    public void setCoverageStartDate(LocalDate coverageStartDate) { this.coverageStartDate = coverageStartDate; }
    public LocalDate getCoverageEndDate() { return coverageEndDate; }
    public void setCoverageEndDate(LocalDate coverageEndDate) { this.coverageEndDate = coverageEndDate; }
    public List<BenefitDetail> getBenefits() { return benefits; }
    public void setBenefits(List<BenefitDetail> benefits) { this.benefits = benefits; }
}
```

- [ ] **Step 3: Create EligibilityResponseRepository.java**

```java
package com.example.edi.common.repository;

import com.example.edi.common.document.EligibilityResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EligibilityResponseRepository extends MongoRepository<EligibilityResponse, String> {
    List<EligibilityResponse> findByMemberIdOrderByReceivedAtDesc(String memberId);
}
```

- [ ] **Step 4: Verify common module compiles**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/example/edi/common/document/EligibilityResponse.java common/src/main/java/com/example/edi/common/document/BenefitDetail.java common/src/main/java/com/example/edi/common/repository/EligibilityResponseRepository.java
git commit -m "feat: add EligibilityResponse and BenefitDetail entities"
```

---

### Task 3: Create 271 loop records and ArchiveProperties config

**Files:**
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/SubscriberInfo.java`
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/PayerInfo.java`
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/BenefitInfo.java`
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/EDI271Response.java`
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/config/ArchiveProperties.java`
- Modify: `insurance-response-app/src/main/resources/application.yml`
- Modify: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/config/MongoConfig.java`

- [ ] **Step 1: Create all 271 loop records**

Create these 4 files as Java records in `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/loop/`:

`SubscriberInfo.java`:
```java
package com.example.edi.insuranceresponse.domain.loop;

public record SubscriberInfo(
    String firstName, String lastName, String memberId,
    String groupNumber, String eligibilityStatus
) {}
```

`PayerInfo.java`:
```java
package com.example.edi.insuranceresponse.domain.loop;

public record PayerInfo(String name, String payerId) {}
```

`BenefitInfo.java`:
```java
package com.example.edi.insuranceresponse.domain.loop;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BenefitInfo(
    String benefitType, String coverageLevel, String serviceType,
    boolean inNetwork, BigDecimal amount, BigDecimal percent,
    String timePeriod, String message,
    LocalDate coverageStartDate, LocalDate coverageEndDate
) {}
```

`EDI271Response.java`:
```java
package com.example.edi.insuranceresponse.domain.loop;

import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.TransactionHeader;
import java.util.List;

public record EDI271Response(
    InterchangeEnvelope envelope, FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader, SubscriberInfo subscriberInfo,
    PayerInfo payerInfo, List<BenefitInfo> benefits
) {}
```

- [ ] **Step 2: Create ArchiveProperties.java**

```java
package com.example.edi.insuranceresponse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.archive")
public record ArchiveProperties(String path) {}
```

- [ ] **Step 3: Update application.yml**

Add to end of existing file:

```yaml

edi:
  archive:
    path: edi/271/incoming

server:
  tomcat:
    max-http-header-size: 128KB
```

Note: `server.port` and `server.tomcat` should be at the same level. Read the file first, add `tomcat` under existing `server:`, and add `edi:` as a new top-level key.

- [ ] **Step 4: Update MongoConfig.java**

Add `@EnableConfigurationProperties(ArchiveProperties.class)` and the import.

- [ ] **Step 5: Verify insurance-response-app compiles**

Run: `./gradlew :insurance-response-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add insurance-response-app/src/main/java/com/example/edi/insuranceresponse/domain/ insurance-response-app/src/main/java/com/example/edi/insuranceresponse/config/ insurance-response-app/src/main/resources/application.yml
git commit -m "feat: add 271 loop records and archive config"
```

---

### Task 4: TDD — EDI271Parser (test first, then implement)

**Files:**
- Create: `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271ParserTest.java`
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Parser.java`

- [ ] **Step 1: Write the failing test**

Create `EDI271ParserTest.java`. Write a valid 271 EDI string as a test fixture, save to a temp file, and test the parser:

Test fixture — a realistic 271 with:
- ISA/GS/ST envelope
- NM1*IL subscriber: SMITH, JOHN, member MEM987654321
- NM1*PR payer: BLUE CROSS BLUE SHIELD, BCBS12345
- EB*1 (active coverage, Health Benefit Plan Coverage, individual)
- EB*C (deductible, individual, $500, calendar year)
- EB*A (copayment, individual, $30, visit)
- DTP*346 (plan begin 20250101)
- DTP*347 (plan end 20251231)
- MSG (additional info)
- SE/GE/IEA trailers

Test methods (7 tests):
1. `parse_extractsSubscriberInfo` — name, memberId
2. `parse_extractsPayerInfo` — name, payerId
3. `parse_mapsActiveEligibility` — EB01=1 → ACTIVE
4. `parse_mapsInactiveEligibility` — EB01=6 → INACTIVE
5. `parse_extractsBenefitDetails` — deductible $500, copay $30
6. `parse_extractsCoverageDates` — 2025-01-01 to 2025-12-31
7. `parse_malformedFile_throwsException` — garbage input throws

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :insurance-response-app:test --tests "com.example.edi.insuranceresponse.service.EDI271ParserTest" 2>&1 | tail -5`
Expected: FAIL — EDI271Parser class does not exist

- [ ] **Step 3: Implement EDI271Parser**

A `@Service` with `EDI271Response parse(Path filePath)`. Uses `EDIInputFactory.newFactory().createEDIStreamReader()`. Reads segments sequentially, building up subscriber info, payer info, and benefits list. Maps EB01: 1→ACTIVE, 6→INACTIVE, 8→INACTIVE, default→UNKNOWN.

Key StAEDI reader API:
```java
var factory = EDIInputFactory.newFactory();
var reader = factory.createEDIStreamReader(new FileInputStream(filePath.toFile()));
while (reader.hasNext()) {
    switch (reader.next()) {
        case SEGMENT_START -> currentSegment = reader.getText();
        case ELEMENT_DATA -> { /* read element by position */ }
        case SEGMENT_END -> { /* process completed segment */ }
    }
}
```

Track element positions within each segment using a counter that resets on SEGMENT_START.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :insurance-response-app:test --tests "com.example.edi.insuranceresponse.service.EDI271ParserTest"`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271ParserTest.java insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Parser.java
git commit -m "feat: add EDI271Parser with StAEDI and TDD tests"
```

---

### Task 5: TDD — EDI271Mapper (test first, then implement)

**Files:**
- Create: `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271MapperTest.java`
- Create: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Mapper.java`

- [ ] **Step 1: Write the failing test**

Build an EDI271Response fixture by hand and test the mapper:

Test methods (5 tests):
1. `map_setsCompletedStatus` — status = "COMPLETED"
2. `map_subscriberFieldsFromSubscriberInfo` — firstName, lastName, memberId, eligibilityStatus
3. `map_payerFieldsFromPayerInfo` — payerName, payerId
4. `map_benefitsListMapped` — benefitType, amount, coverageLevel mapped correctly
5. `map_setsFilePathAndReceivedAt` — filePath and receivedAt set from inputs

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement EDI271Mapper**

A `@Service` with `EligibilityResponse map(EDI271Response response, String filePath, LocalDateTime receivedAt)`. Creates an EligibilityResponse, sets all fields from the loop records, maps BenefitInfo list to BenefitDetail list, sets status=COMPLETED.

- [ ] **Step 4: Run tests to verify they pass**

- [ ] **Step 5: Commit**

```bash
git add insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271MapperTest.java insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Mapper.java
git commit -m "feat: add EDI271Mapper with TDD tests"
```

---

### Task 6: Rewrite EligibilityResponseService, Controller, and update tests

**Files:**
- Modify: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EligibilityResponseService.java`
- Modify: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/controller/InsuranceResponseController.java`
- Modify: `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/controller/InsuranceResponseControllerTest.java`
- Modify: `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/InsuranceResponseServiceTest.java`

- [ ] **Step 1: Rewrite EligibilityResponseService**

Constructor-inject: `EDI271Parser`, `EDI271Mapper`, `EligibilityResponseRepository`, `ArchiveProperties`.

Method: `EligibilityResponse processFile(MultipartFile file)`
1. Generate filename: `System.currentTimeMillis() + "_" + file.getOriginalFilename()`
2. Create archive directory if needed: `Files.createDirectories(Paths.get(archiveProperties.path()))`
3. Copy file: `file.transferTo(archivePath.toFile())`
4. Try: parse with EDI271Parser → map with EDI271Mapper → save → return
5. Catch Exception: create ERROR EligibilityResponse with errorMessage, filePath, receivedAt → save → return

- [ ] **Step 2: Update InsuranceResponseController**

Change return type from `VerificationResult` to `EligibilityResponse`. Call `service.processFile(file)`. Remove temp file handling (service handles archiving now). Return `ResponseEntity.ok(result)` as JSON.

- [ ] **Step 3: Update InsuranceResponseControllerTest**

Mock `EligibilityResponseService.processFile()` returning an `EligibilityResponse`. Test valid upload returns 200 with JSON. Test missing file returns 400.

- [ ] **Step 4: Rewrite InsuranceResponseServiceTest**

Mock EDI271Parser, EDI271Mapper, EligibilityResponseRepository, ArchiveProperties. Test success path (archives, parses, maps, saves). Test error path (archives, saves ERROR document).

- [ ] **Step 5: Verify tests pass**

Run: `./gradlew :insurance-response-app:test`

- [ ] **Step 6: Commit**

```bash
git add insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EligibilityResponseService.java insurance-response-app/src/main/java/com/example/edi/insuranceresponse/controller/InsuranceResponseController.java insurance-response-app/src/test/
git commit -m "feat: rewrite service and controller for 271 parsing with archive flow"
```

---

### Task 7: Clean up old files

**Files:**
- Remove: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Service.java`
- Remove: `insurance-response-app/src/main/java/com/example/edi/insuranceresponse/dto/VerificationResult.java`
- Remove: `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271ServiceTest.java`

- [ ] **Step 1: Delete old files**

```bash
rm insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI271Service.java
rm insurance-response-app/src/main/java/com/example/edi/insuranceresponse/dto/VerificationResult.java
rm insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI271ServiceTest.java
```

- [ ] **Step 2: Verify build and tests pass**

Run: `./gradlew :insurance-response-app:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove old EDI271Service, VerificationResult, and tests"
```

---

### Task 8: Integration tests with Testcontainers

**Files:**
- Create: `insurance-response-app/src/test/java/com/example/edi/insuranceresponse/EligibilityResponseIT.java`

- [ ] **Step 1: Write EligibilityResponseIT**

`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Testcontainers`. Uses `MongoDBContainer("mongo:7")` with `@DynamicPropertySource`. Also set `edi.archive.path` to a `@TempDir` path.

Uses `RestTemplate` + `@LocalServerPort` (not TestRestTemplate — removed in Spring Boot 4).

Test methods:
1. `uploadValidFile_returnsCompletedResponse` — Create a valid 271 EDI string as a temp file, upload via multipart POST, verify response has status=COMPLETED, memberId, benefits list, eligibilityStatus=ACTIVE. Verify document in MongoDB via repository.
2. `uploadMalformedFile_returnsErrorResponse` — Upload garbage file, verify status=ERROR, errorMessage present. Verify document in MongoDB.
3. `uploadValidFile_archivesFile` — Verify file exists in archive directory.

- [ ] **Step 2: Run all tests**

Run: `./gradlew :insurance-response-app:test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add insurance-response-app/src/test/java/com/example/edi/insuranceresponse/EligibilityResponseIT.java
git commit -m "test: add integration tests with Testcontainers for 271 parsing"
```

---

### Task 9: Final verification

- [ ] **Step 1: Run full build**

Run: `./gradlew :insurance-response-app:clean :insurance-response-app:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Create a sample 271 test file**

Save to `specs/003-eligibility-response-parser/sample-271.edi`:

```
ISA*00*          *00*          *ZZ*BCBS12345      *ZZ*CLEARINGHOUSE01*260329*1200*^*00501*000000001*0*T*:~
GS*HB*BCBS12345*CLEARINGHOUSE01*20260329*1200*1*X*005010X279A1~
ST*271*0001*005010X279A1~
BHT*0022*11*12345*20260329*1200~
HL*1**20*1~
NM1*PR*2*BLUE CROSS BLUE SHIELD*****PI*BCBS12345~
HL*2*1*21*1~
NM1*IL*1*SMITH*JOHN****MI*MEM987654321~
DMG*D8*19850715*M~
DTP*346*D8*20250101~
DTP*347*D8*20251231~
EB*1**30*HM*GOLD PLAN~
MSG*PATIENT IS ACTIVE AND ELIGIBLE~
EB*C*IND*30**23*6*500~
EB*A*IND*30**24*7*30~
SE*14*0001~
GE*1*1~
IEA*1*000000001~
```

- [ ] **Step 3: Manual smoke test**

```bash
docker-compose up -d
./gradlew :insurance-response-app:bootRun
```

In another terminal:
```bash
curl -X POST http://localhost:8082/api/insurance/eligibility-response \
  -F "file=@specs/003-eligibility-response-parser/sample-271.edi"
```

Expected: JSON with status=COMPLETED, subscriberLastName=SMITH, memberId=MEM987654321, eligibilityStatus=ACTIVE, benefits list with deductible and copay entries.

- [ ] **Step 4: Test error case**

```bash
echo "NOT AN EDI FILE" > /tmp/bad.edi
curl -X POST http://localhost:8082/api/insurance/eligibility-response \
  -F "file=@/tmp/bad.edi"
```

Expected: JSON with status=ERROR, errorMessage describing the parse failure.

- [ ] **Step 5: Commit any final fixes**
