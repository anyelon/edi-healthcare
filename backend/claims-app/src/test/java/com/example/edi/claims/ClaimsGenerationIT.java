package com.example.edi.claims;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ClaimsGenerationIT {

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

    @Autowired
    private PracticeRepository practiceRepository;
    @Autowired
    private ProviderRepository providerRepository;
    @Autowired
    private PayerRepository payerRepository;
    @Autowired
    private FacilityRepository facilityRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PatientInsuranceRepository patientInsuranceRepository;
    @Autowired
    private EncounterRepository encounterRepository;
    @Autowired
    private EncounterDiagnosisRepository encounterDiagnosisRepository;
    @Autowired
    private EncounterProcedureRepository encounterProcedureRepository;

    private String encounterId;
    private String encounterId2;
    private String practiceId;
    private String facilityId;

    @BeforeEach
    void setUp() {
        encounterProcedureRepository.deleteAll();
        encounterDiagnosisRepository.deleteAll();
        encounterRepository.deleteAll();
        patientInsuranceRepository.deleteAll();
        patientRepository.deleteAll();
        facilityRepository.deleteAll();
        payerRepository.deleteAll();
        providerRepository.deleteAll();
        practiceRepository.deleteAll();

        // Practice
        Practice practice = new Practice();
        practice.setName("SUNSHINE HEALTH CLINIC");
        practice.setNpi("1234567890");
        practice.setTaxId("591234567");
        practice.setTaxonomyCode("207Q00000X");
        practice.setAddress("100 MEDICAL PLAZA DR");
        practice.setCity("ORLANDO");
        practice.setState("FL");
        practice.setZipCode("32801");
        practice.setContactPhone("5551234567");
        practice = practiceRepository.save(practice);
        practiceId = practice.getId();

        // Provider
        Provider provider = new Provider();
        provider.setFirstName("Sarah");
        provider.setLastName("Johnson");
        provider.setNpi("9876543210");
        provider.setTaxonomyCode("207R00000X");
        provider.setPracticeId(practiceId);
        provider = providerRepository.save(provider);

        // Payer
        Payer payer = new Payer();
        payer.setName("BLUE CROSS BLUE SHIELD");
        payer.setPayerId("BCBS12345");
        payer.setAddress("PO BOX 105187");
        payer.setCity("ATLANTA");
        payer.setState("GA");
        payer.setZipCode("30348");
        payer = payerRepository.save(payer);

        // Facility
        Facility facility = new Facility();
        facility.setName("Main Office");
        facility.setPracticeId(practiceId);
        facility.setPlaceOfServiceCode("11");
        facility.setAddress("100 MEDICAL PLAZA DR");
        facility.setCity("ORLANDO");
        facility.setState("FL");
        facility.setZipCode("32801");
        facility = facilityRepository.save(facility);
        facilityId = facility.getId();

        // Patient
        Patient patient = new Patient();
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");
        patient.setGender("M");
        patient.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient.setAddress("456 OAK AVENUE");
        patient.setCity("ORLANDO");
        patient.setState("FL");
        patient.setZipCode("32806");
        patient = patientRepository.save(patient);

        // Insurance
        PatientInsurance insurance = new PatientInsurance();
        insurance.setPatientId(patient.getId());
        insurance.setPayerId(payer.getId());
        insurance.setSubscriberRelationship("self");
        insurance.setGroupNumber("GRP100234");
        insurance.setPolicyType("MC");
        insurance.setEffectiveDate(LocalDate.of(2025, 1, 1));
        insurance.setMemberId("MEM987654321");
        patientInsuranceRepository.save(insurance);

        // Encounter 1
        Encounter encounter = new Encounter();
        encounter.setPatientId(patient.getId());
        encounter.setProviderId(provider.getId());
        encounter.setPracticeId(practiceId);
        encounter.setFacilityId(facilityId);
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));
        encounter = encounterRepository.save(encounter);
        encounterId = encounter.getId();

        // Diagnosis for encounter 1
        EncounterDiagnosis diagnosis = new EncounterDiagnosis();
        diagnosis.setEncounterId(encounterId);
        diagnosis.setDiagnosisCode("J06.9");
        diagnosis.setRank(1);
        encounterDiagnosisRepository.save(diagnosis);

        // Procedure for encounter 1
        EncounterProcedure procedure = new EncounterProcedure();
        procedure.setEncounterId(encounterId);
        procedure.setLineNumber(1);
        procedure.setProcedureCode("99213");
        procedure.setChargeAmount(new BigDecimal("150.00"));
        procedure.setUnits(1);
        procedure.setUnitType("UN");
        procedure.setModifiers(List.of());
        procedure.setDiagnosisPointers(List.of(1));
        encounterProcedureRepository.save(procedure);

        // Encounter 2 (same patient)
        Encounter encounter2 = new Encounter();
        encounter2.setPatientId(patient.getId());
        encounter2.setProviderId(provider.getId());
        encounter2.setPracticeId(practiceId);
        encounter2.setFacilityId(facilityId);
        encounter2.setDateOfService(LocalDate.of(2026, 3, 16));
        encounter2 = encounterRepository.save(encounter2);
        encounterId2 = encounter2.getId();

        // Diagnosis for encounter 2
        EncounterDiagnosis diagnosis2 = new EncounterDiagnosis();
        diagnosis2.setEncounterId(encounterId2);
        diagnosis2.setDiagnosisCode("Z00.00");
        diagnosis2.setRank(1);
        encounterDiagnosisRepository.save(diagnosis2);

        // Procedure for encounter 2
        EncounterProcedure procedure2 = new EncounterProcedure();
        procedure2.setEncounterId(encounterId2);
        procedure2.setLineNumber(1);
        procedure2.setProcedureCode("99214");
        procedure2.setChargeAmount(new BigDecimal("200.00"));
        procedure2.setUnits(1);
        procedure2.setUnitType("UN");
        procedure2.setModifiers(List.of());
        procedure2.setDiagnosisPointers(List.of(1));
        encounterProcedureRepository.save(procedure2);
    }

    @Test
    void generateClaim_singleEncounter_returnsValidEDI837() {
        Map<String, Object> requestBody = Map.of("encounterIds", List.of(encounterId));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/claims/generate"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("ISA");
        assertThat(body).contains("GS*HC");
        assertThat(body).contains("ST*837");
        assertThat(body).contains("CLM");
        assertThat(body).contains("HI*ABK");
        assertThat(body).contains("SV1*HC");
        assertThat(body).contains("DTP*472");
        assertThat(body).contains("SE");
        assertThat(body).contains("GE");
        assertThat(body).contains("IEA");
    }

    @Test
    void generateClaim_multipleEncountersSamePatient_returnsMultipleCLMs() {
        Map<String, Object> requestBody = Map.of("encounterIds", List.of(encounterId, encounterId2));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/claims/generate"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        // Should have one subscriber HL loop and two CLM segments
        assertThat(body).contains("HL*2*1*22*0");
        assertThat(countOccurrences(body, "CLM*")).isEqualTo(2);
        assertThat(countOccurrences(body, "SBR*")).isEqualTo(1);
        assertThat(body).contains("SV1*HC:99213");
        assertThat(body).contains("SV1*HC:99214");
    }

    @Test
    void generateClaim_unknownEncounter_returns404() {
        Map<String, Object> requestBody = Map.of("encounterIds", List.of("NONEXISTENT"));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/claims/generate"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
