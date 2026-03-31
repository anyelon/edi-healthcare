# Prior Authorization (EDI 278) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `prior-auth-app` microservice that generates EDI 278 prior authorization requests from encounter data and parses EDI 278 responses, writing the authorization number back to the encounter.

**Architecture:** New Gradle module `prior-auth-app` (port 8083) following the same Controller to Service to EDI Service layering as the existing apps. Uses `needsAuth` and `clinicalReason` fields on `EncounterProcedure` to identify procedures requiring prior authorization. Frontend adds a tabbed page with encounter table selection and file upload.

**Tech Stack:** Java 21, Spring Boot 4.0.5, StAEDI 1.26.2, MongoDB, Next.js 16, React 19, TypeScript, Tailwind CSS 4, shadcn/ui

---

## File Structure

### Backend -- Common (modified)

| File | Action |
|------|--------|
| `backend/common/src/main/java/com/example/edi/common/document/RequestedProcedure.java` | Create |
| `backend/common/src/main/java/com/example/edi/common/document/Encounter.java` | Modify |

### Backend -- Prior Auth App (new module)

| File | Action |
|------|--------|
| `backend/prior-auth-app/build.gradle` | Create |
| `backend/prior-auth-app/src/main/resources/application.yml` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/PriorAuthApplication.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/MongoConfig.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/InterchangeProperties.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/ArchiveProperties.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/dto/PriorAuthRequestDTO.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/dto/PriorAuthBundle.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/EDI278Request.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/EDI278Response.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/ServiceReviewInfo.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/AuthorizationDecision.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Mapper.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Generator.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Parser.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278ResponseMapper.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/PriorAuthService.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/controller/PriorAuthController.java` | Create |
| `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/controller/GlobalExceptionHandler.java` | Create |
| `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278GeneratorTest.java` | Create |
| `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278ParserTest.java` | Create |
| `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278MapperTest.java` | Create |
| `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278ResponseMapperTest.java` | Create |
| `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/controller/PriorAuthControllerTest.java` | Create |
| `backend/prior-auth-app/src/test/resources/278_response_certified.edi` | Create |
| `backend/prior-auth-app/src/test/resources/278_response_denied.edi` | Create |

### Root config (modified)

| File | Action |
|------|--------|
| `settings.gradle` | Modify |

### Backend -- Claims App (modified for EncounterResponse)

| File | Action |
|------|--------|
| `backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java` | Modify |
| `backend/claims-app/src/main/java/com/example/edi/claims/dto/RequestedProcedureResponse.java` | Create |
| `backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java` | Modify |

### Frontend (new and modified)

| File | Action |
|------|--------|
| `frontend/src/types/index.ts` | Modify |
| `frontend/src/lib/api-client.ts` | Modify |
| `frontend/src/app/api/prior-auth/generate/route.ts` | Create |
| `frontend/src/app/api/prior-auth/response/route.ts` | Create |
| `frontend/src/app/prior-auth/page.tsx` | Create |
| `frontend/src/app/page.tsx` | Modify |
| `frontend/src/components/app-sidebar.tsx` | Modify |

### Cloud deployment (new and modified)

| File | Action |
|------|--------|
| `backend/prior-auth-app/Dockerfile` | Create |
| `backend/prior-auth-app/src/main/resources/application-cloud.yml` | Create |
| `.github/workflows/ci.yml` | Modify |
| `scripts/setup_gcloud_project.sh` | Modify |

---

### Task 1: Data model -- RequestedProcedure and Encounter update

**Files:**
- Create: `backend/common/src/main/java/com/example/edi/common/document/RequestedProcedure.java`
- Modify: `backend/common/src/main/java/com/example/edi/common/document/Encounter.java`

- [ ] **Step 1: Create RequestedProcedure embedded class**

```java
// backend/common/src/main/java/com/example/edi/common/document/RequestedProcedure.java
package com.example.edi.common.document;

public class RequestedProcedure {

    private String procedureCode;
    private String clinicalReason;

    public RequestedProcedure() {}

    public RequestedProcedure(String procedureCode, String clinicalReason) {
        this.procedureCode = procedureCode;
        this.clinicalReason = clinicalReason;
    }

    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }

    public String getClinicalReason() { return clinicalReason; }
    public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }
}
```

- [ ] **Step 2: Add requestedProcedures to Encounter**

Add these lines to `backend/common/src/main/java/com/example/edi/common/document/Encounter.java`:

After `private String authorizationNumber;` add:

```java
private List<RequestedProcedure> requestedProcedures;
```

Add import at top:

```java
import java.util.List;
```

Add getter/setter after the `authorizationNumber` getter/setter:

```java
public List<RequestedProcedure> getRequestedProcedures() { return requestedProcedures; }
public void setRequestedProcedures(List<RequestedProcedure> requestedProcedures) { this.requestedProcedures = requestedProcedures; }
```

- [ ] **Step 3: Build common module to verify compilation**

Run: `./gradlew :common:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/common/src/main/java/com/example/edi/common/document/RequestedProcedure.java backend/common/src/main/java/com/example/edi/common/document/Encounter.java
git commit -m "feat: add RequestedProcedure to Encounter model"
```

---

### Task 2: Update EncounterResponse to expose requestedProcedures

**Files:**
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/dto/RequestedProcedureResponse.java`
- Modify: `backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java`
- Modify: `backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java`

- [ ] **Step 1: Create RequestedProcedureResponse DTO**

```java
// backend/claims-app/src/main/java/com/example/edi/claims/dto/RequestedProcedureResponse.java
package com.example.edi.claims.dto;

public record RequestedProcedureResponse(
        String procedureCode,
        String clinicalReason
) {}
```

- [ ] **Step 2: Add requestedProcedures to EncounterResponse**

Replace the current `EncounterResponse` record in `backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java` with:

```java
package com.example.edi.claims.dto;

import java.time.LocalDate;
import java.util.List;

public record EncounterResponse(
        String id,
        String patientId,
        String patientName,
        String providerId,
        String providerName,
        String facilityId,
        String facilityName,
        LocalDate dateOfService,
        String authorizationNumber,
        List<DiagnosisResponse> diagnoses,
        List<ProcedureResponse> procedures,
        List<RequestedProcedureResponse> requestedProcedures
) {}
```

- [ ] **Step 3: Update EncounterService to map requestedProcedures**

In `backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java`, add the import:

```java
import com.example.edi.common.document.RequestedProcedure;
```

In the `getAllEncounters()` method, inside the `encounters.stream().map(encounter -> { ... })` block, before the `return new EncounterResponse(...)`, add:

```java
List<RequestedProcedureResponse> requestedProcedures = encounter.getRequestedProcedures() != null
        ? encounter.getRequestedProcedures().stream()
                .map(rp -> new RequestedProcedureResponse(rp.getProcedureCode(), rp.getClinicalReason()))
                .toList()
        : List.of();
```

Then add `requestedProcedures` as the last argument in the `new EncounterResponse(...)` constructor call.

- [ ] **Step 4: Build claims-app to verify**

Run: `./gradlew :claims-app:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/claims-app/src/main/java/com/example/edi/claims/dto/RequestedProcedureResponse.java backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java
git commit -m "feat: expose requestedProcedures in EncounterResponse"
```

---

### Task 3: Gradle module scaffold

**Files:**
- Create: `backend/prior-auth-app/build.gradle`
- Create: `backend/prior-auth-app/src/main/resources/application.yml`
- Modify: `settings.gradle`

- [ ] **Step 1: Create build.gradle**

```groovy
// backend/prior-auth-app/build.gradle
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'io.xlate:staedi:1.26.2'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:testcontainers-mongodb:2.0.4'
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter:2.0.4'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

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

- [ ] **Step 2: Create application.yml**

```yaml
# backend/prior-auth-app/src/main/resources/application.yml
server:
  port: 8083
  tomcat:
    max-http-header-size: 128KB

spring:
  application:
    name: prior-auth-app
  data:
    mongodb:
      uri: mongodb://localhost:27017/edi_healthcare
      database: edi_healthcare
  mongodb:
    uri: mongodb://localhost:27017/edi_healthcare
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

edi:
  interchange:
    sender-id-qualifier: ZZ
    sender-id: SENDER_ID
    receiver-id-qualifier: ZZ
    receiver-id: RECEIVER_ID
    ack-requested: "0"
    usage-indicator: T
  archive:
    path: edi/278/incoming
```

- [ ] **Step 3: Register module in settings.gradle**

Add these two lines to `settings.gradle` after the `insurance-response-app` entries:

```groovy
include 'prior-auth-app'
project(':prior-auth-app').projectDir = file('backend/prior-auth-app')
```

- [ ] **Step 4: Create directory structure**

```bash
mkdir -p backend/prior-auth-app/src/main/java/com/example/edi/priorauth/{config,controller,domain,dto,service}
mkdir -p backend/prior-auth-app/src/main/resources
mkdir -p backend/prior-auth-app/src/test/java/com/example/edi/priorauth/{controller,service}
mkdir -p backend/prior-auth-app/src/test/resources
```

- [ ] **Step 5: Commit**

```bash
git add backend/prior-auth-app/build.gradle backend/prior-auth-app/src/main/resources/application.yml settings.gradle
git commit -m "feat: scaffold prior-auth-app Gradle module"
```

---

### Task 4: Spring Boot application and config classes

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/PriorAuthApplication.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/MongoConfig.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/InterchangeProperties.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/ArchiveProperties.java`

- [ ] **Step 1: Create PriorAuthApplication.java**

```java
package com.example.edi.priorauth;

import com.example.edi.priorauth.config.InterchangeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.example.edi.priorauth", "com.example.edi.common"})
@EnableConfigurationProperties(InterchangeProperties.class)
public class PriorAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriorAuthApplication.class, args);
    }
}
```

- [ ] **Step 2: Create MongoConfig.java**

```java
package com.example.edi.priorauth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.edi.common.repository")
@EnableConfigurationProperties(ArchiveProperties.class)
public class MongoConfig {
}
```

- [ ] **Step 3: Create InterchangeProperties.java**

```java
package com.example.edi.priorauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

- [ ] **Step 4: Create ArchiveProperties.java**

```java
package com.example.edi.priorauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.archive")
public record ArchiveProperties(String path) {}
```

- [ ] **Step 5: Verify the app compiles**

Run: `./gradlew :prior-auth-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/PriorAuthApplication.java backend/prior-auth-app/src/main/java/com/example/edi/priorauth/config/
git commit -m "feat: add PriorAuthApplication and config classes"
```

---

### Task 5: DTOs and domain records

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/dto/PriorAuthRequestDTO.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/dto/PriorAuthBundle.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/ServiceReviewInfo.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/AuthorizationDecision.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/EDI278Request.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/EDI278Response.java`

- [ ] **Step 1: Create PriorAuthRequestDTO**

```java
package com.example.edi.priorauth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PriorAuthRequestDTO(
        @NotEmpty List<@NotBlank String> encounterIds
) {}
```

- [ ] **Step 2: Create PriorAuthBundle**

```java
package com.example.edi.priorauth.dto;

import com.example.edi.common.document.*;
import java.util.List;

public record PriorAuthBundle(
        Encounter encounter,
        Patient patient,
        PatientInsurance insurance,
        Payer payer,
        Practice practice,
        List<RequestedProcedure> requestedProcedures
) {}
```

- [ ] **Step 3: Create ServiceReviewInfo**

This record represents one requested service within the 278 HL4 patient event loop.

```java
package com.example.edi.priorauth.domain;

public record ServiceReviewInfo(
        String procedureCode,
        String clinicalReason,
        String serviceDate
) {}
```

- [ ] **Step 4: Create AuthorizationDecision**

This record represents the parsed decision from a 278 response HCR segment.

```java
package com.example.edi.priorauth.domain;

public record AuthorizationDecision(
        String action,
        String authorizationNumber,
        String encounterId
) {}
```

- [ ] **Step 5: Create EDI278Request**

```java
package com.example.edi.priorauth.domain;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import java.util.List;

public record EDI278Request(
        InterchangeEnvelope envelope,
        FunctionalGroup functionalGroup,
        TransactionHeader transactionHeader,
        String payerName,
        String payerId,
        String providerName,
        String providerNpi,
        String providerTaxId,
        List<SubscriberReviewGroup> subscriberGroups
) {
    public record SubscriberReviewGroup(
            SubscriberLoop subscriber,
            String encounterId,
            List<ServiceReviewInfo> services
    ) {}
}
```

- [ ] **Step 6: Create EDI278Response**

```java
package com.example.edi.priorauth.domain;

import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.TransactionHeader;
import java.util.List;

public record EDI278Response(
        InterchangeEnvelope envelope,
        FunctionalGroup functionalGroup,
        TransactionHeader transactionHeader,
        String payerName,
        String payerId,
        String subscriberFirstName,
        String subscriberLastName,
        String memberId,
        List<AuthorizationDecision> decisions
) {}
```

- [ ] **Step 7: Verify compilation**

Run: `./gradlew :prior-auth-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/dto/ backend/prior-auth-app/src/main/java/com/example/edi/priorauth/domain/
git commit -m "feat: add DTOs and domain records for EDI 278"
```

---

### Task 6: EDI278Mapper -- entity-to-domain mapping

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Mapper.java`
- Create: `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278MapperTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.edi.priorauth.service;

import com.example.edi.common.document.*;
import com.example.edi.priorauth.config.InterchangeProperties;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.dto.PriorAuthBundle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EDI278MapperTest {

    private final EDI278Mapper mapper = new EDI278Mapper();

    @Test
    void map_singleBundle_producesCorrectStructure() {
        InterchangeProperties props = new InterchangeProperties(
                "ZZ", "SENDER", "ZZ", "RECEIVER", "0", "T");
        PriorAuthBundle bundle = createTestBundle();

        EDI278Request result = mapper.map(List.of(bundle), props);

        assertNotNull(result.envelope());
        assertEquals("SENDER", result.envelope().senderId());
        assertEquals("RECEIVER", result.envelope().receiverId());
        assertEquals("278", result.transactionHeader().referenceId());
        assertEquals("Test Payer", result.payerName());
        assertEquals("PAY001", result.payerId());
        assertEquals("Test Practice", result.providerName());
        assertEquals(1, result.subscriberGroups().size());

        EDI278Request.SubscriberReviewGroup group = result.subscriberGroups().getFirst();
        assertEquals("ENC001", group.encounterId());
        assertEquals(1, group.services().size());
        assertEquals("99213", group.services().getFirst().procedureCode());
        assertEquals("Chronic pain management", group.services().getFirst().clinicalReason());
    }

    private PriorAuthBundle createTestBundle() {
        Encounter encounter = new Encounter();
        encounter.setId("ENC001");
        encounter.setPatientId("PAT001");
        encounter.setProviderId("PROV001");
        encounter.setPracticeId("PRAC001");
        encounter.setFacilityId("FAC001");
        encounter.setDateOfService(LocalDate.of(2026, 4, 15));
        encounter.setRequestedProcedures(List.of(
                new RequestedProcedure("99213", "Chronic pain management")
        ));

        Patient patient = new Patient();
        patient.setId("PAT001");
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 15));
        patient.setGender("M");
        patient.setAddress("123 Main St");
        patient.setCity("Springfield");
        patient.setState("IL");
        patient.setZipCode("62701");

        PatientInsurance insurance = new PatientInsurance();
        insurance.setMemberId("MEM001");
        insurance.setGroupNumber("GRP001");
        insurance.setPolicyType("HMO");
        insurance.setSubscriberRelationship("self");

        Payer payer = new Payer();
        payer.setPayerId("PAY001");
        payer.setName("Test Payer");

        Practice practice = new Practice();
        practice.setId("PRAC001");
        practice.setName("Test Practice");
        practice.setNpi("1234567890");
        practice.setTaxId("123456789");

        return new PriorAuthBundle(encounter, patient, insurance, payer, practice,
                encounter.getRequestedProcedures());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278MapperTest" -i`
Expected: FAIL -- `EDI278Mapper` class not found

- [ ] **Step 3: Write EDI278Mapper implementation**

```java
package com.example.edi.priorauth.service;

import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.config.InterchangeProperties;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.ServiceReviewInfo;
import com.example.edi.priorauth.dto.PriorAuthBundle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EDI278Mapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HHmm");

    public EDI278Request map(List<PriorAuthBundle> bundles, InterchangeProperties props) {
        LocalDateTime now = LocalDateTime.now();
        String nowDate   = now.format(YYYYMMDD);
        String nowYYMMDD = now.format(YYMMDD);
        String nowTime   = now.format(HHMM);

        long millis        = System.currentTimeMillis();
        String controlNum9 = String.format("%09d", millis % 1_000_000_000L);
        String controlNum5 = String.format("%05d", millis % 100_000L);

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                props.senderIdQualifier(), props.senderId(),
                props.receiverIdQualifier(), props.receiverId(),
                nowYYMMDD, nowTime, controlNum9,
                props.ackRequested(), props.usageIndicator());

        FunctionalGroup functionalGroup = new FunctionalGroup(
                props.senderId(), props.receiverId(),
                nowDate, nowTime, controlNum5);

        TransactionHeader transactionHeader = new TransactionHeader(
                controlNum5, "278", nowDate, nowTime);

        PriorAuthBundle first = bundles.getFirst();
        String payerName = first.payer().getName();
        String payerId   = first.payer().getPayerId();
        String providerName = first.practice().getName();
        String providerNpi  = first.practice().getNpi();
        String providerTaxId = first.practice().getTaxId();

        List<EDI278Request.SubscriberReviewGroup> subscriberGroups = new ArrayList<>();
        for (PriorAuthBundle bundle : bundles) {
            Patient patient = bundle.patient();
            PatientInsurance ins = bundle.insurance();
            Payer payer = bundle.payer();

            SubscriberLoop subscriberLoop = new SubscriberLoop(
                    mapSubscriberRelationship(ins.getSubscriberRelationship()),
                    ins.getGroupNumber(),
                    ins.getPolicyType(),
                    patient.getLastName(),
                    patient.getFirstName(),
                    ins.getMemberId(),
                    patient.getAddress(),
                    patient.getCity(),
                    patient.getState(),
                    patient.getZipCode(),
                    formatDate(patient.getDateOfBirth()),
                    mapGender(patient.getGender()),
                    payer.getName(),
                    payer.getPayerId());

            String serviceDate = formatDate(bundle.encounter().getDateOfService());

            List<ServiceReviewInfo> services = bundle.requestedProcedures().stream()
                    .map(rp -> new ServiceReviewInfo(
                            rp.getProcedureCode(), rp.getClinicalReason(), serviceDate))
                    .toList();

            subscriberGroups.add(new EDI278Request.SubscriberReviewGroup(
                    subscriberLoop, bundle.encounter().getId(), services));
        }

        return new EDI278Request(envelope, functionalGroup, transactionHeader,
                payerName, payerId, providerName, providerNpi, providerTaxId,
                subscriberGroups);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(YYYYMMDD) : "";
    }

    private String mapGender(String gender) {
        if (gender == null) return "U";
        return switch (gender.toUpperCase()) {
            case "M", "MALE"   -> "M";
            case "F", "FEMALE" -> "F";
            default            -> "U";
        };
    }

    private String mapSubscriberRelationship(String relationship) {
        if (relationship == null) return "P";
        return switch (relationship.toLowerCase()) {
            case "self"   -> "P";
            case "spouse" -> "S";
            case "child"  -> "D";
            default       -> "P";
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278MapperTest" -i`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Mapper.java backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278MapperTest.java
git commit -m "feat: add EDI278Mapper with tests"
```

---

### Task 7: EDI278Generator -- write EDI 278 output

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Generator.java`
- Create: `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278GeneratorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.edi.priorauth.service;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.ServiceReviewInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EDI278GeneratorTest {

    private final EDI278Generator generator = new EDI278Generator();

    @Test
    void generate_containsISAandIEA() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("ISA*"));
        assertTrue(edi.contains("IEA*"));
    }

    @Test
    void generate_containsGSWithHIFunctionalId() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("GS*HI*"));
        assertTrue(edi.contains("GE*"));
    }

    @Test
    void generate_containsSTandSE() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("ST*278*"));
        assertTrue(edi.contains("SE*"));
    }

    @Test
    void generate_containsBHTForRequest() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("BHT*0007*"));
    }

    @Test
    void generate_containsHLHierarchy() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("HL*1**20*1"));
        assertTrue(edi.contains("HL*2*1*21*1"));
        assertTrue(edi.contains("HL*3*2*22*1"));
        assertTrue(edi.contains("HL*4*3*EV*0"));
    }

    @Test
    void generate_containsTRNWithEncounterId() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("TRN*1*ENC001"));
    }

    @Test
    void generate_containsUMSegment() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("UM*"));
    }

    @Test
    void generate_containsSV1WithProcedureCode() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("SV1*HC:99213"));
    }

    @Test
    void generate_containsHIWithClinicalReason() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);

        assertTrue(edi.contains("HI*"));
        assertTrue(edi.contains("Chronic pain"));
    }

    private EDI278Request createTestRequest() {
        InterchangeEnvelope envelope = new InterchangeEnvelope(
                "ZZ", "SENDER_ID", "ZZ", "RECEIVER_ID",
                "260415", "1200", "000000001", "0", "T");

        FunctionalGroup fg = new FunctionalGroup(
                "SENDER_ID", "RECEIVER_ID", "20260415", "1200", "00001");

        TransactionHeader th = new TransactionHeader(
                "00001", "278", "20260415", "1200");

        SubscriberLoop subscriber = new SubscriberLoop(
                "P", "GRP001", "HMO", "Doe", "John", "MEM001",
                "123 Main St", "Springfield", "IL", "62701",
                "19900515", "M", "Test Payer", "PAY001");

        ServiceReviewInfo service = new ServiceReviewInfo(
                "99213", "Chronic pain management", "20260415");

        EDI278Request.SubscriberReviewGroup group =
                new EDI278Request.SubscriberReviewGroup(
                        subscriber, "ENC001", List.of(service));

        return new EDI278Request(envelope, fg, th,
                "Test Payer", "PAY001", "Test Practice",
                "1234567890", "123456789", List.of(group));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278GeneratorTest" -i`
Expected: FAIL -- `EDI278Generator` class not found

- [ ] **Step 3: Write EDI278Generator implementation**

```java
package com.example.edi.priorauth.service;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.ServiceReviewInfo;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class EDI278Generator {

    public String generate(EDI278Request request) {
        try {
            var factory = EDIOutputFactory.newFactory();
            factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
            var baos = new ByteArrayOutputStream();
            var writer = factory.createEDIStreamWriter(baos);

            int segmentCount = 0;
            writer.startInterchange();

            writeISA(writer, request.envelope());
            writeGS(writer, request.functionalGroup());

            segmentCount += writeST(writer, request.transactionHeader());
            segmentCount += writeBHT(writer, request.transactionHeader());

            int hlCounter = 1;

            // HL 1 -- Payer (Utilization Management Organization)
            int payerHL = hlCounter++;
            segmentCount += writeHL(writer, payerHL, 0, "20", "1");
            segmentCount += writePayerNM1(writer,
                    request.payerName(), request.payerId());

            // HL 2 -- Provider (Requester)
            int providerHL = hlCounter++;
            segmentCount += writeHL(writer, providerHL, payerHL, "21", "1");
            segmentCount += writeProviderNM1(writer,
                    request.providerName(), request.providerNpi());
            segmentCount += writeProviderREF(writer,
                    request.providerNpi(), request.providerTaxId());

            for (EDI278Request.SubscriberReviewGroup group :
                    request.subscriberGroups()) {
                // HL 3 -- Subscriber
                int subscriberHL = hlCounter++;
                segmentCount += writeHL(writer, subscriberHL,
                        providerHL, "22", "1");
                segmentCount += writeTRN(writer, group.encounterId());
                segmentCount += writeSubscriberNM1(writer, group.subscriber());
                segmentCount += writeDMG(writer, group.subscriber());

                // HL 4 -- Patient Event
                int eventHL = hlCounter++;
                segmentCount += writeHL(writer, eventHL,
                        subscriberHL, "EV", "0");
                segmentCount += writeUM(writer);

                for (ServiceReviewInfo service : group.services()) {
                    segmentCount += writeDTP(writer, service.serviceDate());
                    segmentCount += writeSV1(writer, service.procedureCode());
                    segmentCount += writeHI(writer, service.clinicalReason());
                }
            }

            segmentCount++;
            writeSE(writer, segmentCount,
                    request.transactionHeader().transactionSetControlNumber());
            writeGE(writer, request.functionalGroup().controlNumber());
            writeIEA(writer, request.envelope().controlNumber());

            writer.endInterchange();
            writer.close();

            return baos.toString(StandardCharsets.UTF_8);
        } catch (EDIStreamException e) {
            throw new RuntimeException("Failed to generate EDI 278", e);
        }
    }

    private void writeISA(EDIStreamWriter w, InterchangeEnvelope env)
            throws EDIStreamException {
        w.writeStartSegment("ISA");
        elem(w, "00");
        elem(w, padRight("", 10));
        elem(w, "00");
        elem(w, padRight("", 10));
        elem(w, env.senderIdQualifier());
        elem(w, padRight(env.senderId(), 15));
        elem(w, env.receiverIdQualifier());
        elem(w, padRight(env.receiverId(), 15));
        elem(w, env.date());
        elem(w, env.time());
        elem(w, "^");
        elem(w, "00501");
        elem(w, env.controlNumber());
        elem(w, env.ackRequested());
        elem(w, env.usageIndicator());
        elem(w, ":");
        w.writeEndSegment();
    }

    private void writeGS(EDIStreamWriter w, FunctionalGroup fg)
            throws EDIStreamException {
        w.writeStartSegment("GS");
        elem(w, "HI");
        elem(w, fg.senderId());
        elem(w, fg.receiverId());
        elem(w, fg.date());
        elem(w, fg.time());
        elem(w, fg.controlNumber());
        elem(w, "X");
        elem(w, "005010X217");
        w.writeEndSegment();
    }

    private int writeST(EDIStreamWriter w, TransactionHeader th)
            throws EDIStreamException {
        w.writeStartSegment("ST");
        elem(w, "278");
        elem(w, th.transactionSetControlNumber());
        elem(w, "005010X217");
        w.writeEndSegment();
        return 1;
    }

    private int writeBHT(EDIStreamWriter w, TransactionHeader th)
            throws EDIStreamException {
        w.writeStartSegment("BHT");
        elem(w, "0007");
        elem(w, "13");
        elem(w, th.transactionSetControlNumber());
        elem(w, th.creationDate());
        elem(w, th.creationTime());
        w.writeEndSegment();
        return 1;
    }

    private int writeHL(EDIStreamWriter w, int id, int parentId,
                        String levelCode, String childCode)
            throws EDIStreamException {
        w.writeStartSegment("HL");
        elem(w, String.valueOf(id));
        elem(w, parentId > 0 ? String.valueOf(parentId) : "");
        elem(w, levelCode);
        elem(w, childCode);
        w.writeEndSegment();
        return 1;
    }

    private int writePayerNM1(EDIStreamWriter w,
                              String payerName, String payerId)
            throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "X3");
        elem(w, "2");
        elem(w, payerName);
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "PI");
        elem(w, payerId);
        w.writeEndSegment();
        return 1;
    }

    private int writeProviderNM1(EDIStreamWriter w,
                                 String providerName, String npi)
            throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "1P");
        elem(w, "2");
        elem(w, providerName);
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "XX");
        elem(w, npi);
        w.writeEndSegment();
        return 1;
    }

    private int writeProviderREF(EDIStreamWriter w,
                                 String npi, String taxId)
            throws EDIStreamException {
        int count = 0;
        w.writeStartSegment("REF");
        elem(w, "1J");
        elem(w, npi);
        w.writeEndSegment();
        count++;

        w.writeStartSegment("REF");
        elem(w, "EI");
        elem(w, taxId);
        w.writeEndSegment();
        count++;
        return count;
    }

    private int writeTRN(EDIStreamWriter w, String encounterId)
            throws EDIStreamException {
        w.writeStartSegment("TRN");
        elem(w, "1");
        elem(w, encounterId);
        elem(w, "9SENDER_ID");
        w.writeEndSegment();
        return 1;
    }

    private int writeSubscriberNM1(EDIStreamWriter w, SubscriberLoop sub)
            throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "IL");
        elem(w, "1");
        elem(w, sub.lastName());
        elem(w, sub.firstName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "MI");
        elem(w, sub.memberId());
        w.writeEndSegment();
        return 1;
    }

    private int writeDMG(EDIStreamWriter w, SubscriberLoop sub)
            throws EDIStreamException {
        w.writeStartSegment("DMG");
        elem(w, "D8");
        elem(w, sub.dateOfBirth());
        elem(w, sub.genderCode());
        w.writeEndSegment();
        return 1;
    }

    private int writeUM(EDIStreamWriter w) throws EDIStreamException {
        w.writeStartSegment("UM");
        elem(w, "HS");
        elem(w, "I");
        elem(w, "");
        elem(w, "");
        elem(w, "11");
        w.writeEndSegment();
        return 1;
    }

    private int writeDTP(EDIStreamWriter w, String serviceDate)
            throws EDIStreamException {
        w.writeStartSegment("DTP");
        elem(w, "472");
        elem(w, "D8");
        elem(w, serviceDate);
        w.writeEndSegment();
        return 1;
    }

    private int writeSV1(EDIStreamWriter w, String procedureCode)
            throws EDIStreamException {
        w.writeStartSegment("SV1");
        elem(w, "HC:" + procedureCode);
        elem(w, "");
        elem(w, "UN");
        elem(w, "1");
        w.writeEndSegment();
        return 1;
    }

    private int writeHI(EDIStreamWriter w, String clinicalReason)
            throws EDIStreamException {
        w.writeStartSegment("HI");
        elem(w, "BF:" + clinicalReason);
        w.writeEndSegment();
        return 1;
    }

    private void writeSE(EDIStreamWriter w, int segmentCount,
                         String controlNumber) throws EDIStreamException {
        w.writeStartSegment("SE");
        elem(w, String.valueOf(segmentCount));
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeGE(EDIStreamWriter w, String controlNumber)
            throws EDIStreamException {
        w.writeStartSegment("GE");
        elem(w, "1");
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeIEA(EDIStreamWriter w, String controlNumber)
            throws EDIStreamException {
        w.writeStartSegment("IEA");
        elem(w, "1");
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void elem(EDIStreamWriter w, String value)
            throws EDIStreamException {
        w.writeElement(value != null ? value : "");
    }

    private String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278GeneratorTest" -i`
Expected: All 9 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Generator.java backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278GeneratorTest.java
git commit -m "feat: add EDI278Generator with tests"
```

---

### Task 8: EDI 278 response test resources

**Files:**
- Create: `backend/prior-auth-app/src/test/resources/278_response_certified.edi`
- Create: `backend/prior-auth-app/src/test/resources/278_response_denied.edi`

- [ ] **Step 1: Create certified response sample**

```
ISA*00*          *00*          *ZZ*RECEIVER_ID    *ZZ*SENDER_ID      *260415*1200*^*00501*000000001*0*T*:~
GS*HI*RECEIVER_ID*SENDER_ID*20260415*1200*00001*X*005010X217~
ST*278*00001*005010X217~
BHT*0007*11*00001*20260415*1200~
HL*1**20*1~
NM1*X3*2*Test Payer*****PI*PAY001~
HL*2*1*21*1~
NM1*1P*2*Test Practice*****XX*1234567890~
HL*3*2*22*1~
TRN*1*ENC001*9RECEIVER_ID~
NM1*IL*1*Doe*John****MI*MEM001~
HL*4*3*EV*0~
HCR*A1*AUTH12345~
UM*HS*I***11~
SE*13*00001~
GE*1*00001~
IEA*1*000000001~
```

- [ ] **Step 2: Create denied response sample**

```
ISA*00*          *00*          *ZZ*RECEIVER_ID    *ZZ*SENDER_ID      *260415*1200*^*00501*000000002*0*T*:~
GS*HI*RECEIVER_ID*SENDER_ID*20260415*1200*00002*X*005010X217~
ST*278*00002*005010X217~
BHT*0007*11*00002*20260415*1200~
HL*1**20*1~
NM1*X3*2*Test Payer*****PI*PAY001~
HL*2*1*21*1~
NM1*1P*2*Test Practice*****XX*1234567890~
HL*3*2*22*1~
TRN*1*ENC002*9RECEIVER_ID~
NM1*IL*1*Smith*Jane****MI*MEM002~
HL*4*3*EV*0~
HCR*A2~
AAA*N*72~
UM*HS*I***11~
SE*14*00002~
GE*1*00002~
IEA*1*000000002~
```

- [ ] **Step 3: Commit**

```bash
git add backend/prior-auth-app/src/test/resources/
git commit -m "feat: add sample EDI 278 response test resources"
```

---

### Task 9: EDI278Parser -- parse inbound 278 responses

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Parser.java`
- Create: `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278ParserTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.edi.priorauth.service;

import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EDI278ParserTest {

    private final EDI278Parser parser = new EDI278Parser();

    @Test
    void parse_certifiedResponse_returnsApprovedDecision() throws Exception {
        Path path = Path.of("src/test/resources/278_response_certified.edi");
        EDI278Response response = parser.parse(path);

        assertNotNull(response);
        assertEquals("Test Payer", response.payerName());
        assertEquals("PAY001", response.payerId());
        assertEquals("John", response.subscriberFirstName());
        assertEquals("Doe", response.subscriberLastName());
        assertEquals("MEM001", response.memberId());

        assertEquals(1, response.decisions().size());
        AuthorizationDecision decision = response.decisions().getFirst();
        assertEquals("CERTIFIED", decision.action());
        assertEquals("AUTH12345", decision.authorizationNumber());
        assertEquals("ENC001", decision.encounterId());
    }

    @Test
    void parse_deniedResponse_returnsDeniedDecision() throws Exception {
        Path path = Path.of("src/test/resources/278_response_denied.edi");
        EDI278Response response = parser.parse(path);

        assertNotNull(response);
        assertEquals("Jane", response.subscriberFirstName());
        assertEquals("Smith", response.subscriberLastName());
        assertEquals("MEM002", response.memberId());

        assertEquals(1, response.decisions().size());
        AuthorizationDecision decision = response.decisions().getFirst();
        assertEquals("DENIED", decision.action());
        assertNull(decision.authorizationNumber());
        assertEquals("ENC002", decision.encounterId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278ParserTest" -i`
Expected: FAIL -- `EDI278Parser` class not found

- [ ] **Step 3: Write EDI278Parser implementation**

```java
package com.example.edi.priorauth.service;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class EDI278Parser {

    public EDI278Response parse(Path filePath) throws Exception {
        var factory = EDIInputFactory.newFactory();

        String currentSegment = "";
        int elementPosition = 0;

        // ISA fields
        String isaSenderQual = "", isaSenderId = "";
        String isaReceiverQual = "", isaReceiverId = "";
        String isaDate = "", isaTime = "", isaControlNum = "";
        String isaAck = "", isaUsage = "";

        // GS fields
        String gsSenderId = "", gsReceiverId = "";
        String gsDate = "", gsTime = "", gsControlNum = "";

        // ST fields
        String stControlNum = "";

        // BHT fields
        String bhtDate = "", bhtTime = "";

        // NM1 tracking
        String nm1Qualifier = "";
        String payerName = "", payerId = "";
        String subscriberFirst = "", subscriberLast = "";
        String memberId = "";

        // TRN tracking
        String currentEncounterId = "";

        // HCR tracking
        String hcrAction = "";
        String hcrAuthNum = null;

        List<AuthorizationDecision> decisions = new ArrayList<>();

        try (var stream = new FileInputStream(filePath.toFile());
             var reader = factory.createEDIStreamReader(stream)) {

            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();

                switch (event) {
                    case START_SEGMENT -> {
                        currentSegment = reader.getText();
                        elementPosition = 0;
                        nm1Qualifier = "";
                        hcrAction = "";
                        hcrAuthNum = null;
                    }
                    case ELEMENT_DATA -> {
                        elementPosition++;
                        String value = reader.getText();

                        switch (currentSegment) {
                            case "ISA" -> {
                                switch (elementPosition) {
                                    case 5 -> isaSenderQual = value.trim();
                                    case 6 -> isaSenderId = value.trim();
                                    case 7 -> isaReceiverQual = value.trim();
                                    case 8 -> isaReceiverId = value.trim();
                                    case 9 -> isaDate = value.trim();
                                    case 10 -> isaTime = value.trim();
                                    case 13 -> isaControlNum = value.trim();
                                    case 14 -> isaAck = value.trim();
                                    case 15 -> isaUsage = value.trim();
                                }
                            }
                            case "GS" -> {
                                switch (elementPosition) {
                                    case 2 -> gsSenderId = value.trim();
                                    case 3 -> gsReceiverId = value.trim();
                                    case 4 -> gsDate = value.trim();
                                    case 5 -> gsTime = value.trim();
                                    case 6 -> gsControlNum = value.trim();
                                }
                            }
                            case "ST" -> {
                                if (elementPosition == 2)
                                    stControlNum = value.trim();
                            }
                            case "BHT" -> {
                                switch (elementPosition) {
                                    case 4 -> bhtDate = value.trim();
                                    case 5 -> bhtTime = value.trim();
                                }
                            }
                            case "NM1" -> {
                                switch (elementPosition) {
                                    case 1 -> nm1Qualifier = value.trim();
                                    case 3 -> {
                                        if ("X3".equals(nm1Qualifier))
                                            payerName = value.trim();
                                        if ("IL".equals(nm1Qualifier))
                                            subscriberLast = value.trim();
                                    }
                                    case 4 -> {
                                        if ("IL".equals(nm1Qualifier))
                                            subscriberFirst = value.trim();
                                    }
                                    case 9 -> {
                                        if ("X3".equals(nm1Qualifier))
                                            payerId = value.trim();
                                        if ("IL".equals(nm1Qualifier))
                                            memberId = value.trim();
                                    }
                                }
                            }
                            case "TRN" -> {
                                if (elementPosition == 2)
                                    currentEncounterId = value.trim();
                            }
                            case "HCR" -> {
                                switch (elementPosition) {
                                    case 1 -> hcrAction = value.trim();
                                    case 2 -> hcrAuthNum = value.trim();
                                }
                            }
                        }
                    }
                    case END_SEGMENT -> {
                        if ("HCR".equals(currentSegment)) {
                            String action = mapHcrAction(hcrAction);
                            String authNum = hcrAuthNum != null
                                    && !hcrAuthNum.isEmpty()
                                    ? hcrAuthNum : null;
                            decisions.add(new AuthorizationDecision(
                                    action, authNum, currentEncounterId));
                        }
                    }
                    default -> {}
                }
            }
        }

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                isaSenderQual, isaSenderId, isaReceiverQual, isaReceiverId,
                isaDate, isaTime, isaControlNum, isaAck, isaUsage);
        FunctionalGroup fg = new FunctionalGroup(
                gsSenderId, gsReceiverId, gsDate, gsTime, gsControlNum);
        TransactionHeader th = new TransactionHeader(
                stControlNum, "278", bhtDate, bhtTime);

        return new EDI278Response(envelope, fg, th,
                payerName, payerId, subscriberFirst, subscriberLast,
                memberId, decisions);
    }

    private String mapHcrAction(String code) {
        return switch (code) {
            case "A1" -> "CERTIFIED";
            case "A2" -> "DENIED";
            case "A3" -> "PENDED";
            case "A4" -> "MODIFIED";
            case "A6" -> "CANCELLED";
            default   -> code;
        };
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278ParserTest" -i`
Expected: Both tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Parser.java backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278ParserTest.java
git commit -m "feat: add EDI278Parser with tests"
```

---

### Task 10: EDI278ResponseMapper -- map parsed response and extract auth number

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278ResponseMapper.java`
- Create: `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278ResponseMapperTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.edi.priorauth.service;

import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EDI278ResponseMapperTest {

    private final EDI278ResponseMapper mapper = new EDI278ResponseMapper();

    @Test
    void mapEncounterUpdates_certifiedDecision_returnsAuthNumber() {
        EDI278Response response = createResponse(
                new AuthorizationDecision("CERTIFIED", "AUTH123", "ENC001"));

        Map<String, String> updates = mapper.mapEncounterUpdates(response);

        assertEquals(1, updates.size());
        assertEquals("AUTH123", updates.get("ENC001"));
    }

    @Test
    void mapEncounterUpdates_deniedDecision_returnsEmpty() {
        EDI278Response response = createResponse(
                new AuthorizationDecision("DENIED", null, "ENC001"));

        Map<String, String> updates = mapper.mapEncounterUpdates(response);

        assertTrue(updates.isEmpty());
    }

    @Test
    void mapEncounterUpdates_multipleDecisions_returnsCertifiedOnly() {
        EDI278Response response = createResponseMultiple(List.of(
                new AuthorizationDecision("CERTIFIED", "AUTH001", "ENC001"),
                new AuthorizationDecision("DENIED", null, "ENC002")));

        Map<String, String> updates = mapper.mapEncounterUpdates(response);

        assertEquals(1, updates.size());
        assertEquals("AUTH001", updates.get("ENC001"));
        assertFalse(updates.containsKey("ENC002"));
    }

    private EDI278Response createResponse(AuthorizationDecision decision) {
        return createResponseMultiple(List.of(decision));
    }

    private EDI278Response createResponseMultiple(
            List<AuthorizationDecision> decisions) {
        InterchangeEnvelope env = new InterchangeEnvelope(
                "ZZ", "S", "ZZ", "R", "260415", "1200",
                "000000001", "0", "T");
        FunctionalGroup fg = new FunctionalGroup(
                "S", "R", "20260415", "1200", "00001");
        TransactionHeader th = new TransactionHeader(
                "00001", "278", "20260415", "1200");
        return new EDI278Response(env, fg, th,
                "Payer", "PAY001", "John", "Doe", "MEM001", decisions);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278ResponseMapperTest" -i`
Expected: FAIL -- `EDI278ResponseMapper` class not found

- [ ] **Step 3: Write EDI278ResponseMapper implementation**

```java
package com.example.edi.priorauth.service;

import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EDI278ResponseMapper {

    public Map<String, String> mapEncounterUpdates(EDI278Response response) {
        Map<String, String> updates = new LinkedHashMap<>();
        for (AuthorizationDecision decision : response.decisions()) {
            if ("CERTIFIED".equals(decision.action())
                    && decision.authorizationNumber() != null) {
                updates.put(decision.encounterId(),
                        decision.authorizationNumber());
            }
        }
        return updates;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.service.EDI278ResponseMapperTest" -i`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278ResponseMapper.java backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278ResponseMapperTest.java
git commit -m "feat: add EDI278ResponseMapper with tests"
```

---

### Task 11: PriorAuthService -- orchestrator

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/PriorAuthService.java`

- [ ] **Step 1: Write PriorAuthService**

```java
package com.example.edi.priorauth.service;

import com.example.edi.common.document.*;
import com.example.edi.common.exception.*;
import com.example.edi.common.repository.*;
import com.example.edi.priorauth.config.ArchiveProperties;
import com.example.edi.priorauth.config.InterchangeProperties;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.priorauth.dto.PriorAuthBundle;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PriorAuthService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final PayerRepository payerRepository;
    private final PracticeRepository practiceRepository;
    private final EDI278Mapper edi278Mapper;
    private final EDI278Generator edi278Generator;
    private final EDI278Parser edi278Parser;
    private final EDI278ResponseMapper edi278ResponseMapper;
    private final InterchangeProperties interchangeProperties;
    private final ArchiveProperties archiveProperties;

    public PriorAuthService(
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            PayerRepository payerRepository,
            PracticeRepository practiceRepository,
            EDI278Mapper edi278Mapper,
            EDI278Generator edi278Generator,
            EDI278Parser edi278Parser,
            EDI278ResponseMapper edi278ResponseMapper,
            InterchangeProperties interchangeProperties,
            ArchiveProperties archiveProperties) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.payerRepository = payerRepository;
        this.practiceRepository = practiceRepository;
        this.edi278Mapper = edi278Mapper;
        this.edi278Generator = edi278Generator;
        this.edi278Parser = edi278Parser;
        this.edi278ResponseMapper = edi278ResponseMapper;
        this.interchangeProperties = interchangeProperties;
        this.archiveProperties = archiveProperties;
    }

    public String generatePriorAuth(List<String> encounterIds) {
        List<PriorAuthBundle> bundles = new ArrayList<>();

        for (String encounterId : encounterIds) {
            Encounter encounter = encounterRepository.findById(encounterId)
                    .orElseThrow(() ->
                            new EncounterNotFoundException(encounterId));

            Patient patient = patientRepository
                    .findById(encounter.getPatientId())
                    .orElseThrow(() ->
                            new PatientNotFoundException(
                                    encounter.getPatientId()));

            PatientInsurance insurance = patientInsuranceRepository
                    .findByPatientIdAndTerminationDateIsNull(
                            encounter.getPatientId())
                    .orElseThrow(() ->
                            new InsuranceNotFoundException(
                                    encounter.getPatientId()));

            Payer payer = payerRepository.findById(insurance.getPayerId())
                    .orElseThrow(() ->
                            new PayerNotFoundException(
                                    insurance.getPayerId()));

            Practice practice = practiceRepository
                    .findById(encounter.getPracticeId())
                    .orElseThrow(() ->
                            new PracticeNotFoundException(
                                    encounter.getPracticeId()));

            List<RequestedProcedure> requestedProcedures =
                    encounter.getRequestedProcedures() != null
                            ? encounter.getRequestedProcedures()
                            : List.of();

            bundles.add(new PriorAuthBundle(
                    encounter, patient, insurance, payer, practice,
                    requestedProcedures));
        }

        EDI278Request request = edi278Mapper.map(
                bundles, interchangeProperties);
        return edi278Generator.generate(request);
    }

    public EDI278Response processResponse(MultipartFile file)
            throws Exception {
        String filename = System.currentTimeMillis()
                + "_" + file.getOriginalFilename();
        var archiveDir = Paths.get(archiveProperties.path()).toAbsolutePath();
        Files.createDirectories(archiveDir);
        var archivePath = archiveDir.resolve(filename);
        file.transferTo(archivePath.toFile());

        EDI278Response parsed = edi278Parser.parse(archivePath);

        Map<String, String> updates =
                edi278ResponseMapper.mapEncounterUpdates(parsed);
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            encounterRepository.findById(entry.getKey())
                    .ifPresent(encounter -> {
                        encounter.setAuthorizationNumber(entry.getValue());
                        encounterRepository.save(encounter);
                    });
        }

        return parsed;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :prior-auth-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/PriorAuthService.java
git commit -m "feat: add PriorAuthService orchestrator"
```

---

### Task 12: Controller and exception handler

**Files:**
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/controller/PriorAuthController.java`
- Create: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/controller/GlobalExceptionHandler.java`

- [ ] **Step 1: Create PriorAuthController**

```java
package com.example.edi.priorauth.controller;

import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.priorauth.dto.PriorAuthRequestDTO;
import com.example.edi.priorauth.service.PriorAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/prior-auth")
public class PriorAuthController {

    private final PriorAuthService priorAuthService;

    public PriorAuthController(PriorAuthService priorAuthService) {
        this.priorAuthService = priorAuthService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generatePriorAuth(
            @Valid @RequestBody PriorAuthRequestDTO request) {
        String ediContent = priorAuthService.generatePriorAuth(
                request.encounterIds());
        byte[] bytes = ediContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=278_prior_auth.edi")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @PostMapping(value = "/response",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EDI278Response> processResponse(
            @RequestParam("file") MultipartFile file) throws Exception {
        EDI278Response result = priorAuthService.processResponse(file);
        return ResponseEntity.ok(result);
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

```java
package com.example.edi.priorauth.controller;

import com.example.edi.common.dto.ErrorResponse;
import com.example.edi.common.exception.EdiParseException;
import com.example.edi.common.exception.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ErrorResponse.notFound(
                        ex.getEntityType(), ex.getEntityId()));
    }

    @ExceptionHandler(EdiParseException.class)
    public ResponseEntity<ErrorResponse> handleEdiParse(
            EdiParseException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(ex.getMessage()));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prior-auth-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/controller/
git commit -m "feat: add PriorAuthController and GlobalExceptionHandler"
```

---

### Task 13: PriorAuthController tests

**Files:**
- Create: `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/controller/PriorAuthControllerTest.java`

- [ ] **Step 1: Write controller tests**

```java
package com.example.edi.priorauth.controller;

import com.example.edi.common.exception.EncounterNotFoundException;
import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.service.PriorAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PriorAuthController.class)
class PriorAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriorAuthService priorAuthService;

    @Test
    void generatePriorAuth_validRequest_returns200() throws Exception {
        when(priorAuthService.generatePriorAuth(List.of("ENC001")))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encounterIds": ["ENC001"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=278_prior_auth.edi"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    void generatePriorAuth_missingEncounterIds_returns400() throws Exception {
        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePriorAuth_emptyEncounterIds_returns400() throws Exception {
        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encounterIds": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePriorAuth_encounterNotFound_returns404() throws Exception {
        when(priorAuthService.generatePriorAuth(List.of("ENC001")))
                .thenThrow(new EncounterNotFoundException("ENC001"));

        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encounterIds": ["ENC001"]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.entityType").value("Encounter"))
                .andExpect(jsonPath("$.entityId").value("ENC001"));
    }

    @Test
    void processResponse_validFile_returns200() throws Exception {
        InterchangeEnvelope env = new InterchangeEnvelope(
                "ZZ", "S", "ZZ", "R", "260415", "1200",
                "000000001", "0", "T");
        FunctionalGroup fg = new FunctionalGroup(
                "S", "R", "20260415", "1200", "00001");
        TransactionHeader th = new TransactionHeader(
                "00001", "278", "20260415", "1200");
        EDI278Response response = new EDI278Response(env, fg, th,
                "Test Payer", "PAY001", "John", "Doe", "MEM001",
                List.of(new AuthorizationDecision(
                        "CERTIFIED", "AUTH123", "ENC001")));

        when(priorAuthService.processResponse(any()))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "278_response.edi",
                "text/plain", "ISA*00*...".getBytes());

        mockMvc.perform(multipart("/api/prior-auth/response").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payerName").value("Test Payer"))
                .andExpect(jsonPath("$.decisions[0].action")
                        .value("CERTIFIED"))
                .andExpect(jsonPath("$.decisions[0].authorizationNumber")
                        .value("AUTH123"));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :prior-auth-app:test --tests "com.example.edi.priorauth.controller.PriorAuthControllerTest" -i`
Expected: All 5 tests PASS

- [ ] **Step 3: Run full module tests**

Run: `./gradlew :prior-auth-app:test -i`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add backend/prior-auth-app/src/test/java/com/example/edi/priorauth/controller/PriorAuthControllerTest.java
git commit -m "feat: add PriorAuthController tests"
```

---

### Task 14: Frontend types and API client

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/lib/api-client.ts`

- [ ] **Step 1: Add frontend types**

Add to the end of `frontend/src/types/index.ts`:

```typescript
export interface RequestedProcedureResponse {
  procedureCode: string;
  clinicalReason: string;
}

export interface AuthorizationDecision {
  action: string;
  authorizationNumber: string | null;
  encounterId: string;
}

export interface PriorAuthResponse {
  payerName: string;
  payerId: string;
  subscriberFirstName: string;
  subscriberLastName: string;
  memberId: string;
  decisions: AuthorizationDecision[];
}
```

Also add `requestedProcedures` to the existing `EncounterResponse` interface. Replace the interface with:

```typescript
export interface EncounterResponse {
  id: string;
  patientId: string;
  patientName: string;
  providerId: string;
  providerName: string;
  facilityId: string;
  facilityName: string;
  dateOfService: string;
  authorizationNumber: string;
  diagnoses: DiagnosisResponse[];
  procedures: ProcedureResponse[];
  requestedProcedures: RequestedProcedureResponse[];
}
```

- [ ] **Step 2: Add API client functions**

Update the import in `frontend/src/lib/api-client.ts`:

```typescript
import type {
  SeedResult,
  EligibilityResponse,
  EncounterResponse,
  PatientResponse,
  PriorAuthResponse,
} from "@/types";
```

Add these functions before `downloadBlob`:

```typescript
export async function generatePriorAuth(
  encounterIds: string[]
): Promise<Blob> {
  const res = await fetch("/api/prior-auth/generate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ encounterIds }),
  });
  if (!res.ok)
    throw new Error(`Prior auth generation failed: ${res.statusText}`);
  return res.blob();
}

export async function parsePriorAuthResponse(
  file: File
): Promise<PriorAuthResponse> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch("/api/prior-auth/response", {
    method: "POST",
    body: formData,
  });
  if (!res.ok)
    throw new Error(`Prior auth response parsing failed: ${res.statusText}`);
  return res.json();
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/lib/api-client.ts
git commit -m "feat: add prior auth types and API client functions"
```

---

### Task 15: Frontend BFF API routes

**Files:**
- Create: `frontend/src/app/api/prior-auth/generate/route.ts`
- Create: `frontend/src/app/api/prior-auth/response/route.ts`

- [ ] **Step 1: Create generate route**

```typescript
// frontend/src/app/api/prior-auth/generate/route.ts
const PRIOR_AUTH_API =
  process.env.PRIOR_AUTH_API_URL || "http://localhost:8083";

export async function POST(request: Request) {
  const body = await request.json();
  const response = await fetch(
    `${PRIOR_AUTH_API}/api/prior-auth/generate`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );

  const data = await response.arrayBuffer();
  return new Response(data, {
    status: response.status,
    headers: {
      "Content-Type":
        response.headers.get("Content-Type") || "text/plain",
      "Content-Disposition":
        response.headers.get("Content-Disposition") || "",
    },
  });
}
```

- [ ] **Step 2: Create response route**

```typescript
// frontend/src/app/api/prior-auth/response/route.ts
const PRIOR_AUTH_API =
  process.env.PRIOR_AUTH_API_URL || "http://localhost:8083";

export async function POST(request: Request) {
  const formData = await request.formData();
  const response = await fetch(
    `${PRIOR_AUTH_API}/api/prior-auth/response`,
    {
      method: "POST",
      body: formData,
    }
  );
  const data = await response.json();
  return Response.json(data, { status: response.status });
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/api/prior-auth/
git commit -m "feat: add BFF proxy routes for prior auth"
```

---

### Task 16: Frontend prior auth page

**Files:**
- Create: `frontend/src/app/prior-auth/page.tsx`

- [ ] **Step 1: Check Next.js 16 docs for breaking changes**

Run: `ls frontend/node_modules/next/dist/docs/` and read any relevant guides before writing the page. Follow the exact patterns from `frontend/src/app/claims/page.tsx` (Generate tab) and `frontend/src/app/eligibility-response/page.tsx` (Response tab).

- [ ] **Step 2: Create the prior auth page**

```tsx
// frontend/src/app/prior-auth/page.tsx
"use client";

import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { GenerationLayout } from "@/components/generation-layout";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { Dropzone } from "@/components/dropzone";
import {
  fetchEncounters,
  generatePriorAuth,
  parsePriorAuthResponse,
} from "@/lib/api-client";
import type { EncounterResponse, PriorAuthResponse } from "@/types";

const columns: ColumnDef<EncounterResponse>[] = [
  { header: "Patient", accessor: "patientName" },
  { header: "Date of Service", accessor: "dateOfService" },
  { header: "Provider", accessor: "providerName" },
  {
    header: "Requested Procedures",
    accessor: "requestedProcedures",
    cell: (row) => (
      <div className="flex flex-wrap gap-1">
        {(row.requestedProcedures ?? []).map((rp) => (
          <Badge
            key={rp.procedureCode}
            variant="secondary"
            className="bg-purple-500/10 text-purple-600 dark:text-purple-400"
          >
            {rp.procedureCode}
          </Badge>
        ))}
      </div>
    ),
  },
  {
    header: "Auth Status",
    accessor: "authorizationNumber",
    cell: (row) =>
      row.authorizationNumber ? (
        <Badge variant="default">{row.authorizationNumber}</Badge>
      ) : (
        <Badge variant="outline">Pending</Badge>
      ),
  },
];

export default function PriorAuthPage() {
  const [tab, setTab] = useState<"generate" | "response">("generate");

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight">
              Prior Authorization
            </h1>
            <Badge variant="outline">EDI 278</Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Generate prior authorization requests and parse responses
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant={tab === "generate" ? "default" : "outline"}
            onClick={() => setTab("generate")}
          >
            Generate Request
          </Button>
          <Button
            variant={tab === "response" ? "default" : "outline"}
            onClick={() => setTab("response")}
          >
            Parse Response
          </Button>
        </div>
      </div>

      {tab === "generate" ? <GenerateTab /> : <ResponseTab />}
    </div>
  );
}

function GenerateTab() {
  const [encounters, setEncounters] = useState<EncounterResponse[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function loadEncounters() {
    setError(null);
    setFetching(true);
    fetchEncounters()
      .then(setEncounters)
      .catch((err) => {
        const message =
          err instanceof Error
            ? err.message
            : "Failed to fetch encounters";
        setError(message);
        toast.error(message);
      })
      .finally(() => setFetching(false));
  }

  useEffect(() => {
    loadEncounters();
  }, []);

  async function handleGenerate(ids: string[]) {
    setLoading(true);
    try {
      const blob = await generatePriorAuth(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 278 prior authorization request generated.");
    } catch (err) {
      toast.error(
        err instanceof Error
          ? err.message
          : "Failed to generate prior auth"
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <GenerationLayout
      title=""
      description=""
      badgeLabel="EDI 278"
      selectedCount={selectedIds.size}
      totalCount={encounters.length}
      isLoading={loading}
      onGenerateAll={() =>
        handleGenerate(encounters.map((e) => e.id))
      }
      onGenerateSelected={() =>
        handleGenerate([...selectedIds])
      }
      preview={preview}
      previewFilename="278_prior_auth.edi"
      onClosePreview={() => setPreview(null)}
    >
      {fetching ? (
        <div className="flex h-64 items-center justify-center text-muted-foreground">
          Loading encounters...
        </div>
      ) : error ? (
        <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
          <p>{error}</p>
          <Button variant="outline" onClick={loadEncounters}>
            Retry
          </Button>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={encounters}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          getId={(e) => e.id}
          emptyMessage="No encounters found. Try seeding the database from the sidebar."
        />
      )}
    </GenerationLayout>
  );
}

function ResponseTab() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PriorAuthResponse | null>(null);

  const handleFile = useCallback(async (file: File) => {
    setLoading(true);
    setResult(null);
    try {
      const response = await parsePriorAuthResponse(file);
      setResult(response);
      toast.success("EDI 278 response parsed successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error
          ? err.message
          : "Failed to parse response"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-semibold">
            Upload EDI 278 Response
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Dropzone onFileSelect={handleFile} />
          {loading && (
            <p className="mt-3 text-sm text-muted-foreground">
              Parsing response...
            </p>
          )}
        </CardContent>
      </Card>

      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">
              Response Details
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-xs font-medium uppercase text-muted-foreground">
                  Subscriber
                </p>
                <p className="mt-1 font-medium">
                  {result.subscriberFirstName}{" "}
                  {result.subscriberLastName}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase text-muted-foreground">
                  Member ID
                </p>
                <p className="mt-1 font-mono">{result.memberId}</p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase text-muted-foreground">
                  Payer
                </p>
                <p className="mt-1 font-medium">
                  {result.payerName}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {result?.decisions && result.decisions.length > 0 && (
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle className="text-sm font-semibold">
              Authorization Decisions
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Encounter</TableHead>
                  <TableHead>Decision</TableHead>
                  <TableHead>Authorization Number</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.decisions.map((d, i) => (
                  <TableRow key={i}>
                    <TableCell className="font-mono">
                      {d.encounterId}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          d.action === "CERTIFIED"
                            ? "default"
                            : "destructive"
                        }
                      >
                        {d.action}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-mono">
                      {d.authorizationNumber || "-"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/prior-auth/
git commit -m "feat: add prior authorization page with generate and response tabs"
```

---

### Task 17: Update dashboard and sidebar navigation

**Files:**
- Modify: `frontend/src/app/page.tsx`
- Modify: `frontend/src/components/app-sidebar.tsx`

- [ ] **Step 1: Add prior auth to dashboard**

In `frontend/src/app/page.tsx`, add the import for `ShieldCheck` from lucide-react:

```typescript
import { FileText, ArrowRightLeft, FileSearch, ShieldCheck } from "lucide-react";
```

Add a fourth item to the `workflows` array:

```typescript
{
    href: "/prior-auth",
    title: "Prior Authorization",
    badge: "EDI 278",
    description: "Generate prior authorization requests and parse responses.",
    detail: "Submit encounter IDs to request authorization, or upload responses to update records.",
    icon: ShieldCheck,
    primary: false,
},
```

Update the grid to accommodate 4 cards: change `md:grid-cols-3` to `md:grid-cols-2 lg:grid-cols-4`.

- [ ] **Step 2: Add prior auth to sidebar**

In `frontend/src/components/app-sidebar.tsx`, add the import for `ShieldCheck`:

```typescript
import {
  LayoutDashboard,
  FileText,
  ArrowRightLeft,
  FileSearch,
  ShieldCheck,
  Database,
} from "lucide-react";
```

Add a new entry to the `workflowItems` array:

```typescript
{ href: "/prior-auth", label: "Prior Auth (278)", icon: ShieldCheck },
```

- [ ] **Step 3: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/page.tsx frontend/src/components/app-sidebar.tsx
git commit -m "feat: add prior auth to dashboard and sidebar navigation"
```

---

### Task 18: Async data fetching in PriorAuthService

**Files:**
- Modify: `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/PriorAuthService.java`

The `generatePriorAuth` method currently fetches Patient, Insurance, Payer, and Practice sequentially for each encounter. Use `CompletableFuture` to parallelize the independent lookups.

- [ ] **Step 1: Refactor generatePriorAuth to use CompletableFuture**

Replace the sequential loop body in `generatePriorAuth` with parallel fetching. The full updated method:

```java
public String generatePriorAuth(List<String> encounterIds) {
    List<PriorAuthBundle> bundles = new ArrayList<>();

    for (String encounterId : encounterIds) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() ->
                        new EncounterNotFoundException(encounterId));

        CompletableFuture<Patient> patientFuture =
                CompletableFuture.supplyAsync(() ->
                        patientRepository.findById(encounter.getPatientId())
                                .orElseThrow(() ->
                                        new PatientNotFoundException(
                                                encounter.getPatientId())));

        CompletableFuture<PatientInsurance> insuranceFuture =
                CompletableFuture.supplyAsync(() ->
                        patientInsuranceRepository
                                .findByPatientIdAndTerminationDateIsNull(
                                        encounter.getPatientId())
                                .orElseThrow(() ->
                                        new InsuranceNotFoundException(
                                                encounter.getPatientId())));

        CompletableFuture<Practice> practiceFuture =
                CompletableFuture.supplyAsync(() ->
                        practiceRepository.findById(encounter.getPracticeId())
                                .orElseThrow(() ->
                                        new PracticeNotFoundException(
                                                encounter.getPracticeId())));

        Patient patient = patientFuture.join();
        PatientInsurance insurance = insuranceFuture.join();
        Practice practice = practiceFuture.join();

        Payer payer = payerRepository.findById(insurance.getPayerId())
                .orElseThrow(() ->
                        new PayerNotFoundException(
                                insurance.getPayerId()));

        List<RequestedProcedure> requestedProcedures =
                encounter.getRequestedProcedures() != null
                        ? encounter.getRequestedProcedures()
                        : List.of();

        bundles.add(new PriorAuthBundle(
                encounter, patient, insurance, payer, practice,
                requestedProcedures));
    }

    EDI278Request request = edi278Mapper.map(
            bundles, interchangeProperties);
    return edi278Generator.generate(request);
}
```

Add the import at the top of the file:

```java
import java.util.concurrent.CompletableFuture;
```

Note: `Payer` lookup depends on `insurance.getPayerId()` so it cannot be parallelized with the insurance lookup. Patient, Insurance, and Practice are independent and run concurrently.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :prior-auth-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all prior-auth-app tests**

Run: `./gradlew :prior-auth-app:test -i`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/PriorAuthService.java
git commit -m "feat: use CompletableFuture for parallel data fetching in PriorAuthService"
```

---

### Task 19: Cloud deployment -- Dockerfile, cloud profile, CI/CD, and secrets

**Files:**
- Create: `backend/prior-auth-app/Dockerfile`
- Create: `backend/prior-auth-app/src/main/resources/application-cloud.yml`
- Modify: `.github/workflows/ci.yml`
- Modify: `scripts/setup_gcloud_project.sh`

- [ ] **Step 1: Create Dockerfile**

```dockerfile
# backend/prior-auth-app/Dockerfile

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Copy module build files
COPY backend/common/build.gradle backend/common/build.gradle
COPY backend/claims-app/build.gradle backend/claims-app/build.gradle
COPY backend/insurance-request-app/build.gradle backend/insurance-request-app/build.gradle
COPY backend/insurance-response-app/build.gradle backend/insurance-response-app/build.gradle
COPY backend/prior-auth-app/build.gradle backend/prior-auth-app/build.gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY backend/common/src backend/common/src
COPY backend/prior-auth-app/src backend/prior-auth-app/src

# Build the boot JAR
RUN ./gradlew :prior-auth-app:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/backend/prior-auth-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create application-cloud.yml**

```yaml
# backend/prior-auth-app/src/main/resources/application-cloud.yml
server:
  port: ${PORT:8080}

spring:
  data:
    mongodb:
      uri: "${MONGODB_URI}"
      database: ${MONGODB_DATABASE:edi_healthcare}
  mongodb:
    uri: "${MONGODB_URI}"
```

- [ ] **Step 3: Update CI/CD workflow -- add prior-auth-app to workflow_dispatch options**

In `.github/workflows/ci.yml`, in the `workflow_dispatch.inputs.app.options` list, add `prior-auth-app` after `insurance-response-app`:

```yaml
        options:
          - claims-app
          - insurance-request-app
          - insurance-response-app
          - prior-auth-app
          - frontend
          - all
```

- [ ] **Step 4: Update CI/CD workflow -- add prior-auth-app to change detection**

In the `detect-changes` job, in the `Determine affected apps` step:

Update the shared files check to include prior-auth-app in the triggered list. Replace:

```bash
            APPS=("claims-app" "insurance-request-app" "insurance-response-app")
```

with:

```bash
            APPS=("claims-app" "insurance-request-app" "insurance-response-app" "prior-auth-app")
```

Add a new detection block after the `insurance-response-app` check:

```bash
            if echo "$CHANGED_FILES" | grep -q '^backend/prior-auth-app/'; then
              APPS+=("prior-auth-app")
            fi
```

- [ ] **Step 5: Update CI/CD workflow -- add all app options to include prior-auth-app**

In the `Determine affected apps` step, update the `all` case. Replace:

```bash
              echo 'apps=["claims-app","insurance-request-app","insurance-response-app","frontend"]' >> "$GITHUB_OUTPUT"
```

with:

```bash
              echo 'apps=["claims-app","insurance-request-app","insurance-response-app","prior-auth-app","frontend"]' >> "$GITHUB_OUTPUT"
```

- [ ] **Step 6: Update CI/CD workflow -- add PRIOR_AUTH_API_URL to frontend deploy secrets (dev)**

In the `deploy-dev` job, in the `Deploy frontend to Cloud Run (dev)` step, update the `--set-secrets` line. Replace:

```bash
            --set-secrets "CLAIMS_API_URL=claims-api-url-dev:latest,REQUEST_API_URL=request-api-url-dev:latest,RESPONSE_API_URL=response-api-url-dev:latest" \
```

with:

```bash
            --set-secrets "CLAIMS_API_URL=claims-api-url-dev:latest,REQUEST_API_URL=request-api-url-dev:latest,RESPONSE_API_URL=response-api-url-dev:latest,PRIOR_AUTH_API_URL=prior-auth-api-url-dev:latest" \
```

- [ ] **Step 7: Update CI/CD workflow -- add PRIOR_AUTH_API_URL to frontend deploy secrets (prod)**

In the `deploy-prod` job, in the `Deploy frontend to Cloud Run (prod)` step, update the `--set-secrets` line. Replace:

```bash
            --set-secrets "CLAIMS_API_URL=claims-api-url-prod:latest,REQUEST_API_URL=request-api-url-prod:latest,RESPONSE_API_URL=response-api-url-prod:latest" \
```

with:

```bash
            --set-secrets "CLAIMS_API_URL=claims-api-url-prod:latest,REQUEST_API_URL=request-api-url-prod:latest,RESPONSE_API_URL=response-api-url-prod:latest,PRIOR_AUTH_API_URL=prior-auth-api-url-prod:latest" \
```

- [ ] **Step 8: Update setup script -- add PRIOR_AUTH_API_URL**

In `scripts/setup_gcloud_project.sh`, add the `PRIOR_AUTH_API_URL` variable in both the dev and prod config blocks.

In the dev block, after `RESPONSE_API_URL=...` add:

```bash
  PRIOR_AUTH_API_URL="https://prior-auth-app-dev-1053092970650.us-central1.run.app"
```

In the prod block, after `RESPONSE_API_URL=...` add:

```bash
  PRIOR_AUTH_API_URL="https://prior-auth-app-prod-PLACEHOLDER.us-central1.run.app"
```

At the end of Step 7 (storing frontend API URL secrets), after `create_or_update_secret "response-api-url-$ENV" "$RESPONSE_API_URL"`, add:

```bash
create_or_update_secret "prior-auth-api-url-$ENV" "$PRIOR_AUTH_API_URL"
```

- [ ] **Step 9: Commit**

```bash
git add backend/prior-auth-app/Dockerfile backend/prior-auth-app/src/main/resources/application-cloud.yml .github/workflows/ci.yml scripts/setup_gcloud_project.sh
git commit -m "feat: add cloud deployment config for prior-auth-app"
```

---

### Task 20: Refactor data model — move auth fields from Encounter to EncounterProcedure

The original design used a separate `RequestedProcedure` embedded list on `Encounter`. This is being replaced: procedures needing prior auth are already in `encounter_procedures`, so we add `needsAuth` and `clinicalReason` directly to `EncounterProcedure`.

**Files to modify:**

| Layer | File | Change |
|-------|------|--------|
| Common | `EncounterProcedure.java` | Add `boolean needsAuth` and `String clinicalReason` with getters/setters |
| Common | `Encounter.java` | Remove `requestedProcedures` field, getter, setter, and `List` import |
| Common | `RequestedProcedure.java` | Delete this file entirely |
| Claims DTO | `RequestedProcedureResponse.java` | Delete this file entirely |
| Claims DTO | `ProcedureResponse.java` | Add `boolean needsAuth` and `String clinicalReason` to the record |
| Claims DTO | `EncounterResponse.java` | Remove `requestedProcedures` field (last parameter) |
| Claims Service | `EncounterService.java` | Remove `requestedProcedures` mapping; add `needsAuth` and `clinicalReason` to `ProcedureResponse` constructor; remove `RequestedProcedure` import |
| Claims Test | `EncounterControllerTest.java` | Update `EncounterResponse` constructor (remove last param) |
| Claims Seed | `DevSeedController.java` | Remove `setRequestedProcedures` calls; add `setNeedsAuth(true)` and `setClinicalReason(...)` on specific `EncounterProcedure` objects |
| Prior-auth DTO | `PriorAuthBundle.java` | Replace `List<RequestedProcedure>` with `List<EncounterProcedure>` (filtered to needsAuth) |
| Prior-auth Service | `PriorAuthService.java` | Fetch `EncounterProcedure` list from `EncounterProcedureRepository`, filter `needsAuth == true`, pass to bundle |
| Prior-auth Mapper | `EDI278Mapper.java` | Change `bundle.requestedProcedures()` to `bundle.authProcedures()`, map from `EncounterProcedure` fields |
| Prior-auth Test | `EDI278MapperTest.java` | Use `EncounterProcedure` with `needsAuth`/`clinicalReason` instead of `RequestedProcedure` |
| Frontend types | `types/index.ts` | Remove `RequestedProcedureResponse` and `requestedProcedures` from `EncounterResponse`; add `needsAuth` and `clinicalReason` to `ProcedureResponse` |
| Frontend page | `prior-auth/page.tsx` | Update "Requested Procedures" column to filter `row.procedures` where `needsAuth` is true |

- [ ] **Step 1: Update EncounterProcedure — add needsAuth and clinicalReason**

In `backend/common/src/main/java/com/example/edi/common/document/EncounterProcedure.java`, add after `private List<Integer> diagnosisPointers;`:

```java
private boolean needsAuth;
private String clinicalReason;
```

Add getters/setters:

```java
public boolean isNeedsAuth() { return needsAuth; }
public void setNeedsAuth(boolean needsAuth) { this.needsAuth = needsAuth; }

public String getClinicalReason() { return clinicalReason; }
public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }
```

- [ ] **Step 2: Remove RequestedProcedure from Encounter**

In `backend/common/src/main/java/com/example/edi/common/document/Encounter.java`:
- Remove `private List<RequestedProcedure> requestedProcedures;`
- Remove getter `getRequestedProcedures()` and setter `setRequestedProcedures(...)`
- Remove `import java.util.List;`

- [ ] **Step 3: Delete RequestedProcedure.java**

Delete `backend/common/src/main/java/com/example/edi/common/document/RequestedProcedure.java`

- [ ] **Step 4: Update ProcedureResponse — add needsAuth and clinicalReason**

Replace `backend/claims-app/src/main/java/com/example/edi/claims/dto/ProcedureResponse.java`:

```java
package com.example.edi.claims.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProcedureResponse(
        String procedureCode,
        List<String> modifiers,
        BigDecimal chargeAmount,
        int units,
        boolean needsAuth,
        String clinicalReason
) {}
```

- [ ] **Step 5: Delete RequestedProcedureResponse.java**

Delete `backend/claims-app/src/main/java/com/example/edi/claims/dto/RequestedProcedureResponse.java`

- [ ] **Step 6: Remove requestedProcedures from EncounterResponse**

Replace `backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java`:

```java
package com.example.edi.claims.dto;

import java.time.LocalDate;
import java.util.List;

public record EncounterResponse(
        String id,
        String patientId,
        String patientName,
        String providerId,
        String providerName,
        String facilityId,
        String facilityName,
        LocalDate dateOfService,
        String authorizationNumber,
        List<DiagnosisResponse> diagnoses,
        List<ProcedureResponse> procedures
) {}
```

- [ ] **Step 7: Update EncounterService mapping**

In `backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java`:
- Remove the `requestedProcedures` mapping block and `RequestedProcedure` import
- Update the `ProcedureResponse` constructor in the procedures mapping to include `needsAuth` and `clinicalReason`:

```java
List<ProcedureResponse> procedures = proceduresByEncounterId
        .getOrDefault(encounter.getId(), List.of()).stream()
        .map(p -> new ProcedureResponse(
                p.getProcedureCode(),
                p.getModifiers(),
                p.getChargeAmount(),
                p.getUnits(),
                p.isNeedsAuth(),
                p.getClinicalReason()))
        .toList();
```

- Remove `requestedProcedures` from the `EncounterResponse` constructor call (it was the last parameter).

- [ ] **Step 8: Update EncounterControllerTest**

In `backend/claims-app/src/test/java/com/example/edi/claims/controller/EncounterControllerTest.java`, remove the `List.of()` that was the last argument in the `EncounterResponse` constructor.

- [ ] **Step 9: Update DevSeedController**

In `backend/claims-app/src/main/java/com/example/edi/claims/controller/DevSeedController.java`:
- Remove `setRequestedProcedures(...)` calls from both encounters
- Add `needsAuth` and `clinicalReason` to specific `EncounterProcedure` objects:

On `proc1a` (99213):
```java
proc1a.setNeedsAuth(true);
proc1a.setClinicalReason("Acute upper respiratory infection follow-up");
```

On `proc2a` (99214):
```java
proc2a.setNeedsAuth(true);
proc2a.setClinicalReason("Chronic low back pain evaluation");
```

On `proc2b` (97140):
```java
proc2b.setNeedsAuth(true);
proc2b.setClinicalReason("Manual therapy for lumbar spine dysfunction");
```

Leave `proc1b` (87880) without needsAuth (defaults to false).

- [ ] **Step 10: Build claims-app**

Run: `./gradlew :claims-app:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Update PriorAuthBundle**

Replace `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/dto/PriorAuthBundle.java`:

```java
package com.example.edi.priorauth.dto;

import com.example.edi.common.document.*;
import java.util.List;

public record PriorAuthBundle(
        Encounter encounter,
        Patient patient,
        PatientInsurance insurance,
        Payer payer,
        Practice practice,
        List<EncounterProcedure> authProcedures
) {}
```

- [ ] **Step 12: Update PriorAuthService — fetch EncounterProcedures**

In `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/PriorAuthService.java`:

Add `EncounterProcedureRepository` injection (field + constructor parameter).

Replace the `requestedProcedures` block in `generatePriorAuth` with:

```java
List<EncounterProcedure> authProcedures = encounterProcedureRepository
        .findByEncounterIdOrderByLineNumberAsc(encounterId).stream()
        .filter(EncounterProcedure::isNeedsAuth)
        .toList();

bundles.add(new PriorAuthBundle(encounter, patient, insurance, payer, practice, authProcedures));
```

Remove the `RequestedProcedure` import.

- [ ] **Step 13: Update EDI278Mapper**

In `backend/prior-auth-app/src/main/java/com/example/edi/priorauth/service/EDI278Mapper.java`:

Change the services mapping from `bundle.requestedProcedures()` to `bundle.authProcedures()`, and map from `EncounterProcedure` fields:

```java
List<ServiceReviewInfo> services = bundle.authProcedures().stream()
        .map(ep -> new ServiceReviewInfo(
                ep.getProcedureCode(), ep.getClinicalReason(), serviceDate))
        .toList();
```

Replace the `RequestedProcedure`-related import with `EncounterProcedure`:
```java
import com.example.edi.common.document.EncounterProcedure;
```

- [ ] **Step 14: Update EDI278MapperTest**

In `backend/prior-auth-app/src/test/java/com/example/edi/priorauth/service/EDI278MapperTest.java`:

Replace `RequestedProcedure` usage with `EncounterProcedure`:

```java
EncounterProcedure proc = new EncounterProcedure();
proc.setProcedureCode("99213");
proc.setNeedsAuth(true);
proc.setClinicalReason("Chronic pain management");
proc.setLineNumber(1);
proc.setUnits(1);
proc.setUnitType("UN");
proc.setChargeAmount(new java.math.BigDecimal("150.00"));
proc.setModifiers(List.of());
proc.setDiagnosisPointers(List.of(1));
```

Update the bundle construction:
```java
return new PriorAuthBundle(encounter, patient, insurance, payer, practice,
        List.of(proc));
```

Remove `encounter.setRequestedProcedures(...)` call.

- [ ] **Step 15: Build prior-auth-app**

Run: `./gradlew :prior-auth-app:test`
Expected: All tests PASS

- [ ] **Step 16: Update frontend types**

In `frontend/src/types/index.ts`:
- Remove `RequestedProcedureResponse` interface
- Remove `requestedProcedures: RequestedProcedureResponse[];` from `EncounterResponse`
- Add `needsAuth: boolean;` and `clinicalReason: string | null;` to `ProcedureResponse`

- [ ] **Step 17: Update prior-auth page**

In `frontend/src/app/prior-auth/page.tsx`, update the "Requested Procedures" column to filter from `row.procedures`:

```tsx
{
    header: "Needs Auth",
    accessor: "procedures",
    cell: (row) => {
      const authProcs = (row.procedures ?? []).filter((p) => p.needsAuth);
      return (
        <div className="flex flex-wrap gap-1">
          {authProcs.length > 0 ? (
            authProcs.map((p) => (
              <Badge
                key={p.procedureCode}
                variant="secondary"
                className="bg-purple-500/10 text-purple-600 dark:text-purple-400"
              >
                {p.procedureCode}
              </Badge>
            ))
          ) : (
            <span className="text-xs text-muted-foreground">—</span>
          )}
        </div>
      );
    },
},
```

- [ ] **Step 18: Build frontend**

Run: `cd frontend && npm run build && npm run lint`
Expected: Both pass

- [ ] **Step 19: Full build verification**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL for all modules

- [ ] **Step 20: Commit**

```bash
git add -A
git commit -m "refactor: move prior auth fields from Encounter to EncounterProcedure

Replace RequestedProcedure embedded list on Encounter with needsAuth and
clinicalReason fields on EncounterProcedure. Procedures requiring prior
auth are now flagged directly in the encounter_procedures collection."
```

---

### Task 21: Full build verification

- [ ] **Step 1: Run full backend build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL for all modules

- [ ] **Step 2: Run full frontend build**

Run: `cd frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 3: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit any fixes if needed**

If any issues were found, fix and commit them.
