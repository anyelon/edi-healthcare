package com.example.edi.insuranceresponse;

import com.example.edi.common.repository.EligibilityResponseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EligibilityResponseIT {

    private static final String VALID_EDI_271 =
            "ISA*00*          *00*          *ZZ*BCBS12345      *ZZ*CLEARINGHOUSE01*260329*1200*^*00501*000000001*0*T*:~\n" +
            "GS*HB*BCBS12345*CLEARINGHOUSE01*20260329*1200*1*X*005010X279A1~\n" +
            "ST*271*0001*005010X279A1~\n" +
            "BHT*0022*11*12345*20260329*1200~\n" +
            "HL*1**20*1~\n" +
            "NM1*PR*2*BLUE CROSS BLUE SHIELD*****PI*BCBS12345~\n" +
            "HL*2*1*21*1~\n" +
            "NM1*IL*1*SMITH*JOHN****MI*MEM987654321~\n" +
            "DMG*D8*19850715*M~\n" +
            "DTP*346*D8*20250101~\n" +
            "DTP*347*D8*20251231~\n" +
            "EB*1**30**HM*GOLD PLAN~\n" +
            "MSG*PATIENT IS ACTIVE AND ELIGIBLE~\n" +
            "EB*C*IND*30***23*500~\n" +
            "EB*A*IND*30***7*30~\n" +
            "SE*15*0001~\n" +
            "GE*1*1~\n" +
            "IEA*1*000000001~\n";

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("edi.archive.path", () -> tempDir.toString());
    }

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = buildRestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EligibilityResponseRepository eligibilityResponseRepository;

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

    @BeforeEach
    void setUp() {
        eligibilityResponseRepository.deleteAll();
    }

    @Test
    void uploadValidFile_returnsCompletedResponse() throws Exception {
        Path tempFile = tempDir.resolve("valid_271.edi");
        Files.writeString(tempFile, VALID_EDI_271);

        ResponseEntity<String> response = postFile(tempFile.toFile());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(json.get("memberId").asText()).isEqualTo("MEM987654321");
        assertThat(json.get("eligibilityStatus").asText()).isEqualTo("ACTIVE");

        assertThat(eligibilityResponseRepository.count()).isEqualTo(1);
    }

    @Test
    void uploadMalformedFile_returnsErrorResponse() throws Exception {
        Path tempFile = tempDir.resolve("malformed_271.edi");
        Files.writeString(tempFile, "NOT AN EDI FILE");

        ResponseEntity<String> response = postFile(tempFile.toFile());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("status").asText()).isEqualTo("ERROR");
        assertThat(json.has("errorMessage")).isTrue();
        assertThat(json.get("errorMessage").asText()).isNotBlank();

        assertThat(eligibilityResponseRepository.count()).isEqualTo(1);
    }

    @Test
    void uploadValidFile_archivesFile() throws Exception {
        Path tempFile = tempDir.resolve("archive_test_271.edi");
        Files.writeString(tempFile, VALID_EDI_271);

        postFile(tempFile.toFile());

        File[] archiveFiles = tempDir.toFile().listFiles();
        assertThat(archiveFiles).isNotNull();
        assertThat(archiveFiles.length).isGreaterThan(0);
    }

    private ResponseEntity<String> postFile(File file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(
                url("/api/insurance/eligibility-response"),
                requestEntity,
                String.class
        );
    }
}
