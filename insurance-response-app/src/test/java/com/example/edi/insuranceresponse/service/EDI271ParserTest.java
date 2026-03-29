package com.example.edi.insuranceresponse.service;

import com.example.edi.insuranceresponse.domain.loop.BenefitInfo;
import com.example.edi.insuranceresponse.domain.loop.EDI271Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EDI271ParserTest {

    private EDI271Parser parser;

    @TempDir
    Path tempDir;

    private Path ediFile;

    private static final String VALID_271 = String.join("\n",
            "ISA*00*          *00*          *ZZ*BCBS12345      *ZZ*CLEARINGHOUSE01*260329*1200*^*00501*000000001*0*T*:~",
            "GS*HB*BCBS12345*CLEARINGHOUSE01*20260329*1200*1*X*005010X279A1~",
            "ST*271*0001*005010X279A1~",
            "BHT*0022*11*12345*20260329*1200~",
            "HL*1**20*1~",
            "NM1*PR*2*BLUE CROSS BLUE SHIELD*****PI*BCBS12345~",
            "HL*2*1*21*1~",
            "NM1*IL*1*SMITH*JOHN****MI*MEM987654321~",
            "DMG*D8*19850715*M~",
            "DTP*346*D8*20250101~",
            "DTP*347*D8*20251231~",
            "EB*1**30**HM*GOLD PLAN~",
            "MSG*PATIENT IS ACTIVE AND ELIGIBLE~",
            "EB*C*IND*30***23*500~",
            "EB*A*IND*30***7*30~",
            "SE*15*0001~",
            "GE*1*1~",
            "IEA*1*000000001~"
    );

    @BeforeEach
    void setUp() throws IOException {
        parser = new EDI271Parser();
        ediFile = tempDir.resolve("test271.edi");
        Files.writeString(ediFile, VALID_271);
    }

    @Test
    void parse_extractsSubscriberInfo() {
        EDI271Response response = parser.parse(ediFile);

        assertThat(response.subscriberInfo().firstName()).isEqualTo("JOHN");
        assertThat(response.subscriberInfo().lastName()).isEqualTo("SMITH");
        assertThat(response.subscriberInfo().memberId()).isEqualTo("MEM987654321");
    }

    @Test
    void parse_extractsPayerInfo() {
        EDI271Response response = parser.parse(ediFile);

        assertThat(response.payerInfo().name()).isEqualTo("BLUE CROSS BLUE SHIELD");
        assertThat(response.payerInfo().payerId()).isEqualTo("BCBS12345");
    }

    @Test
    void parse_mapsActiveEligibility() {
        EDI271Response response = parser.parse(ediFile);

        assertThat(response.subscriberInfo().eligibilityStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void parse_mapsInactiveEligibility() throws IOException {
        String inactive271 = VALID_271.replace("EB*1**30**HM*GOLD PLAN~", "EB*6**30**HM*GOLD PLAN~");
        Path inactiveFile = tempDir.resolve("inactive271.edi");
        Files.writeString(inactiveFile, inactive271);

        EDI271Response response = parser.parse(inactiveFile);

        assertThat(response.subscriberInfo().eligibilityStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void parse_extractsBenefitDetails() {
        EDI271Response response = parser.parse(ediFile);

        assertThat(response.benefits()).hasSize(3);

        BenefitInfo deductible = response.benefits().stream()
                .filter(b -> "DEDUCTIBLE".equals(b.benefitType()))
                .findFirst().orElseThrow();
        assertThat(deductible.coverageLevel()).isEqualTo("IND");
        assertThat(deductible.amount()).isEqualByComparingTo(new BigDecimal("500"));

        BenefitInfo coinsurance = response.benefits().stream()
                .filter(b -> "CO-INSURANCE".equals(b.benefitType()))
                .findFirst().orElseThrow();
        assertThat(coinsurance.coverageLevel()).isEqualTo("IND");
        assertThat(coinsurance.amount()).isEqualByComparingTo(new BigDecimal("30"));
    }

    @Test
    void parse_extractsCoverageDates() {
        EDI271Response response = parser.parse(ediFile);

        BenefitInfo firstBenefit = response.benefits().getFirst();
        assertThat(firstBenefit.coverageStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(firstBenefit.coverageEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void parse_malformedFile_throwsException() throws IOException {
        Path malformedFile = tempDir.resolve("malformed.edi");
        Files.writeString(malformedFile, "NOT EDI DATA");

        assertThatThrownBy(() -> parser.parse(malformedFile))
                .isInstanceOf(RuntimeException.class);
    }
}
