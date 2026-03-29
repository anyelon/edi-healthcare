# Data GET Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/patients` and `GET /api/encounters` endpoints to claims-app so the frontend can display data for selection instead of requiring manual ID entry.

**Architecture:** Two new controller/service pairs in claims-app following existing patterns (constructor injection, record DTOs, `@WebMvcTest` tests). EncounterService batch-loads related entities to build enriched responses with resolved names.

**Tech Stack:** Spring Boot 4.0.4, Java 21, MongoDB, JUnit 5, MockMvc

---

## File Structure

**New files in `backend/claims-app/src/main/java/com/example/edi/claims/`:**

| File | Responsibility |
|------|---------------|
| `dto/PatientResponse.java` | Patient API response record |
| `dto/EncounterResponse.java` | Enriched encounter API response record |
| `dto/DiagnosisResponse.java` | Diagnosis nested in EncounterResponse |
| `dto/ProcedureResponse.java` | Procedure nested in EncounterResponse |
| `service/PatientService.java` | Fetches patients, maps to DTOs |
| `service/EncounterService.java` | Fetches encounters, batch-loads related entities, maps to enriched DTOs |
| `controller/PatientController.java` | `GET /api/patients` |
| `controller/EncounterController.java` | `GET /api/encounters` |

**New test files in `backend/claims-app/src/test/java/com/example/edi/claims/`:**

| File | Responsibility |
|------|---------------|
| `controller/PatientControllerTest.java` | WebMvcTest for PatientController |
| `controller/EncounterControllerTest.java` | WebMvcTest for EncounterController |
| `service/PatientServiceTest.java` | Unit test for PatientService |
| `service/EncounterServiceTest.java` | Unit test for EncounterService |

---

### Task 1: Patient DTO and Service

**Files:**
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/dto/PatientResponse.java`
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/service/PatientService.java`
- Test: `backend/claims-app/src/test/java/com/example/edi/claims/service/PatientServiceTest.java`

- [ ] **Step 1: Write the PatientResponse record**

```java
package com.example.edi.claims.dto;

import java.time.LocalDate;

public record PatientResponse(
        String id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String city,
        String state,
        String zipCode,
        String phone
) {}
```

- [ ] **Step 2: Write the failing PatientService test**

```java
package com.example.edi.claims.service;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.common.document.Patient;
import com.example.edi.common.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    @Test
    void getAllPatients_returnsMappedResponses() {
        Patient patient = new Patient();
        patient.setId("P1");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");
        patient.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient.setGender("M");
        patient.setAddress("456 OAK AVE");
        patient.setCity("ORLANDO");
        patient.setState("FL");
        patient.setZipCode("32806");
        patient.setPhone("5553334444");

        when(patientRepository.findAll()).thenReturn(List.of(patient));

        List<PatientResponse> result = patientService.getAllPatients();

        assertThat(result).hasSize(1);
        PatientResponse response = result.getFirst();
        assertThat(response.id()).isEqualTo("P1");
        assertThat(response.firstName()).isEqualTo("JOHN");
        assertThat(response.lastName()).isEqualTo("SMITH");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1985, 7, 15));
        assertThat(response.gender()).isEqualTo("M");
    }

    @Test
    void getAllPatients_emptyDatabase_returnsEmptyList() {
        when(patientRepository.findAll()).thenReturn(List.of());

        List<PatientResponse> result = patientService.getAllPatients();

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.PatientServiceTest" --rerun`
Expected: Compilation failure — `PatientService` class does not exist yet.

- [ ] **Step 4: Write PatientService implementation**

```java
package com.example.edi.claims.service;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.common.document.Patient;
import com.example.edi.common.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getAddress(),
                patient.getCity(),
                patient.getState(),
                patient.getZipCode(),
                patient.getPhone()
        );
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.PatientServiceTest" --rerun`
Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

```bash
git add backend/claims-app/src/main/java/com/example/edi/claims/dto/PatientResponse.java \
       backend/claims-app/src/main/java/com/example/edi/claims/service/PatientService.java \
       backend/claims-app/src/test/java/com/example/edi/claims/service/PatientServiceTest.java
git commit -m "feat: add PatientService and PatientResponse DTO"
```

---

### Task 2: Patient Controller

**Files:**
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/controller/PatientController.java`
- Test: `backend/claims-app/src/test/java/com/example/edi/claims/controller/PatientControllerTest.java`

- [ ] **Step 1: Write the failing PatientController test**

```java
package com.example.edi.claims.controller;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.claims.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    void getPatients_returns200WithPatientList() throws Exception {
        PatientResponse patient = new PatientResponse(
                "P1", "JOHN", "SMITH", LocalDate.of(1985, 7, 15), "M",
                "456 OAK AVE", "ORLANDO", "FL", "32806", "5553334444");

        when(patientService.getAllPatients()).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("P1"))
                .andExpect(jsonPath("$[0].firstName").value("JOHN"))
                .andExpect(jsonPath("$[0].lastName").value("SMITH"))
                .andExpect(jsonPath("$[0].dateOfBirth").value("1985-07-15"))
                .andExpect(jsonPath("$[0].gender").value("M"));
    }

    @Test
    void getPatients_emptyDatabase_returns200WithEmptyList() throws Exception {
        when(patientService.getAllPatients()).thenReturn(List.of());

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.controller.PatientControllerTest" --rerun`
Expected: Compilation failure — `PatientController` class does not exist yet.

- [ ] **Step 3: Write PatientController implementation**

```java
package com.example.edi.claims.controller;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.claims.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientResponse> getPatients() {
        return patientService.getAllPatients();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.controller.PatientControllerTest" --rerun`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add backend/claims-app/src/main/java/com/example/edi/claims/controller/PatientController.java \
       backend/claims-app/src/test/java/com/example/edi/claims/controller/PatientControllerTest.java
git commit -m "feat: add GET /api/patients endpoint"
```

---

### Task 3: Encounter DTOs

**Files:**
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/dto/DiagnosisResponse.java`
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/dto/ProcedureResponse.java`
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java`

- [ ] **Step 1: Write DiagnosisResponse record**

```java
package com.example.edi.claims.dto;

public record DiagnosisResponse(
        String diagnosisCode,
        int rank
) {}
```

- [ ] **Step 2: Write ProcedureResponse record**

```java
package com.example.edi.claims.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProcedureResponse(
        String procedureCode,
        List<String> modifiers,
        BigDecimal chargeAmount,
        int units
) {}
```

- [ ] **Step 3: Write EncounterResponse record**

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

- [ ] **Step 4: Commit**

```bash
git add backend/claims-app/src/main/java/com/example/edi/claims/dto/DiagnosisResponse.java \
       backend/claims-app/src/main/java/com/example/edi/claims/dto/ProcedureResponse.java \
       backend/claims-app/src/main/java/com/example/edi/claims/dto/EncounterResponse.java
git commit -m "feat: add encounter response DTOs"
```

---

### Task 4: Encounter Service

**Files:**
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java`
- Test: `backend/claims-app/src/test/java/com/example/edi/claims/service/EncounterServiceTest.java`

- [ ] **Step 1: Write the failing EncounterService test**

```java
package com.example.edi.claims.service;

import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private EncounterDiagnosisRepository encounterDiagnosisRepository;

    @Mock
    private EncounterProcedureRepository encounterProcedureRepository;

    @InjectMocks
    private EncounterService encounterService;

    @Test
    void getAllEncounters_returnsEnrichedResponses() {
        Encounter encounter = new Encounter();
        encounter.setId("ENC1");
        encounter.setPatientId("P1");
        encounter.setProviderId("PROV1");
        encounter.setFacilityId("FAC1");
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));
        encounter.setAuthorizationNumber("AUTH001");

        Patient patient = new Patient();
        patient.setId("P1");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");

        Provider provider = new Provider();
        provider.setId("PROV1");
        provider.setFirstName("Sarah");
        provider.setLastName("Johnson");

        Facility facility = new Facility();
        facility.setId("FAC1");
        facility.setName("Main Office");

        EncounterDiagnosis diagnosis = new EncounterDiagnosis();
        diagnosis.setEncounterId("ENC1");
        diagnosis.setDiagnosisCode("J06.9");
        diagnosis.setRank(1);

        EncounterProcedure procedure = new EncounterProcedure();
        procedure.setEncounterId("ENC1");
        procedure.setProcedureCode("99213");
        procedure.setModifiers(List.of());
        procedure.setChargeAmount(new BigDecimal("150.00"));
        procedure.setUnits(1);

        when(encounterRepository.findAll()).thenReturn(List.of(encounter));
        when(patientRepository.findAllById(List.of("P1"))).thenReturn(List.of(patient));
        when(providerRepository.findAllById(List.of("PROV1"))).thenReturn(List.of(provider));
        when(facilityRepository.findAllById(List.of("FAC1"))).thenReturn(List.of(facility));
        when(encounterDiagnosisRepository.findAll()).thenReturn(List.of(diagnosis));
        when(encounterProcedureRepository.findAll()).thenReturn(List.of(procedure));

        List<EncounterResponse> result = encounterService.getAllEncounters();

        assertThat(result).hasSize(1);
        EncounterResponse response = result.getFirst();
        assertThat(response.id()).isEqualTo("ENC1");
        assertThat(response.patientName()).isEqualTo("JOHN SMITH");
        assertThat(response.providerName()).isEqualTo("Sarah Johnson");
        assertThat(response.facilityName()).isEqualTo("Main Office");
        assertThat(response.dateOfService()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(response.authorizationNumber()).isEqualTo("AUTH001");
        assertThat(response.diagnoses()).hasSize(1);
        assertThat(response.diagnoses().getFirst().diagnosisCode()).isEqualTo("J06.9");
        assertThat(response.procedures()).hasSize(1);
        assertThat(response.procedures().getFirst().procedureCode()).isEqualTo("99213");
    }

    @Test
    void getAllEncounters_emptyDatabase_returnsEmptyList() {
        when(encounterRepository.findAll()).thenReturn(List.of());

        List<EncounterResponse> result = encounterService.getAllEncounters();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllEncounters_missingRelatedEntity_usesUnknownFallback() {
        Encounter encounter = new Encounter();
        encounter.setId("ENC1");
        encounter.setPatientId("P1");
        encounter.setProviderId("PROV_MISSING");
        encounter.setFacilityId("FAC_MISSING");
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));

        Patient patient = new Patient();
        patient.setId("P1");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");

        when(encounterRepository.findAll()).thenReturn(List.of(encounter));
        when(patientRepository.findAllById(List.of("P1"))).thenReturn(List.of(patient));
        when(providerRepository.findAllById(List.of("PROV_MISSING"))).thenReturn(List.of());
        when(facilityRepository.findAllById(List.of("FAC_MISSING"))).thenReturn(List.of());
        when(encounterDiagnosisRepository.findAll()).thenReturn(List.of());
        when(encounterProcedureRepository.findAll()).thenReturn(List.of());

        List<EncounterResponse> result = encounterService.getAllEncounters();

        assertThat(result).hasSize(1);
        EncounterResponse response = result.getFirst();
        assertThat(response.providerName()).isEqualTo("Unknown");
        assertThat(response.facilityName()).isEqualTo("Unknown");
        assertThat(response.patientName()).isEqualTo("JOHN SMITH");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.EncounterServiceTest" --rerun`
Expected: Compilation failure — `EncounterService` class does not exist yet.

- [ ] **Step 3: Write EncounterService implementation**

```java
package com.example.edi.claims.service;

import com.example.edi.claims.dto.DiagnosisResponse;
import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.claims.dto.ProcedureResponse;
import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final FacilityRepository facilityRepository;
    private final EncounterDiagnosisRepository encounterDiagnosisRepository;
    private final EncounterProcedureRepository encounterProcedureRepository;

    public EncounterService(EncounterRepository encounterRepository,
                            PatientRepository patientRepository,
                            ProviderRepository providerRepository,
                            FacilityRepository facilityRepository,
                            EncounterDiagnosisRepository encounterDiagnosisRepository,
                            EncounterProcedureRepository encounterProcedureRepository) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.facilityRepository = facilityRepository;
        this.encounterDiagnosisRepository = encounterDiagnosisRepository;
        this.encounterProcedureRepository = encounterProcedureRepository;
    }

    public List<EncounterResponse> getAllEncounters() {
        List<Encounter> encounters = encounterRepository.findAll();
        if (encounters.isEmpty()) {
            return List.of();
        }

        List<String> patientIds = encounters.stream().map(Encounter::getPatientId).distinct().toList();
        List<String> providerIds = encounters.stream().map(Encounter::getProviderId).distinct().toList();
        List<String> facilityIds = encounters.stream().map(Encounter::getFacilityId).distinct().toList();

        Map<String, Patient> patientsById = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));
        Map<String, Provider> providersById = providerRepository.findAllById(providerIds).stream()
                .collect(Collectors.toMap(Provider::getId, p -> p));
        Map<String, Facility> facilitiesById = facilityRepository.findAllById(facilityIds).stream()
                .collect(Collectors.toMap(Facility::getId, f -> f));

        Map<String, List<EncounterDiagnosis>> diagnosesByEncounterId = encounterDiagnosisRepository.findAll().stream()
                .collect(Collectors.groupingBy(EncounterDiagnosis::getEncounterId));
        Map<String, List<EncounterProcedure>> proceduresByEncounterId = encounterProcedureRepository.findAll().stream()
                .collect(Collectors.groupingBy(EncounterProcedure::getEncounterId));

        return encounters.stream().map(encounter -> {
            Patient patient = patientsById.get(encounter.getPatientId());
            Provider provider = providersById.get(encounter.getProviderId());
            Facility facility = facilitiesById.get(encounter.getFacilityId());

            String patientName = patient != null
                    ? patient.getFirstName() + " " + patient.getLastName()
                    : "Unknown";
            String providerName = provider != null
                    ? provider.getFirstName() + " " + provider.getLastName()
                    : "Unknown";
            String facilityName = facility != null ? facility.getName() : "Unknown";

            List<DiagnosisResponse> diagnoses = diagnosesByEncounterId
                    .getOrDefault(encounter.getId(), List.of()).stream()
                    .map(d -> new DiagnosisResponse(d.getDiagnosisCode(), d.getRank()))
                    .toList();

            List<ProcedureResponse> procedures = proceduresByEncounterId
                    .getOrDefault(encounter.getId(), List.of()).stream()
                    .map(p -> new ProcedureResponse(
                            p.getProcedureCode(),
                            p.getModifiers(),
                            p.getChargeAmount(),
                            p.getUnits()))
                    .toList();

            return new EncounterResponse(
                    encounter.getId(),
                    encounter.getPatientId(),
                    patientName,
                    encounter.getProviderId(),
                    providerName,
                    encounter.getFacilityId(),
                    facilityName,
                    encounter.getDateOfService(),
                    encounter.getAuthorizationNumber(),
                    diagnoses,
                    procedures
            );
        }).toList();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.service.EncounterServiceTest" --rerun`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add backend/claims-app/src/main/java/com/example/edi/claims/service/EncounterService.java \
       backend/claims-app/src/test/java/com/example/edi/claims/service/EncounterServiceTest.java
git commit -m "feat: add EncounterService with batch-loaded enrichment"
```

---

### Task 5: Encounter Controller

**Files:**
- Create: `backend/claims-app/src/main/java/com/example/edi/claims/controller/EncounterController.java`
- Test: `backend/claims-app/src/test/java/com/example/edi/claims/controller/EncounterControllerTest.java`

- [ ] **Step 1: Write the failing EncounterController test**

```java
package com.example.edi.claims.controller;

import com.example.edi.claims.dto.DiagnosisResponse;
import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.claims.dto.ProcedureResponse;
import com.example.edi.claims.service.EncounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EncounterController.class)
class EncounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EncounterService encounterService;

    @Test
    void getEncounters_returns200WithEnrichedList() throws Exception {
        EncounterResponse encounter = new EncounterResponse(
                "ENC1", "P1", "JOHN SMITH", "PROV1", "Sarah Johnson",
                "FAC1", "Main Office", LocalDate.of(2026, 3, 15), "AUTH001",
                List.of(new DiagnosisResponse("J06.9", 1)),
                List.of(new ProcedureResponse("99213", List.of(), new BigDecimal("150.00"), 1))
        );

        when(encounterService.getAllEncounters()).thenReturn(List.of(encounter));

        mockMvc.perform(get("/api/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ENC1"))
                .andExpect(jsonPath("$[0].patientName").value("JOHN SMITH"))
                .andExpect(jsonPath("$[0].providerName").value("Sarah Johnson"))
                .andExpect(jsonPath("$[0].facilityName").value("Main Office"))
                .andExpect(jsonPath("$[0].dateOfService").value("2026-03-15"))
                .andExpect(jsonPath("$[0].diagnoses[0].diagnosisCode").value("J06.9"))
                .andExpect(jsonPath("$[0].diagnoses[0].rank").value(1))
                .andExpect(jsonPath("$[0].procedures[0].procedureCode").value("99213"))
                .andExpect(jsonPath("$[0].procedures[0].chargeAmount").value(150.00));
    }

    @Test
    void getEncounters_emptyDatabase_returns200WithEmptyList() throws Exception {
        when(encounterService.getAllEncounters()).thenReturn(List.of());

        mockMvc.perform(get("/api/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.controller.EncounterControllerTest" --rerun`
Expected: Compilation failure — `EncounterController` class does not exist yet.

- [ ] **Step 3: Write EncounterController implementation**

```java
package com.example.edi.claims.controller;

import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.claims.service.EncounterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @GetMapping
    public List<EncounterResponse> getEncounters() {
        return encounterService.getAllEncounters();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :claims-app:test --tests "com.example.edi.claims.controller.EncounterControllerTest" --rerun`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add backend/claims-app/src/main/java/com/example/edi/claims/controller/EncounterController.java \
       backend/claims-app/src/test/java/com/example/edi/claims/controller/EncounterControllerTest.java
git commit -m "feat: add GET /api/encounters endpoint"
```

---

### Task 6: Run Full Test Suite

- [ ] **Step 1: Run all claims-app tests**

Run: `./gradlew :claims-app:test --rerun`
Expected: All tests pass, including existing ClaimsControllerTest and any other tests.

- [ ] **Step 2: Run full project tests**

Run: `./gradlew test --rerun`
Expected: All tests pass across all modules (common, claims-app, insurance-request-app, insurance-response-app).

- [ ] **Step 3: Verify build succeeds**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL
