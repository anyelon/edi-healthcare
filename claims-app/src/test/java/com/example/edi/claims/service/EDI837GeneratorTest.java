package com.example.edi.claims.service;

import com.example.edi.claims.domain.loop.ClaimLoop;
import com.example.edi.claims.domain.loop.DiagnosisEntry;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.ServiceLineLoop;
import com.example.edi.common.edi.loop.BillingProviderLoop;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.Receiver;
import com.example.edi.common.edi.loop.Submitter;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDI837GeneratorTest {

    private EDI837Generator generator;
    private EDI837Claim claim;

    @BeforeEach
    void setUp() {
        generator = new EDI837Generator();

        var envelope = new InterchangeEnvelope(
            "ZZ", "SENDER001", "ZZ", "RECEIVER01",
            "260315", "1430", "000000001", "0", "T"
        );

        var functionalGroup = new FunctionalGroup(
            "SENDER001", "RECEIVER01", "20260315", "1430", "1"
        );

        var transactionHeader = new TransactionHeader(
            "0001", "000000001", "20260315", "1430"
        );

        var submitter = new Submitter(
            "SUNSHINE HEALTH CLINIC", "1234567890", "5551234567"
        );

        var receiver = new Receiver(
            "BLUE CROSS BLUE SHIELD", "BCBS12345"
        );

        var billingProvider = new BillingProviderLoop(
            "SUNSHINE HEALTH CLINIC", "1234567890", "591234567",
            "100 MEDICAL PLAZA DR", "ORLANDO", "FL", "32801"
        );

        var subscriber = new SubscriberLoop(
            "P", "GRP100234", "MC",
            "SMITH", "JOHN", "MEM987654321",
            "456 OAK AVENUE", "ORLANDO", "FL", "32806",
            "19850715", "M",
            "BLUE CROSS BLUE SHIELD", "BCBS12345"
        );

        var diagnoses = List.of(
            new DiagnosisEntry(1, "J06.9"),
            new DiagnosisEntry(2, "R05.9")
        );

        var serviceLines = List.of(
            new ServiceLineLoop(1, "99213", List.of(), new BigDecimal("150.00"), 1, "UN", List.of(1, 2), "20260315"),
            new ServiceLineLoop(2, "87880", List.of(), new BigDecimal("100.00"), 1, "UN", List.of(1), "20260315")
        );

        var claimLoop = new ClaimLoop(
            "MEM987654321", new BigDecimal("250.00"), "11", diagnoses, serviceLines
        );

        claim = new EDI837Claim(
            envelope, functionalGroup, transactionHeader,
            submitter, receiver, billingProvider, subscriber, claimLoop
        );
    }

    @Test
    void generate_producesValidISASegment() {
        String edi = generator.generate(claim);

        assertThat(edi).startsWith("ISA*");
        assertThat(edi).contains("*00501*");
        assertThat(edi).contains("*SENDER001      *");
    }

    @Test
    void generate_producesCorrectGSVersion() {
        String edi = generator.generate(claim);

        assertThat(edi).contains("005010X222A1");
    }

    @Test
    void generate_producesCorrectSTBHT() {
        String edi = generator.generate(claim);

        assertThat(edi).contains("ST*837*0001*005010X222A1~");
        assertThat(edi).contains("BHT*0019*00*000000001*20260315*1430*CH~");
    }

    @Test
    void generate_producesHIWithCorrectQualifiers() {
        String edi = generator.generate(claim);

        assertThat(edi).contains("ABK:J06.9");
        assertThat(edi).contains("ABF:R05.9");
    }

    @Test
    void generate_producesSV1WithDiagnosisPointers() {
        String edi = generator.generate(claim);

        assertThat(edi).contains("SV1*HC:99213*150.00*UN*1***1:2~");
        assertThat(edi).contains("SV1*HC:87880*100.00*UN*1***1~");
    }

    @Test
    void generate_IEAControlNumberMatchesISA() {
        String edi = generator.generate(claim);

        assertThat(edi).contains("IEA*1*000000001~");
    }

    @Test
    void generate_fullOutputContainsAllRequiredSegments() {
        String edi = generator.generate(claim);

        assertThat(edi).contains("ISA*");
        assertThat(edi).contains("GS*");
        assertThat(edi).contains("ST*");
        assertThat(edi).contains("BHT*");
        assertThat(edi).contains("NM1*");
        assertThat(edi).contains("PER*");
        assertThat(edi).contains("N3*");
        assertThat(edi).contains("N4*");
        assertThat(edi).contains("REF*");
        assertThat(edi).contains("SBR*");
        assertThat(edi).contains("CLM*");
        assertThat(edi).contains("HI*");
        assertThat(edi).contains("SV1*");
        assertThat(edi).contains("DTP*");
        assertThat(edi).contains("SE*");
        assertThat(edi).contains("GE*");
        assertThat(edi).contains("IEA*");
    }
}
