# EDI 837P Claim Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Build a service that generates EDI 837P 5010 professional claims from patient encounter data in MongoDB.

**Architecture:** Two-layer design — MongoDB @Document entities for persistent data, transient Java record classes representing 837 loop structures. A mapper converts DB entities to loop records, custom business validation runs on the loop records, then StAEDI's EDIStreamWriter generates structurally valid EDI output. Shared loop records and StAEDI live in the common module for reuse by 270/271 apps.

**Tech Stack:** Spring Boot 4.0.4, Java 21, MongoDB, StAEDI 1.25.3, Testcontainers, JUnit 5, Gradle multi-module

**Spec:** `specs/002-claims-generator/design.md`

---

### Task 1: Add StAEDI and Testcontainers dependencies

**Files:**
- Modify: `common/build.gradle`
- Modify: `claims-app/build.gradle`

- [ ] **Step 1: Add StAEDI to common/build.gradle**

Add to the dependencies block in `common/build.gradle`:

```groovy
implementation 'io.xlate:staedi:1.25.3'
```

- [ ] **Step 2: Add Testcontainers to claims-app/build.gradle**

Add to the dependencies block in `claims-app/build.gradle`:

```groovy
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:mongodb'
testImplementation 'org.testcontainers:junit-jupiter'
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew :claims-app:compileTestJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add common/build.gradle claims-app/build.gradle
git commit -m "build: add StAEDI and Testcontainers dependencies"
```

---

### Task 2: Create new database entities in common module

**Files:**
- Create: `common/src/main/java/com/example/edi/common/document/Practice.java`
- Create: `common/src/main/java/com/example/edi/common/document/Provider.java`
- Create: `common/src/main/java/com/example/edi/common/document/Payer.java`
- Create: `common/src/main/java/com/example/edi/common/document/Facility.java`
- Create: `common/src/main/java/com/example/edi/common/document/PatientInsurance.java`
- Create: `common/src/main/java/com/example/edi/common/document/Encounter.java`
- Create: `common/src/main/java/com/example/edi/common/document/EncounterDiagnosis.java`
- Create: `common/src/main/java/com/example/edi/common/document/EncounterProcedure.java`
- Modify: `common/src/main/java/com/example/edi/common/document/Patient.java`

- [ ] **Step 1: Create Practice.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "practices")
public class Practice {

    @Id
    private String id;
    private String name;
    private String npi;
    private String taxId;
    private String taxonomyCode;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String contactPhone;

    public Practice() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNpi() { return npi; }
    public void setNpi(String npi) { this.npi = npi; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getTaxonomyCode() { return taxonomyCode; }
    public void setTaxonomyCode(String taxonomyCode) { this.taxonomyCode = taxonomyCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
```

- [ ] **Step 2: Create Provider.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "providers")
public class Provider {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String npi;
    private String taxonomyCode;
    private String practiceId;

    public Provider() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getNpi() { return npi; }
    public void setNpi(String npi) { this.npi = npi; }
    public String getTaxonomyCode() { return taxonomyCode; }
    public void setTaxonomyCode(String taxonomyCode) { this.taxonomyCode = taxonomyCode; }
    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }
}
```

- [ ] **Step 3: Create Payer.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payers")
public class Payer {

    @Id
    private String id;
    private String name;
    private String payerId;
    private String address;
    private String city;
    private String state;
    private String zipCode;

    public Payer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
}
```

- [ ] **Step 4: Create Facility.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "facilities")
public class Facility {

    @Id
    private String id;
    private String name;
    private String practiceId;
    private String placeOfServiceCode;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String npi;

    public Facility() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }
    public String getPlaceOfServiceCode() { return placeOfServiceCode; }
    public void setPlaceOfServiceCode(String placeOfServiceCode) { this.placeOfServiceCode = placeOfServiceCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNpi() { return npi; }
    public void setNpi(String npi) { this.npi = npi; }
}
```

- [ ] **Step 5: Create PatientInsurance.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "patient_insurances")
public class PatientInsurance {

    @Id
    private String id;
    private String patientId;
    private String payerId;
    private String memberId;
    private String groupNumber;
    private String policyType;
    private String subscriberRelationship;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;

    public PatientInsurance() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public String getSubscriberRelationship() { return subscriberRelationship; }
    public void setSubscriberRelationship(String subscriberRelationship) { this.subscriberRelationship = subscriberRelationship; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
}
```

- [ ] **Step 6: Create Encounter.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "encounters")
public class Encounter {

    @Id
    private String id;
    private String patientId;
    private String providerId;
    private String practiceId;
    private String facilityId;
    private LocalDate dateOfService;
    private String authorizationNumber;

    public Encounter() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }
    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }
    public LocalDate getDateOfService() { return dateOfService; }
    public void setDateOfService(LocalDate dateOfService) { this.dateOfService = dateOfService; }
    public String getAuthorizationNumber() { return authorizationNumber; }
    public void setAuthorizationNumber(String authorizationNumber) { this.authorizationNumber = authorizationNumber; }
}
```

- [ ] **Step 7: Create EncounterDiagnosis.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "encounter_diagnoses")
public class EncounterDiagnosis {

    @Id
    private String id;
    private String encounterId;
    private int rank;
    private String diagnosisCode;
    private String description;

    public EncounterDiagnosis() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(String diagnosisCode) { this.diagnosisCode = diagnosisCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

- [ ] **Step 8: Create EncounterProcedure.java**

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Document(collection = "encounter_procedures")
public class EncounterProcedure {

    @Id
    private String id;
    private String encounterId;
    private int lineNumber;
    private String procedureCode;
    private List<String> modifiers;
    private BigDecimal chargeAmount;
    private int units;
    private String unitType;
    private List<Integer> diagnosisPointers;

    public EncounterProcedure() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }
    public List<String> getModifiers() { return modifiers; }
    public void setModifiers(List<String> modifiers) { this.modifiers = modifiers; }
    public BigDecimal getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(BigDecimal chargeAmount) { this.chargeAmount = chargeAmount; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public List<Integer> getDiagnosisPointers() { return diagnosisPointers; }
    public void setDiagnosisPointers(List<Integer> diagnosisPointers) { this.diagnosisPointers = diagnosisPointers; }
}
```

- [ ] **Step 9: Modify Patient.java — remove insurance fields, add phone**

Remove fields: memberId, insurancePayerId, insurancePayerName, insuranceGroupNumber and their getters/setters. Add field phone with getter/setter. The result:

```java
package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "patients")
public class Patient {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;

    public Patient() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
```

- [ ] **Step 10: Verify common module compiles**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add common/src/main/java/com/example/edi/common/document/
git commit -m "feat: add new database entities for 837P claim generation"
```

---

### Task 3: Create repositories for new entities

**Files:**
- Create: `common/src/main/java/com/example/edi/common/repository/PracticeRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/ProviderRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/PayerRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/FacilityRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/PatientInsuranceRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/EncounterRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/EncounterDiagnosisRepository.java`
- Create: `common/src/main/java/com/example/edi/common/repository/EncounterProcedureRepository.java`

- [ ] **Step 1: Create all repository interfaces**

`PracticeRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.Practice;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PracticeRepository extends MongoRepository<Practice, String> {
}
```

`ProviderRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.Provider;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProviderRepository extends MongoRepository<Provider, String> {
}
```

`PayerRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.Payer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PayerRepository extends MongoRepository<Payer, String> {
}
```

`FacilityRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.Facility;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FacilityRepository extends MongoRepository<Facility, String> {
}
```

`PatientInsuranceRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.PatientInsurance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientInsuranceRepository extends MongoRepository<PatientInsurance, String> {
    Optional<PatientInsurance> findByPatientIdAndTerminationDateIsNull(String patientId);
}
```

`EncounterRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.Encounter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EncounterRepository extends MongoRepository<Encounter, String> {
}
```

`EncounterDiagnosisRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.EncounterDiagnosis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EncounterDiagnosisRepository extends MongoRepository<EncounterDiagnosis, String> {
    List<EncounterDiagnosis> findByEncounterIdOrderByRankAsc(String encounterId);
}
```

`EncounterProcedureRepository.java`:
```java
package com.example.edi.common.repository;

import com.example.edi.common.document.EncounterProcedure;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EncounterProcedureRepository extends MongoRepository<EncounterProcedure, String> {
    List<EncounterProcedure> findByEncounterIdOrderByLineNumberAsc(String encounterId);
}
```

- [ ] **Step 2: Verify common module compiles**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/com/example/edi/common/repository/
git commit -m "feat: add repositories for new database entities"
```

---

### Task 4: Create shared EDI loop records in common module

**Files:**
- Create: `common/src/main/java/com/example/edi/common/edi/loop/InterchangeEnvelope.java`
- Create: `common/src/main/java/com/example/edi/common/edi/loop/FunctionalGroup.java`
- Create: `common/src/main/java/com/example/edi/common/edi/loop/TransactionHeader.java`
- Create: `common/src/main/java/com/example/edi/common/edi/loop/Submitter.java`
- Create: `common/src/main/java/com/example/edi/common/edi/loop/Receiver.java`
- Create: `common/src/main/java/com/example/edi/common/edi/loop/BillingProviderLoop.java`
- Create: `common/src/main/java/com/example/edi/common/edi/loop/SubscriberLoop.java`

- [ ] **Step 1: Create all shared loop records**

`InterchangeEnvelope.java`:
```java
package com.example.edi.common.edi.loop;

public record InterchangeEnvelope(
    String senderIdQualifier,
    String senderId,
    String receiverIdQualifier,
    String receiverId,
    String date,
    String time,
    String controlNumber,
    String ackRequested,
    String usageIndicator
) {}
```

`FunctionalGroup.java`:
```java
package com.example.edi.common.edi.loop;

public record FunctionalGroup(
    String senderId,
    String receiverId,
    String date,
    String time,
    String controlNumber
) {}
```

`TransactionHeader.java`:
```java
package com.example.edi.common.edi.loop;

public record TransactionHeader(
    String transactionSetControlNumber,
    String referenceId,
    String creationDate,
    String creationTime
) {}
```

`Submitter.java`:
```java
package com.example.edi.common.edi.loop;

public record Submitter(
    String name,
    String identifier,
    String contactPhone
) {}
```

`Receiver.java`:
```java
package com.example.edi.common.edi.loop;

public record Receiver(
    String name,
    String identifier
) {}
```

`BillingProviderLoop.java`:
```java
package com.example.edi.common.edi.loop;

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

`SubscriberLoop.java`:
```java
package com.example.edi.common.edi.loop;

public record SubscriberLoop(
    String subscriberRelationship,
    String groupNumber,
    String policyType,
    String lastName,
    String firstName,
    String memberId,
    String address,
    String city,
    String state,
    String zipCode,
    String dateOfBirth,
    String genderCode,
    String payerName,
    String payerId
) {}
```

- [ ] **Step 2: Verify common module compiles**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/com/example/edi/common/edi/
git commit -m "feat: add shared EDI loop records to common module"
```

---

### Task 5: Create 837-specific loop records and InterchangeProperties

**Files:**
- Create: `claims-app/src/main/java/com/example/edi/claims/domain/loop/DiagnosisEntry.java`
- Create: `claims-app/src/main/java/com/example/edi/claims/domain/loop/ServiceLineLoop.java`
- Create: `claims-app/src/main/java/com/example/edi/claims/domain/loop/ClaimLoop.java`
- Create: `claims-app/src/main/java/com/example/edi/claims/domain/loop/EDI837Claim.java`
- Create: `claims-app/src/main/java/com/example/edi/claims/config/InterchangeProperties.java`
- Modify: `claims-app/src/main/resources/application.yml`
- Modify: `claims-app/src/main/java/com/example/edi/claims/config/MongoConfig.java`

- [ ] **Step 1: Create DiagnosisEntry.java**

```java
package com.example.edi.claims.domain.loop;

public record DiagnosisEntry(
    int rank,
    String diagnosisCode
) {}
```

- [ ] **Step 2: Create ServiceLineLoop.java**

```java
package com.example.edi.claims.domain.loop;

import java.math.BigDecimal;
import java.util.List;

public record ServiceLineLoop(
    int lineNumber,
    String procedureCode,
    List<String> modifiers,
    BigDecimal chargeAmount,
    int units,
    String unitType,
    List<Integer> diagnosisPointers,
    String dateOfService
) {}
```

- [ ] **Step 3: Create ClaimLoop.java**

```java
package com.example.edi.claims.domain.loop;

import java.math.BigDecimal;
import java.util.List;

public record ClaimLoop(
    String claimId,
    BigDecimal totalCharge,
    String placeOfServiceCode,
    List<DiagnosisEntry> diagnoses,
    List<ServiceLineLoop> serviceLines
) {}
```

- [ ] **Step 4: Create EDI837Claim.java**

```java
package com.example.edi.claims.domain.loop;

import com.example.edi.common.edi.loop.BillingProviderLoop;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.Receiver;
import com.example.edi.common.edi.loop.Submitter;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;

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

- [ ] **Step 5: Create InterchangeProperties.java**

```java
package com.example.edi.claims.config;

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

- [ ] **Step 6: Add edi.interchange config to application.yml**

Add this block to the end of `claims-app/src/main/resources/application.yml`:

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

- [ ] **Step 7: Enable ConfigurationProperties in MongoConfig**

Update `claims-app/src/main/java/com/example/edi/claims/config/MongoConfig.java`:

```java
package com.example.edi.claims.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.edi.common.repository")
@EnableConfigurationProperties(InterchangeProperties.class)
public class MongoConfig {
}
```

- [ ] **Step 8: Verify claims-app compiles**

Run: `./gradlew :claims-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add claims-app/src/main/java/com/example/edi/claims/domain/ claims-app/src/main/java/com/example/edi/claims/config/ claims-app/src/main/resources/application.yml
git commit -m "feat: add 837-specific loop records and interchange config"
```

---

### Task 6: TDD — EDI837Generator (test first, then implement)

**Files:**
- Create: `claims-app/src/test/java/com/example/edi/claims/service/EDI837GeneratorTest.java`
- Create: `claims-app/src/main/java/com/example/edi/claims/service/EDI837Generator.java`

- [ ] **Step 1: Write the failing test**

Create `claims-app/src/test/java/com/example/edi/claims/service/EDI837GeneratorTest.java`. This test builds an EDI837Claim by hand and verifies the generator (backed by StAEDI) produces correct EDI output. Test cases: ISA padding, GS version, ST/SE count, HI qualifiers, SV1 pointers, IEA match, full segment presence.

The test class constructs fixtures in setUp() and has 7 test methods covering each segment group. Tests validate the string output — they don't depend on StAEDI directly, so if the underlying writer changes, the contract stays the same.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837GeneratorTest" 2>&1 | tail -5`
Expected: FAIL — EDI837Generator class does not exist yet

- [ ] **Step 3: Implement EDI837Generator using StAEDI**

Create `claims-app/src/main/java/com/example/edi/claims/service/EDI837Generator.java` as a @Service with a single `generate(EDI837Claim)` method. Uses StAEDI's `EDIOutputFactory` and `EDIStreamWriter` to produce structurally valid X12 output:

```java
var factory = EDIOutputFactory.newFactory();
var baos = new ByteArrayOutputStream();
var writer = factory.createEDIStreamWriter(baos);

writer.startInterchange();
// ISA elements — StAEDI handles fixed-width padding automatically
writer.writeStartSegment("ISA");
writer.writeElement("00");
writer.writeElement("          ");
// ... remaining ISA elements
writer.writeEndSegment();

// GS, ST, BHT, NM1, etc. segments
writer.writeStartSegment("CLM");
writer.writeElement(claimId);
writer.writeElement(totalCharge);
// ...
writer.writeEndSegment();

// StAEDI tracks segment count for SE automatically
writer.endInterchange();
writer.close();
return baos.toString(StandardCharsets.UTF_8);
```

Key: StAEDI manages separators (`*`, `:`, `~`), ISA padding, and SE segment count. The generator focuses on segment order and mapping loop record fields to elements. See spec for segment order.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837GeneratorTest"`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add claims-app/src/test/java/com/example/edi/claims/service/EDI837GeneratorTest.java claims-app/src/main/java/com/example/edi/claims/service/EDI837Generator.java
git commit -m "feat: add EDI837Generator with TDD tests"
```

---

### Task 7: TDD — EDI837Mapper (test first, then implement)

**Files:**
- Create: `claims-app/src/test/java/com/example/edi/claims/service/EDI837MapperTest.java`
- Create: `claims-app/src/main/java/com/example/edi/claims/service/EDI837Mapper.java`

- [ ] **Step 1: Write the failing test**

Create `claims-app/src/test/java/com/example/edi/claims/service/EDI837MapperTest.java`. Builds DB entity fixtures in setUp() and tests the map() method. 7 test methods: billingProvider from Practice, subscriber from Patient+Insurance+Payer, dateOfBirth formatting, gender mapping, diagnosis ordering, totalCharge calculation, envelope from config. See spec for all assertions.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837MapperTest" 2>&1 | tail -5`
Expected: FAIL — EDI837Mapper class does not exist yet

- [ ] **Step 3: Implement EDI837Mapper**

Create `claims-app/src/main/java/com/example/edi/claims/service/EDI837Mapper.java` as a @Service with a `map(Practice, Provider, Patient, PatientInsurance, Payer, Encounter, List<EncounterDiagnosis>, List<EncounterProcedure>, Facility, InterchangeProperties)` method. Generates control numbers, formats dates, maps gender codes, calculates total charge, builds all loop records. See spec for mapping rules.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.EDI837MapperTest"`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add claims-app/src/test/java/com/example/edi/claims/service/EDI837MapperTest.java claims-app/src/main/java/com/example/edi/claims/service/EDI837Mapper.java
git commit -m "feat: add EDI837Mapper with TDD tests"
```

---

### Task 8: Rewrite ClaimsService, ClaimsController, and ClaimsRequest DTO

**Files:**
- Modify: `claims-app/src/main/java/com/example/edi/claims/dto/ClaimsRequest.java`
- Modify: `claims-app/src/main/java/com/example/edi/claims/service/ClaimsService.java`
- Modify: `claims-app/src/main/java/com/example/edi/claims/controller/ClaimsController.java`
- Modify: `claims-app/src/test/java/com/example/edi/claims/controller/ClaimsControllerTest.java`

- [ ] **Step 1: Update ClaimsRequest to accept encounterId**

Change ClaimsRequest to have a single `@NotBlank String encounterId` field instead of patientId and dateOfService.

- [ ] **Step 2: Rewrite ClaimsService**

Replace constructor to inject all new repositories, EDI837Mapper, EDI837Generator, and InterchangeProperties. Change `generateClaim` signature to accept `String encounterId`. Follow the 11-step lookup flow from the spec.

- [ ] **Step 3: Update ClaimsController**

Change the generate endpoint to call `claimsService.generateClaim(request.encounterId())`.

- [ ] **Step 4: Update ClaimsControllerTest**

Update test fixtures to use `{"encounterId": "ENC001"}` format. Update mock to match new `generateClaim(String)` signature. Remove the old patientId/dateOfService validation tests, add missing encounterId test.

- [ ] **Step 5: Verify all tests pass**

Run: `./gradlew :claims-app:test`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add claims-app/src/main/java/com/example/edi/claims/dto/ claims-app/src/main/java/com/example/edi/claims/service/ClaimsService.java claims-app/src/main/java/com/example/edi/claims/controller/ClaimsController.java claims-app/src/test/java/com/example/edi/claims/controller/ClaimsControllerTest.java
git commit -m "feat: rewrite ClaimsService and controller for encounter-based claim generation"
```

---

### Task 9: Create DevSeedController

**Files:**
- Create: `claims-app/src/main/java/com/example/edi/claims/controller/DevSeedController.java`

- [ ] **Step 1: Implement DevSeedController**

POST /api/dev/seed endpoint. Injects all repositories via constructor. Checks `practiceRepository.count() > 0` for idempotency. Creates sample data per spec: 1 Practice, 2 Providers, 1 Payer, 3 Facilities, 2 Patients with PatientInsurance, 2 Encounters with diagnoses and procedures. Returns JSON map of created IDs.

- [ ] **Step 2: Verify claims-app compiles**

Run: `./gradlew :claims-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add claims-app/src/main/java/com/example/edi/claims/controller/DevSeedController.java
git commit -m "feat: add dev seed endpoint for sample data"
```

---

### Task 10: Clean up old files and tests

**Files:**
- Remove: `common/src/main/java/com/example/edi/common/document/Company.java`
- Remove: `common/src/main/java/com/example/edi/common/document/Visit.java`
- Remove: `common/src/main/java/com/example/edi/common/document/PlaceOfService.java`
- Remove: `common/src/main/java/com/example/edi/common/repository/CompanyRepository.java`
- Remove: `common/src/main/java/com/example/edi/common/repository/VisitRepository.java`
- Remove: `common/src/main/java/com/example/edi/common/repository/PlaceOfServiceRepository.java`
- Remove: `claims-app/src/main/java/com/example/edi/claims/service/EDI837Service.java`
- Remove: `claims-app/src/test/java/com/example/edi/claims/service/EDI837ServiceTest.java`
- Remove: `claims-app/src/test/java/com/example/edi/claims/service/ClaimsServiceTest.java`

- [ ] **Step 1: Delete old entity, repository, service, and test files**

```bash
rm common/src/main/java/com/example/edi/common/document/Company.java
rm common/src/main/java/com/example/edi/common/document/Visit.java
rm common/src/main/java/com/example/edi/common/document/PlaceOfService.java
rm common/src/main/java/com/example/edi/common/repository/CompanyRepository.java
rm common/src/main/java/com/example/edi/common/repository/VisitRepository.java
rm common/src/main/java/com/example/edi/common/repository/PlaceOfServiceRepository.java
rm claims-app/src/main/java/com/example/edi/claims/service/EDI837Service.java
rm claims-app/src/test/java/com/example/edi/claims/service/EDI837ServiceTest.java
rm claims-app/src/test/java/com/example/edi/claims/service/ClaimsServiceTest.java
```

- [ ] **Step 2: Verify claims-app builds and all tests pass**

Run: `./gradlew :claims-app:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove old Company, Visit, PlaceOfService entities and EDI837Service"
```

---

### Task 11: Integration tests with Testcontainers

**Files:**
- Create: `claims-app/src/test/java/com/example/edi/claims/ClaimsGenerationIT.java`
- Create: `claims-app/src/test/java/com/example/edi/claims/DevSeedControllerIT.java`

- [ ] **Step 1: Write ClaimsGenerationIT**

@SpringBootTest with RANDOM_PORT + @Testcontainers. MongoDBContainer("mongo:7") with @DynamicPropertySource. Seeds test data in @BeforeEach via repositories. Tests: POST /api/claims/generate with valid encounterId returns 200 with EDI containing ISA, GS, ST, CLM, HI, SV1, DTP, SE, GE, IEA. Also tests unknown encounterId returns 500.

- [ ] **Step 2: Write DevSeedControllerIT**

Same Testcontainers setup. Tests: POST /api/dev/seed creates data (practiceRepository.count() == 1, encounterRepository.count() == 2). Second call returns "Data already seeded" and counts unchanged.

- [ ] **Step 3: Run all tests**

Run: `./gradlew :claims-app:test`
Expected: All unit and integration tests PASS

- [ ] **Step 4: Commit**

```bash
git add claims-app/src/test/java/com/example/edi/claims/ClaimsGenerationIT.java claims-app/src/test/java/com/example/edi/claims/DevSeedControllerIT.java
git commit -m "test: add integration tests with Testcontainers for claim generation and seed endpoint"
```

---

### Task 12: Final verification

- [ ] **Step 1: Run full build**

Run: `./gradlew :claims-app:clean :claims-app:build`
Expected: BUILD SUCCESSFUL with all tests passing

- [ ] **Step 2: Manual smoke test**

```bash
docker-compose up -d
./gradlew :claims-app:bootRun
```

In another terminal:
```bash
curl -s -X POST http://localhost:8080/api/dev/seed | jq .
curl -s -X POST http://localhost:8080/api/claims/generate \
  -H "Content-Type: application/json" \
  -d '{"encounterId":"<paste-encounter-id>"}' \
  -o claim.edi
cat claim.edi
```

Expected: Valid 837P EDI file with ISA, GS, ST, BHT, NM1, CLM, HI, SV1, DTP, SE, GE, IEA segments

- [ ] **Step 3: Commit any final fixes if needed**
