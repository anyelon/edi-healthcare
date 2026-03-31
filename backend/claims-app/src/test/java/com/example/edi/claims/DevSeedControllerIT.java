package com.example.edi.claims;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DevSeedControllerIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
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
    }

    @Test
    void seed_createsData() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/dev/seed"),
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("practiceId");
        assertThat(body).contains("encounterIds");
        assertThat(practiceRepository.count()).isEqualTo(1);
        assertThat(encounterRepository.count()).isEqualTo(2);
    }

    @Test
    void seed_isIdempotent() {
        restTemplate.postForEntity(url("/api/dev/seed"), null, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/dev/seed"),
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("Data already seeded");
        assertThat(practiceRepository.count()).isEqualTo(1);
    }
}
