# EDI 270 Eligibility Request Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite the insurance-request-app to generate EDI 270 5010 eligibility inquiry files for multiple patients grouped by payer, using StAEDI and the three-layer architecture (Mapper, Generator, Service).

**Architecture:** Three-layer pattern matching claims-app: EDI270Mapper maps entities to loop records, EDI270Generator writes loop records to EDI via StAEDI, InsuranceRequestService orchestrates repo lookups and delegates. New 270-specific loop records live in the insurance-request-app domain/loop package; shared records from common are reused.

**Tech Stack:** Spring Boot 4.0.4, Java 21, MongoDB, StAEDI 1.25.3 (via common module), Testcontainers

---

## File Map

**Create:**
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/EDI270Inquiry.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/InformationSourceGroup.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/InformationReceiverGroup.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/EligibilitySubscriber.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/EligibilityBundle.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/config/InterchangeProperties.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Mapper.java`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Generator.java`
- `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270MapperTest.java` (replace existing)
- `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270GeneratorTest.java` (replace existing)
- `insurance-request-app/src/test/java/com/example/edi/insurancerequest/EligibilityInquiryIT.java`

**Modify:**
- `insurance-request-app/build.gradle` -- add StAEDI, Testcontainers deps, Docker config
- `insurance-request-app/src/main/resources/application.yml` -- add `edi.interchange` config
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/InsuranceRequestApplication.java` -- add `@EnableConfigurationProperties`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/InsuranceRequestDTO.java` -- change to `List<String> patientIds`
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/controller/InsuranceRequestController.java` -- update to pass list
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/InsuranceRequestService.java` -- full rewrite
- `insurance-request-app/src/test/java/com/example/edi/insurancerequest/controller/InsuranceRequestControllerTest.java` -- update for new DTO
- `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/InsuranceRequestServiceTest.java` -- full rewrite

**Delete:**
- `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Service.java` -- replaced by EDI270Mapper + EDI270Generator

---

### Task 1: Add Dependencies and Configuration

**Files:**
- Modify: `insurance-request-app/build.gradle`
- Modify: `insurance-request-app/src/main/resources/application.yml`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/config/InterchangeProperties.java`
- Modify: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/InsuranceRequestApplication.java`

- [ ] **Step 1: Update build.gradle**

Replace the full contents of `insurance-request-app/build.gradle`:

```groovy
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'io.xlate:staedi:1.25.3'
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

- [ ] **Step 2: Add interchange config to application.yml**

Add the `edi.interchange` section to the end of `insurance-request-app/src/main/resources/application.yml`:

```yaml
edi:
  interchange:
    sender-id-qualifier: ZZ
    sender-id: SENDER_ID
    receiver-id-qualifier: ZZ
    receiver-id: RECEIVER_ID
    ack-requested: "0"
    usage-indicator: T
```

- [ ] **Step 3: Create InterchangeProperties record**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/config/InterchangeProperties.java`:

```java
package com.example.edi.insurancerequest.config;

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

- [ ] **Step 4: Enable config properties on the application class**

Update `InsuranceRequestApplication.java` to add `@EnableConfigurationProperties`:

```java
package com.example.edi.insurancerequest;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.example.edi.insurancerequest", "com.example.edi.common"})
@EnableConfigurationProperties(InterchangeProperties.class)
public class InsuranceRequestApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsuranceRequestApplication.class, args);
    }
}
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew :insurance-request-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add insurance-request-app/build.gradle \
      insurance-request-app/src/main/resources/application.yml \
      insurance-request-app/src/main/java/com/example/edi/insurancerequest/config/InterchangeProperties.java \
      insurance-request-app/src/main/java/com/example/edi/insurancerequest/InsuranceRequestApplication.java
git commit -m "feat: add StAEDI, Testcontainers deps and interchange config to insurance-request-app"
```

---

### Task 2: Create 270-Specific Loop Records and EligibilityBundle

**Files:**
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/EDI270Inquiry.java`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/InformationSourceGroup.java`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/InformationReceiverGroup.java`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/EligibilitySubscriber.java`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/EligibilityBundle.java`

- [ ] **Step 1: Create EDI270Inquiry record**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/EDI270Inquiry.java`:

```java
package com.example.edi.insurancerequest.domain.loop;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;

import java.util.List;

public record EDI270Inquiry(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    List<InformationSourceGroup> informationSourceGroups
) {}
```

- [ ] **Step 2: Create InformationSourceGroup record**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/InformationSourceGroup.java`:

```java
package com.example.edi.insurancerequest.domain.loop;

public record InformationSourceGroup(
    String payerName,
    String payerId,
    InformationReceiverGroup informationReceiver
) {}
```

- [ ] **Step 3: Create InformationReceiverGroup record**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/InformationReceiverGroup.java`:

```java
package com.example.edi.insurancerequest.domain.loop;

import java.util.List;

public record InformationReceiverGroup(
    String providerName,
    String providerNpi,
    String providerTaxId,
    List<EligibilitySubscriber> subscribers
) {}
```

- [ ] **Step 4: Create EligibilitySubscriber record**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/EligibilitySubscriber.java`:

```java
package com.example.edi.insurancerequest.domain.loop;

import com.example.edi.common.edi.loop.SubscriberLoop;

public record EligibilitySubscriber(
    SubscriberLoop subscriber,
    String eligibilityDate
) {}
```

- [ ] **Step 5: Create EligibilityBundle record**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/EligibilityBundle.java`:

```java
package com.example.edi.insurancerequest.dto;

import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;

public record EligibilityBundle(
    Patient patient,
    PatientInsurance insurance,
    Payer payer
) {}
```

- [ ] **Step 6: Verify build compiles**

Run: `./gradlew :insurance-request-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/ \
      insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/EligibilityBundle.java
git commit -m "feat: add 270-specific loop records and EligibilityBundle"
```

---

### Task 3: Implement EDI270Mapper with TDD

**Files:**
- Create: `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270MapperTest.java`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Mapper.java`

- [ ] **Step 1: Write the failing tests**

Delete old `EDI270ServiceTest.java` and create `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270MapperTest.java`:

```java
package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDI270MapperTest {

    private EDI270Mapper mapper;
    private InterchangeProperties props;
    private Practice practice;
    private Patient patient;
    private PatientInsurance insurance;
    private Payer payer;

    @BeforeEach
    void setUp() {
        mapper = new EDI270Mapper();
        props = new InterchangeProperties("ZZ", "SENDER001", "ZZ", "RECEIVER01", "0", "T");

        practice = new Practice();
        practice.setName("SUNSHINE HEALTH CLINIC");
        practice.setNpi("1234567890");
        practice.setTaxId("591234567");

        patient = new Patient();
        patient.setId("P001");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");
        patient.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient.setGender("M");
        patient.setAddress("456 OAK AVENUE");
        patient.setCity("ORLANDO");
        patient.setState("FL");
        patient.setZipCode("32806");

        payer = new Payer();
        payer.setName("BLUE CROSS BLUE SHIELD");
        payer.setPayerId("BCBS12345");

        insurance = new PatientInsurance();
        insurance.setPatientId("P001");
        insurance.setPayerId("payer1");
        insurance.setMemberId("MEM987654321");
        insurance.setGroupNumber("GRP100234");
        insurance.setPolicyType("MC");
        insurance.setSubscriberRelationship("self");
    }

    private EligibilityBundle makeBundle() {
        return new EligibilityBundle(patient, insurance, payer);
    }

    private EDI270Inquiry doMap() {
        return mapper.map(practice, List.of(makeBundle()), props);
    }

    @Test
    void map_envelopeUsesConfigProperties() {
        EDI270Inquiry inquiry = doMap();

        assertThat(inquiry.envelope().senderIdQualifier()).isEqualTo("ZZ");
        assertThat(inquiry.envelope().senderId()).isEqualTo("SENDER001");
        assertThat(inquiry.envelope().receiverIdQualifier()).isEqualTo("ZZ");
        assertThat(inquiry.envelope().receiverId()).isEqualTo("RECEIVER01");
        assertThat(inquiry.envelope().ackRequested()).isEqualTo("0");
        assertThat(inquiry.envelope().usageIndicator()).isEqualTo("T");
    }

    @Test
    void map_singlePatient_producesOneInformationSourceGroup() {
        EDI270Inquiry inquiry = doMap();

        assertThat(inquiry.informationSourceGroups()).hasSize(1);
        InformationSourceGroup group = inquiry.informationSourceGroups().getFirst();
        assertThat(group.payerName()).isEqualTo("BLUE CROSS BLUE SHIELD");
        assertThat(group.payerId()).isEqualTo("BCBS12345");
    }

    @Test
    void map_informationReceiverFromPractice() {
        EDI270Inquiry inquiry = doMap();

        var receiver = inquiry.informationSourceGroups().getFirst().informationReceiver();
        assertThat(receiver.providerName()).isEqualTo("SUNSHINE HEALTH CLINIC");
        assertThat(receiver.providerNpi()).isEqualTo("1234567890");
        assertThat(receiver.providerTaxId()).isEqualTo("591234567");
    }

    @Test
    void map_subscriberFromPatientAndInsurance() {
        EDI270Inquiry inquiry = doMap();

        var subscriber = inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber();
        assertThat(subscriber.firstName()).isEqualTo("JOHN");
        assertThat(subscriber.lastName()).isEqualTo("SMITH");
        assertThat(subscriber.memberId()).isEqualTo("MEM987654321");
        assertThat(subscriber.groupNumber()).isEqualTo("GRP100234");
        assertThat(subscriber.address()).isEqualTo("456 OAK AVENUE");
        assertThat(subscriber.city()).isEqualTo("ORLANDO");
        assertThat(subscriber.state()).isEqualTo("FL");
        assertThat(subscriber.zipCode()).isEqualTo("32806");
    }

    @Test
    void map_dateOfBirthFormattedAsYYYYMMDD() {
        EDI270Inquiry inquiry = doMap();

        var subscriber = inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber();
        assertThat(subscriber.dateOfBirth()).isEqualTo("19850715");
    }

    @Test
    void map_genderMappedCorrectly() {
        EDI270Inquiry inquiryM = doMap();
        assertThat(inquiryM.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber().genderCode()).isEqualTo("M");

        patient.setGender("F");
        EDI270Inquiry inquiryF = doMap();
        assertThat(inquiryF.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber().genderCode()).isEqualTo("F");

        patient.setGender(null);
        EDI270Inquiry inquiryNull = doMap();
        assertThat(inquiryNull.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber().genderCode()).isEqualTo("U");
    }

    @Test
    void map_eligibilityDateIsToday() {
        EDI270Inquiry inquiry = doMap();

        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        var eligDate = inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().eligibilityDate();
        assertThat(eligDate).isEqualTo(today);
    }

    @Test
    void map_twoPatientsSamePayer_producesOneGroupWithTwoSubscribers() {
        Patient patient2 = new Patient();
        patient2.setId("P002");
        patient2.setFirstName("JANE");
        patient2.setLastName("DOE");
        patient2.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient2.setGender("F");
        patient2.setAddress("789 ELM ST");
        patient2.setCity("ORLANDO");
        patient2.setState("FL");
        patient2.setZipCode("32807");

        PatientInsurance insurance2 = new PatientInsurance();
        insurance2.setPatientId("P002");
        insurance2.setPayerId("payer1");
        insurance2.setMemberId("MEM111222333");
        insurance2.setGroupNumber("GRP200345");
        insurance2.setPolicyType("MC");
        insurance2.setSubscriberRelationship("self");

        EligibilityBundle bundle1 = makeBundle();
        EligibilityBundle bundle2 = new EligibilityBundle(patient2, insurance2, payer);

        EDI270Inquiry inquiry = mapper.map(practice, List.of(bundle1, bundle2), props);

        assertThat(inquiry.informationSourceGroups()).hasSize(1);
        assertThat(inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers()).hasSize(2);
    }

    @Test
    void map_twoPatientsDifferentPayers_producesTwoGroups() {
        Payer payer2 = new Payer();
        payer2.setName("AETNA");
        payer2.setPayerId("AETNA001");

        Patient patient2 = new Patient();
        patient2.setId("P002");
        patient2.setFirstName("JANE");
        patient2.setLastName("DOE");
        patient2.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient2.setGender("F");
        patient2.setAddress("789 ELM ST");
        patient2.setCity("ORLANDO");
        patient2.setState("FL");
        patient2.setZipCode("32807");

        PatientInsurance insurance2 = new PatientInsurance();
        insurance2.setPatientId("P002");
        insurance2.setPayerId("payer2");
        insurance2.setMemberId("MEM444555666");
        insurance2.setGroupNumber("GRP300456");
        insurance2.setPolicyType("MC");
        insurance2.setSubscriberRelationship("self");

        EligibilityBundle bundle1 = makeBundle();
        EligibilityBundle bundle2 = new EligibilityBundle(patient2, insurance2, payer2);

        EDI270Inquiry inquiry = mapper.map(practice, List.of(bundle1, bundle2), props);

        assertThat(inquiry.informationSourceGroups()).hasSize(2);
        assertThat(inquiry.informationSourceGroups().get(0).payerName()).isEqualTo("BLUE CROSS BLUE SHIELD");
        assertThat(inquiry.informationSourceGroups().get(1).payerName()).isEqualTo("AETNA");
        assertThat(inquiry.informationSourceGroups().get(0)
                .informationReceiver().subscribers()).hasSize(1);
        assertThat(inquiry.informationSourceGroups().get(1)
                .informationReceiver().subscribers()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.service.EDI270MapperTest" --info`
Expected: Compilation failure -- `EDI270Mapper` class does not exist yet.

- [ ] **Step 3: Implement EDI270Mapper**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Mapper.java`:

```java
package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.EligibilitySubscriber;
import com.example.edi.insurancerequest.domain.loop.InformationReceiverGroup;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EDI270Mapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HHmm");

    public EDI270Inquiry map(Practice practice,
                             List<EligibilityBundle> bundles,
                             InterchangeProperties props) {

        LocalDateTime now = LocalDateTime.now();
        String nowDate   = now.format(YYYYMMDD);
        String nowYYMMDD = now.format(YYMMDD);
        String nowTime   = now.format(HHMM);

        long millis        = System.currentTimeMillis();
        String controlNum9 = String.format("%09d", millis % 1_000_000_000L);
        String controlNum5 = String.format("%05d", millis % 100_000L);

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                props.senderIdQualifier(),
                props.senderId(),
                props.receiverIdQualifier(),
                props.receiverId(),
                nowYYMMDD,
                nowTime,
                controlNum9,
                props.ackRequested(),
                props.usageIndicator()
        );

        FunctionalGroup functionalGroup = new FunctionalGroup(
                props.senderId(),
                props.receiverId(),
                nowDate,
                nowTime,
                controlNum5
        );

        TransactionHeader transactionHeader = new TransactionHeader(
                controlNum5,
                "270",
                nowDate,
                nowTime
        );

        String today = LocalDate.now().format(YYYYMMDD);

        // Group bundles by payer ID
        Map<String, List<EligibilityBundle>> grouped = new LinkedHashMap<>();
        for (EligibilityBundle bundle : bundles) {
            String key = bundle.payer().getPayerId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(bundle);
        }

        List<InformationSourceGroup> sourceGroups = new ArrayList<>();
        for (List<EligibilityBundle> payerBundles : grouped.values()) {
            EligibilityBundle representative = payerBundles.getFirst();
            Payer payer = representative.payer();

            List<EligibilitySubscriber> subscribers = new ArrayList<>();
            for (EligibilityBundle bundle : payerBundles) {
                Patient patient = bundle.patient();
                PatientInsurance ins = bundle.insurance();

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
                        payer.getPayerId()
                );

                subscribers.add(new EligibilitySubscriber(subscriberLoop, today));
            }

            InformationReceiverGroup receiverGroup = new InformationReceiverGroup(
                    practice.getName(),
                    practice.getNpi(),
                    practice.getTaxId(),
                    subscribers
            );

            sourceGroups.add(new InformationSourceGroup(
                    payer.getName(),
                    payer.getPayerId(),
                    receiverGroup
            ));
        }

        return new EDI270Inquiry(envelope, functionalGroup, transactionHeader, sourceGroups);
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

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.service.EDI270MapperTest" --info`
Expected: All 9 tests PASS

- [ ] **Step 5: Commit**

```bash
git add insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Mapper.java \
      insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270MapperTest.java
git commit -m "feat: add EDI270Mapper with TDD tests"
```

---

### Task 4: Implement EDI270Generator with TDD

**Files:**
- Create: `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270GeneratorTest.java`
- Create: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Generator.java`

- [ ] **Step 1: Write the failing tests**

Delete old `EDI270ServiceTest.java` and create `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270GeneratorTest.java`:

```java
package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.EligibilitySubscriber;
import com.example.edi.insurancerequest.domain.loop.InformationReceiverGroup;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDI270GeneratorTest {

    private EDI270Generator generator;
    private InterchangeEnvelope envelope;
    private FunctionalGroup functionalGroup;
    private TransactionHeader transactionHeader;

    @BeforeEach
    void setUp() {
        generator = new EDI270Generator();

        envelope = new InterchangeEnvelope(
            "ZZ", "SENDER001", "ZZ", "RECEIVER01",
            "260315", "1430", "000000001", "0", "T"
        );

        functionalGroup = new FunctionalGroup(
            "SENDER001", "RECEIVER01", "20260315", "1430", "00001"
        );

        transactionHeader = new TransactionHeader(
            "00001", "270", "20260315", "1430"
        );
    }

    private SubscriberLoop makeSubscriber(String lastName, String firstName, String memberId) {
        return new SubscriberLoop(
            "P", "GRP100234", "MC",
            lastName, firstName, memberId,
            "456 OAK AVENUE", "ORLANDO", "FL", "32806",
            "19850715", "M",
            "BLUE CROSS BLUE SHIELD", "BCBS12345"
        );
    }

    private InformationSourceGroup makeSourceGroup(String payerName, String payerId,
                                                    List<EligibilitySubscriber> subscribers) {
        var receiver = new InformationReceiverGroup(
            "SUNSHINE HEALTH CLINIC", "1234567890", "591234567", subscribers
        );
        return new InformationSourceGroup(payerName, payerId, receiver);
    }

    private EDI270Inquiry buildInquiry(List<InformationSourceGroup> groups) {
        return new EDI270Inquiry(envelope, functionalGroup, transactionHeader, groups);
    }

    @Test
    void generate_singleSubscriber_producesValidISASegment() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).startsWith("ISA*");
        assertThat(edi).contains("*00501*");
        assertThat(edi).contains("*SENDER001      *");
    }

    @Test
    void generate_singleSubscriber_producesCorrectGSVersion() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("GS*HS*");
        assertThat(edi).contains("005010X279A1");
    }

    @Test
    void generate_singleSubscriber_producesCorrectSTAndBHT() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("ST*270*00001*005010X279A1~");
        assertThat(edi).contains("BHT*0022*13*270*20260315*1430~");
    }

    @Test
    void generate_singleSubscriber_producesCorrectHLHierarchy() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        // HL*1 = Information Source (payer), no parent, level 20, has children
        assertThat(edi).contains("HL*1**20*1~");
        // HL*2 = Information Receiver (provider), parent 1, level 21, has children
        assertThat(edi).contains("HL*2*1*21*1~");
        // HL*3 = Subscriber, parent 2, level 22, no children
        assertThat(edi).contains("HL*3*2*22*0~");
    }

    @Test
    void generate_singleSubscriber_producesPayerNM1() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("NM1*PR*2*BLUE CROSS BLUE SHIELD*****PI*BCBS12345~");
    }

    @Test
    void generate_singleSubscriber_producesProviderNM1AndREF() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("NM1*1P*2*SUNSHINE HEALTH CLINIC*****XX*1234567890~");
        assertThat(edi).contains("REF*EI*591234567~");
    }

    @Test
    void generate_singleSubscriber_producesSubscriberNM1AndDMG() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("NM1*IL*1*SMITH*JOHN*****MI*MEM987654321~");
        assertThat(edi).contains("DMG*D8*19850715*M~");
    }

    @Test
    void generate_singleSubscriber_producesDTPAndEQ() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("DTP*291*D8*20260328~");
        assertThat(edi).contains("EQ*30~");
    }

    @Test
    void generate_singleSubscriber_producesTrailers() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("SE*");
        assertThat(edi).contains("GE*1*00001~");
        assertThat(edi).contains("IEA*1*000000001~");
    }

    @Test
    void generate_twoSubscribersSamePayer_producesTwoSubscriberHLs() {
        var sub1 = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM001"), "20260328");
        var sub2 = new EligibilitySubscriber(makeSubscriber("DOE", "JANE", "MEM002"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BCBS", "BCBS12345", List.of(sub1, sub2))));

        String edi = generator.generate(inquiry);

        // One payer HL, one provider HL, two subscriber HLs
        assertThat(edi).contains("HL*1**20*1~");
        assertThat(edi).contains("HL*2*1*21*1~");
        assertThat(edi).contains("HL*3*2*22*0~");
        assertThat(edi).contains("HL*4*2*22*0~");
        assertThat(countOccurrences(edi, "NM1*IL*")).isEqualTo(2);
        assertThat(countOccurrences(edi, "EQ*30~")).isEqualTo(2);
    }

    @Test
    void generate_twoPayerGroups_producesCorrectHLSequence() {
        var sub1 = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM001"), "20260328");
        var sub2 = new EligibilitySubscriber(makeSubscriber("DOE", "JANE", "MEM002"), "20260328");

        var group1 = makeSourceGroup("BCBS", "BCBS12345", List.of(sub1));
        var group2 = makeSourceGroup("AETNA", "AETNA001", List.of(sub2));
        var inquiry = buildInquiry(List.of(group1, group2));

        String edi = generator.generate(inquiry);

        // First payer group: HL 1 (payer) -> HL 2 (provider) -> HL 3 (subscriber)
        assertThat(edi).contains("HL*1**20*1~");
        assertThat(edi).contains("HL*2*1*21*1~");
        assertThat(edi).contains("HL*3*2*22*0~");
        // Second payer group: HL 4 (payer) -> HL 5 (provider) -> HL 6 (subscriber)
        assertThat(edi).contains("HL*4**20*1~");
        assertThat(edi).contains("HL*5*4*21*1~");
        assertThat(edi).contains("HL*6*5*22*0~");
        assertThat(countOccurrences(edi, "NM1*PR*")).isEqualTo(2);
        assertThat(countOccurrences(edi, "NM1*1P*")).isEqualTo(2);
    }

    @Test
    void generate_singleSubscriber_allRequiredSegmentsPresent() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BCBS", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("ISA*");
        assertThat(edi).contains("GS*");
        assertThat(edi).contains("ST*");
        assertThat(edi).contains("BHT*");
        assertThat(edi).contains("HL*");
        assertThat(edi).contains("NM1*PR*");
        assertThat(edi).contains("NM1*1P*");
        assertThat(edi).contains("REF*EI*");
        assertThat(edi).contains("NM1*IL*");
        assertThat(edi).contains("DMG*");
        assertThat(edi).contains("DTP*291*");
        assertThat(edi).contains("EQ*30~");
        assertThat(edi).contains("SE*");
        assertThat(edi).contains("GE*");
        assertThat(edi).contains("IEA*");
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.service.EDI270GeneratorTest" --info`
Expected: Compilation failure -- `EDI270Generator` class does not exist yet.

- [ ] **Step 3: Implement EDI270Generator**

Create `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Generator.java`:

```java
package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.EligibilitySubscriber;
import com.example.edi.insurancerequest.domain.loop.InformationReceiverGroup;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class EDI270Generator {

    public String generate(EDI270Inquiry inquiry) {
        try {
            var factory = EDIOutputFactory.newFactory();
            factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
            var baos = new ByteArrayOutputStream();
            var writer = factory.createEDIStreamWriter(baos);

            int segmentCount = 0;

            writer.startInterchange();

            writeISA(writer, inquiry.envelope());
            writeGS(writer, inquiry.functionalGroup());

            segmentCount += writeST(writer, inquiry.transactionHeader());
            segmentCount += writeBHT(writer, inquiry.transactionHeader());

            int hlCounter = 1;
            for (InformationSourceGroup sourceGroup : inquiry.informationSourceGroups()) {
                // HL - Information Source (Payer)
                int payerHL = hlCounter++;
                segmentCount += writeHL(writer, payerHL, 0, "20", "1");
                segmentCount += writePayerNM1(writer, sourceGroup);

                // HL - Information Receiver (Provider)
                InformationReceiverGroup receiver = sourceGroup.informationReceiver();
                int providerHL = hlCounter++;
                segmentCount += writeHL(writer, providerHL, payerHL, "21", "1");
                segmentCount += writeProviderNM1(writer, receiver);
                segmentCount += writeProviderREF(writer, receiver);

                // HL - Subscribers
                for (EligibilitySubscriber eligSub : receiver.subscribers()) {
                    int subscriberHL = hlCounter++;
                    segmentCount += writeHL(writer, subscriberHL, providerHL, "22", "0");
                    segmentCount += writeSubscriberNM1(writer, eligSub.subscriber());
                    segmentCount += writeDMG(writer, eligSub.subscriber());
                    segmentCount += writeDTP(writer, eligSub.eligibilityDate());
                    segmentCount += writeEQ(writer);
                }
            }

            segmentCount++;
            writeSE(writer, segmentCount, inquiry.transactionHeader().transactionSetControlNumber());
            writeGE(writer, inquiry.functionalGroup().controlNumber());
            writeIEA(writer, inquiry.envelope().controlNumber());

            writer.endInterchange();
            writer.close();

            return baos.toString(StandardCharsets.UTF_8);
        } catch (EDIStreamException e) {
            throw new RuntimeException("Failed to generate EDI 270", e);
        }
    }

    private void writeISA(EDIStreamWriter w, InterchangeEnvelope env) throws EDIStreamException {
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

    private void writeGS(EDIStreamWriter w, FunctionalGroup fg) throws EDIStreamException {
        w.writeStartSegment("GS");
        elem(w, "HS");
        elem(w, fg.senderId());
        elem(w, fg.receiverId());
        elem(w, fg.date());
        elem(w, fg.time());
        elem(w, fg.controlNumber());
        elem(w, "X");
        elem(w, "005010X279A1");
        w.writeEndSegment();
    }

    private int writeST(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("ST");
        elem(w, "270");
        elem(w, th.transactionSetControlNumber());
        elem(w, "005010X279A1");
        w.writeEndSegment();
        return 1;
    }

    private int writeBHT(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("BHT");
        elem(w, "0022");
        elem(w, "13");
        elem(w, th.referenceId());
        elem(w, th.creationDate());
        elem(w, th.creationTime());
        w.writeEndSegment();
        return 1;
    }

    private int writeHL(EDIStreamWriter w, int hlNumber, int parentHL, String levelCode, String childCode) throws EDIStreamException {
        w.writeStartSegment("HL");
        elem(w, String.valueOf(hlNumber));
        elem(w, parentHL > 0 ? String.valueOf(parentHL) : "");
        elem(w, levelCode);
        elem(w, childCode);
        w.writeEndSegment();
        return 1;
    }

    private int writePayerNM1(EDIStreamWriter w, InformationSourceGroup source) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "PR");
        elem(w, "2");
        elem(w, source.payerName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "PI");
        elem(w, source.payerId());
        w.writeEndSegment();
        return 1;
    }

    private int writeProviderNM1(EDIStreamWriter w, InformationReceiverGroup receiver) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "1P");
        elem(w, "2");
        elem(w, receiver.providerName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "XX");
        elem(w, receiver.providerNpi());
        w.writeEndSegment();
        return 1;
    }

    private int writeProviderREF(EDIStreamWriter w, InformationReceiverGroup receiver) throws EDIStreamException {
        w.writeStartSegment("REF");
        elem(w, "EI");
        elem(w, receiver.providerTaxId());
        w.writeEndSegment();
        return 1;
    }

    private int writeSubscriberNM1(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
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

    private int writeDMG(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("DMG");
        elem(w, "D8");
        elem(w, sub.dateOfBirth());
        elem(w, sub.genderCode());
        w.writeEndSegment();
        return 1;
    }

    private int writeDTP(EDIStreamWriter w, String eligibilityDate) throws EDIStreamException {
        w.writeStartSegment("DTP");
        elem(w, "291");
        elem(w, "D8");
        elem(w, eligibilityDate);
        w.writeEndSegment();
        return 1;
    }

    private int writeEQ(EDIStreamWriter w) throws EDIStreamException {
        w.writeStartSegment("EQ");
        elem(w, "30");
        w.writeEndSegment();
        return 1;
    }

    private void writeSE(EDIStreamWriter w, int segmentCount, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("SE");
        elem(w, String.valueOf(segmentCount));
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeGE(EDIStreamWriter w, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("GE");
        elem(w, "1");
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeIEA(EDIStreamWriter w, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("IEA");
        elem(w, "1");
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void elem(EDIStreamWriter w, String value) throws EDIStreamException {
        w.writeElement(value != null ? value : "");
    }

    private String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.service.EDI270GeneratorTest" --info`
Expected: All 12 tests PASS

- [ ] **Step 5: Commit**

```bash
git add insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Generator.java \
      insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270GeneratorTest.java
git commit -m "feat: add EDI270Generator with StAEDI and TDD tests"
```

---

### Task 5: Rewrite InsuranceRequestService, Controller, and DTO

**Files:**
- Modify: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/InsuranceRequestDTO.java`
- Modify: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/InsuranceRequestService.java`
- Modify: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/controller/InsuranceRequestController.java`
- Modify: `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/InsuranceRequestServiceTest.java`
- Modify: `insurance-request-app/src/test/java/com/example/edi/insurancerequest/controller/InsuranceRequestControllerTest.java`
- Delete: `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Service.java`
- Delete: `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270ServiceTest.java`

- [ ] **Step 1: Update InsuranceRequestDTO**

Replace `insurance-request-app/src/main/java/com/example/edi/insurancerequest/dto/InsuranceRequestDTO.java`:

```java
package com.example.edi.insurancerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InsuranceRequestDTO(
        @NotEmpty List<@NotBlank String> patientIds
) {
}
```

- [ ] **Step 2: Write failing service test**

Replace `insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/InsuranceRequestServiceTest.java`:

```java
package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import com.example.edi.common.repository.PatientInsuranceRepository;
import com.example.edi.common.repository.PatientRepository;
import com.example.edi.common.repository.PayerRepository;
import com.example.edi.common.repository.PracticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceRequestServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientInsuranceRepository patientInsuranceRepository;
    @Mock private PayerRepository payerRepository;
    @Mock private PracticeRepository practiceRepository;
    @Mock private EDI270Mapper edi270Mapper;
    @Mock private EDI270Generator edi270Generator;

    private InterchangeProperties props;
    private InsuranceRequestService service;

    @BeforeEach
    void setUp() {
        props = new InterchangeProperties("ZZ", "SENDER001", "ZZ", "RECEIVER01", "0", "T");
        service = new InsuranceRequestService(
                patientRepository, patientInsuranceRepository, payerRepository,
                practiceRepository, edi270Mapper, edi270Generator, props);
    }

    @Test
    void generateEligibilityInquiry_singlePatient_callsMapperAndGenerator() {
        Patient patient = new Patient();
        patient.setId("P001");

        PatientInsurance insurance = new PatientInsurance();
        insurance.setPayerId("payer1");

        Payer payer = new Payer();
        payer.setPayerId("BCBS12345");

        Practice practice = new Practice();

        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(patientInsuranceRepository.findByPatientIdAndTerminationDateIsNull("P001"))
                .thenReturn(Optional.of(insurance));
        when(payerRepository.findById("payer1")).thenReturn(Optional.of(payer));
        when(practiceRepository.findAll()).thenReturn(List.of(practice));
        when(edi270Mapper.map(any(), any(), any())).thenReturn(null);
        when(edi270Generator.generate(any())).thenReturn("EDI_CONTENT");

        String result = service.generateEligibilityInquiry(List.of("P001"));

        assertThat(result).isEqualTo("EDI_CONTENT");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EligibilityBundle>> bundlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(edi270Mapper).map(eq(practice), bundlesCaptor.capture(), eq(props));
        assertThat(bundlesCaptor.getValue()).hasSize(1);
        assertThat(bundlesCaptor.getValue().getFirst().patient()).isSameAs(patient);
    }

    @Test
    void generateEligibilityInquiry_patientNotFound_throwsException() {
        when(practiceRepository.findAll()).thenReturn(List.of(new Practice()));
        when(patientRepository.findById("P999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateEligibilityInquiry(List.of("P999")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void generateEligibilityInquiry_noActiveInsurance_throwsException() {
        Patient patient = new Patient();
        patient.setId("P001");

        when(practiceRepository.findAll()).thenReturn(List.of(new Practice()));
        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(patientInsuranceRepository.findByPatientIdAndTerminationDateIsNull("P001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateEligibilityInquiry(List.of("P001")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Active insurance not found");
    }

    @Test
    void generateEligibilityInquiry_noPractice_throwsException() {
        when(practiceRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateEligibilityInquiry(List.of("P001")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No practice found");
    }
}
```

- [ ] **Step 3: Run service tests to verify they fail**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.service.InsuranceRequestServiceTest" --info`
Expected: Compilation failure -- constructor signature does not match yet.

- [ ] **Step 4: Rewrite InsuranceRequestService**

Replace `insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/InsuranceRequestService.java`:

```java
package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import com.example.edi.common.repository.PatientInsuranceRepository;
import com.example.edi.common.repository.PatientRepository;
import com.example.edi.common.repository.PayerRepository;
import com.example.edi.common.repository.PracticeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InsuranceRequestService {

    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final PayerRepository payerRepository;
    private final PracticeRepository practiceRepository;
    private final EDI270Mapper edi270Mapper;
    private final EDI270Generator edi270Generator;
    private final InterchangeProperties interchangeProperties;

    public InsuranceRequestService(PatientRepository patientRepository,
                                   PatientInsuranceRepository patientInsuranceRepository,
                                   PayerRepository payerRepository,
                                   PracticeRepository practiceRepository,
                                   EDI270Mapper edi270Mapper,
                                   EDI270Generator edi270Generator,
                                   InterchangeProperties interchangeProperties) {
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.payerRepository = payerRepository;
        this.practiceRepository = practiceRepository;
        this.edi270Mapper = edi270Mapper;
        this.edi270Generator = edi270Generator;
        this.interchangeProperties = interchangeProperties;
    }

    public String generateEligibilityInquiry(List<String> patientIds) {
        Practice practice = practiceRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No practice found in the system"));

        List<EligibilityBundle> bundles = new ArrayList<>();

        for (String patientId : patientIds) {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

            PatientInsurance insurance = patientInsuranceRepository
                    .findByPatientIdAndTerminationDateIsNull(patientId)
                    .orElseThrow(() -> new RuntimeException(
                            "Active insurance not found for patient: " + patientId));

            Payer payer = payerRepository.findById(insurance.getPayerId())
                    .orElseThrow(() -> new RuntimeException("Payer not found: " + insurance.getPayerId()));

            bundles.add(new EligibilityBundle(patient, insurance, payer));
        }

        var inquiry = edi270Mapper.map(practice, bundles, interchangeProperties);
        return edi270Generator.generate(inquiry);
    }
}
```

- [ ] **Step 5: Run service tests to verify they pass**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.service.InsuranceRequestServiceTest" --info`
Expected: All 4 tests PASS

- [ ] **Step 6: Update controller**

Replace `insurance-request-app/src/main/java/com/example/edi/insurancerequest/controller/InsuranceRequestController.java`:

```java
package com.example.edi.insurancerequest.controller;

import com.example.edi.insurancerequest.dto.InsuranceRequestDTO;
import com.example.edi.insurancerequest.service.InsuranceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceRequestController {

    private final InsuranceRequestService insuranceRequestService;

    public InsuranceRequestController(InsuranceRequestService insuranceRequestService) {
        this.insuranceRequestService = insuranceRequestService;
    }

    @PostMapping("/eligibility-request")
    public ResponseEntity<byte[]> requestEligibility(@Valid @RequestBody InsuranceRequestDTO request) {
        String ediContent = insuranceRequestService.generateEligibilityInquiry(request.patientIds());
        byte[] bytes = ediContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=270_inquiry.edi")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
```

- [ ] **Step 7: Update controller tests**

Replace `insurance-request-app/src/test/java/com/example/edi/insurancerequest/controller/InsuranceRequestControllerTest.java`:

```java
package com.example.edi.insurancerequest.controller;

import com.example.edi.insurancerequest.service.InsuranceRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsuranceRequestController.class)
class InsuranceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuranceRequestService insuranceRequestService;

    @Test
    void requestEligibility_validRequest_returns200() throws Exception {
        when(insuranceRequestService.generateEligibilityInquiry(List.of("P001")))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": ["P001"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=270_inquiry.edi"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    void requestEligibility_multiplePatients_returns200() throws Exception {
        when(insuranceRequestService.generateEligibilityInquiry(List.of("P001", "P002")))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": ["P001", "P002"]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void requestEligibility_emptyList_returns400() throws Exception {
        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestEligibility_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestEligibility_blankPatientId_returns400() throws Exception {
        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": [""]}
                                """))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 8: Delete old EDI270Service and its test**

```bash
rm insurance-request-app/src/main/java/com/example/edi/insurancerequest/service/EDI270Service.java
rm insurance-request-app/src/test/java/com/example/edi/insurancerequest/service/EDI270ServiceTest.java
```

- [ ] **Step 9: Run all unit tests**

Run: `./gradlew :insurance-request-app:test --info`
Expected: All tests PASS

- [ ] **Step 10: Commit**

```bash
git add -A insurance-request-app/src/
git commit -m "feat: rewrite InsuranceRequestService, controller, and DTO for multi-patient 270 generation"
```

---

### Task 6: Add Integration Tests

**Files:**
- Create: `insurance-request-app/src/test/java/com/example/edi/insurancerequest/EligibilityInquiryIT.java`

- [ ] **Step 1: Write integration test**

Create `insurance-request-app/src/test/java/com/example/edi/insurancerequest/EligibilityInquiryIT.java`:

```java
package com.example.edi.insurancerequest;

import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EligibilityInquiryIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
        return rt;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Autowired private PracticeRepository practiceRepository;
    @Autowired private PayerRepository payerRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PatientInsuranceRepository patientInsuranceRepository;

    private String patient1Id;
    private String patient2Id;

    @BeforeEach
    void setUp() {
        patientInsuranceRepository.deleteAll();
        patientRepository.deleteAll();
        payerRepository.deleteAll();
        practiceRepository.deleteAll();

        // Practice
        Practice practice = new Practice();
        practice.setName("SUNSHINE HEALTH CLINIC");
        practice.setNpi("1234567890");
        practice.setTaxId("591234567");
        practice.setAddress("100 MEDICAL PLAZA DR");
        practice.setCity("ORLANDO");
        practice.setState("FL");
        practice.setZipCode("32801");
        practice.setContactPhone("5551234567");
        practiceRepository.save(practice);

        // Payer 1
        Payer payer1 = new Payer();
        payer1.setName("BLUE CROSS BLUE SHIELD");
        payer1.setPayerId("BCBS12345");
        payer1 = payerRepository.save(payer1);

        // Payer 2
        Payer payer2 = new Payer();
        payer2.setName("AETNA");
        payer2.setPayerId("AETNA001");
        payer2 = payerRepository.save(payer2);

        // Patient 1
        Patient patient1 = new Patient();
        patient1.setFirstName("JOHN");
        patient1.setLastName("SMITH");
        patient1.setGender("M");
        patient1.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient1.setAddress("456 OAK AVENUE");
        patient1.setCity("ORLANDO");
        patient1.setState("FL");
        patient1.setZipCode("32806");
        patient1 = patientRepository.save(patient1);
        patient1Id = patient1.getId();

        PatientInsurance ins1 = new PatientInsurance();
        ins1.setPatientId(patient1Id);
        ins1.setPayerId(payer1.getId());
        ins1.setMemberId("MEM987654321");
        ins1.setGroupNumber("GRP100234");
        ins1.setPolicyType("MC");
        ins1.setSubscriberRelationship("self");
        ins1.setEffectiveDate(LocalDate.of(2025, 1, 1));
        patientInsuranceRepository.save(ins1);

        // Patient 2 (different payer)
        Patient patient2 = new Patient();
        patient2.setFirstName("JANE");
        patient2.setLastName("DOE");
        patient2.setGender("F");
        patient2.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient2.setAddress("789 ELM ST");
        patient2.setCity("ORLANDO");
        patient2.setState("FL");
        patient2.setZipCode("32807");
        patient2 = patientRepository.save(patient2);
        patient2Id = patient2.getId();

        PatientInsurance ins2 = new PatientInsurance();
        ins2.setPatientId(patient2Id);
        ins2.setPayerId(payer2.getId());
        ins2.setMemberId("MEM111222333");
        ins2.setGroupNumber("GRP200345");
        ins2.setPolicyType("MC");
        ins2.setSubscriberRelationship("self");
        ins2.setEffectiveDate(LocalDate.of(2025, 1, 1));
        patientInsuranceRepository.save(ins2);
    }

    @Test
    void eligibilityRequest_singlePatient_returnsValidEDI270() {
        Map<String, Object> requestBody = Map.of("patientIds", List.of(patient1Id));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/insurance/eligibility-request"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("ISA");
        assertThat(body).contains("GS*HS");
        assertThat(body).contains("ST*270");
        assertThat(body).contains("005010X279A1");
        assertThat(body).contains("BHT*0022*13");
        assertThat(body).contains("NM1*PR*");
        assertThat(body).contains("NM1*1P*");
        assertThat(body).contains("NM1*IL*1*SMITH*JOHN");
        assertThat(body).contains("DMG*D8*19850715*M");
        assertThat(body).contains("DTP*291*D8");
        assertThat(body).contains("EQ*30");
        assertThat(body).contains("SE*");
        assertThat(body).contains("GE*");
        assertThat(body).contains("IEA*");
    }

    @Test
    void eligibilityRequest_twoPatientsDifferentPayers_returnsTwoPayerGroups() {
        Map<String, Object> requestBody = Map.of("patientIds", List.of(patient1Id, patient2Id));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/insurance/eligibility-request"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        // Two payer NM1 segments
        assertThat(countOccurrences(body, "NM1*PR*")).isEqualTo(2);
        // Two provider NM1 segments (one per payer group)
        assertThat(countOccurrences(body, "NM1*1P*")).isEqualTo(2);
        // Two subscriber NM1 segments
        assertThat(countOccurrences(body, "NM1*IL*")).isEqualTo(2);
        // Two EQ segments
        assertThat(countOccurrences(body, "EQ*30")).isEqualTo(2);
        // Both patient names present
        assertThat(body).contains("SMITH*JOHN");
        assertThat(body).contains("DOE*JANE");
    }

    @Test
    void eligibilityRequest_unknownPatient_returns500() {
        Map<String, Object> requestBody = Map.of("patientIds", List.of("NONEXISTENT"));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/insurance/eligibility-request"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void eligibilityRequest_emptyList_returns400() {
        Map<String, Object> requestBody = Map.of("patientIds", List.of());

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/insurance/eligibility-request"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
```

- [ ] **Step 2: Run integration tests**

Run: `./gradlew :insurance-request-app:test --tests "com.example.edi.insurancerequest.EligibilityInquiryIT" --info`
Expected: All 4 tests PASS (requires Docker for Testcontainers)

- [ ] **Step 3: Run all tests to confirm nothing is broken**

Run: `./gradlew :insurance-request-app:test --info`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add insurance-request-app/src/test/java/com/example/edi/insurancerequest/EligibilityInquiryIT.java
git commit -m "test: add integration tests for EDI 270 eligibility inquiry generation"
```

---

### Task 7: Verify End-to-End

- [ ] **Step 1: Run full project build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL for all modules

- [ ] **Step 2: Commit any remaining cleanup**

If any old test files (`EDI270ServiceTest.java`) or the old `EDI270Service.java` were not deleted in Task 5, delete them now and commit:

```bash
git status
# If there are remaining changes:
git add -A insurance-request-app/
git commit -m "chore: remove old EDI270Service and tests replaced by mapper/generator pattern"
```
