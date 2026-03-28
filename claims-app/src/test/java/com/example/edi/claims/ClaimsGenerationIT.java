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

        // Provider
        Provider provider = new Provider();
        provider.setFirstName("Sarah");
        provider.setLastName("Johnson");
        provider.setNpi("9876543210");
        provider.setTaxonomyCode("207R00000X");
        provider.setPracticeId(practice.getId());
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
        facility.setPracticeId(practice.getId());
        facility.setPlaceOfServiceCode("11");
        facility.setAddress("100 MEDICAL PLAZA DR");
        facility.setCity("ORLANDO");
        facility.setState("FL");
        facility.setZipCode("32801");
        facility = facilityRepository.save(facility);

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

        // Encounter
        Encounter encounter = new Encounter();
        encounter.setPatientId(patient.getId());
        encounter.setProviderId(provider.getId());
        encounter.setPracticeId(practice.getId());
        encounter.setFacilityId(facility.getId());
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));
        encounter = encounterRepository.save(encounter);
        encounterId = encounter.getId();

        // Diagnosis
        EncounterDiagnosis diagnosis = new EncounterDiagnosis();
        diagnosis.setEncounterId(encounterId);
        diagnosis.setDiagnosisCode("J06.9");
        diagnosis.setRank(1);
        encounterDiagnosisRepository.save(diagnosis);

        // Procedure
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
    }

    @Test
    void generateClaim_returnsValidEDI837() {
        Map<String, String> requestBody = Map.of("encounterId", encounterId);

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
    void generateClaim_unknownEncounter_returns500() {
        Map<String, String> requestBody = Map.of("encounterId", "NONEXISTENT");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/claims/generate"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
