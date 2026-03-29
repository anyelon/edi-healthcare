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
        assertThat(countOccurrences(body, "NM1*PR*")).isEqualTo(2);
        assertThat(countOccurrences(body, "NM1*1P*")).isEqualTo(2);
        assertThat(countOccurrences(body, "NM1*IL*")).isEqualTo(2);
        assertThat(countOccurrences(body, "EQ*30")).isEqualTo(2);
        assertThat(body).contains("SMITH*JOHN");
        assertThat(body).contains("DOE*JANE");
    }

    @Test
    void eligibilityRequest_unknownPatient_returns404() {
        Map<String, Object> requestBody = Map.of("patientIds", List.of("NONEXISTENT"));

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/insurance/eligibility-request"),
                requestBody,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
