package com.example.edi.claims.service;

import com.example.edi.claims.domain.loop.ClaimLoop;
import com.example.edi.claims.domain.loop.DiagnosisEntry;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.ServiceLineLoop;
import com.example.edi.claims.domain.loop.SubscriberGroup;
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
    private InterchangeEnvelope envelope;
    private FunctionalGroup functionalGroup;
    private TransactionHeader transactionHeader;
    private Submitter submitter;
    private Receiver receiver;
    private BillingProviderLoop billingProvider;

    @BeforeEach
    void setUp() {
        generator = new EDI837Generator();

        envelope = new InterchangeEnvelope(
            "ZZ", "SENDER001", "ZZ", "RECEIVER01",
            "260315", "1430", "000000001", "0", "T"
        );

        functionalGroup = new FunctionalGroup(
            "SENDER001", "RECEIVER01", "20260315", "1430", "1"
        );

        transactionHeader = new TransactionHeader(
            "0001", "000000001", "20260315", "1430"
        );

        submitter = new Submitter(
            "SUNSHINE HEALTH CLINIC", "1234567890", "5551234567"
        );

        receiver = new Receiver(
            "BLUE CROSS BLUE SHIELD", "BCBS12345"
        );

        billingProvider = new BillingProviderLoop(
            "SUNSHINE HEALTH CLINIC", "1234567890", "591234567",
            "100 MEDICAL PLAZA DR", "ORLANDO", "FL", "32801"
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

    private ClaimLoop makeClaimLoop(String claimId, BigDecimal totalCharge) {
        var diagnoses = List.of(
            new DiagnosisEntry(1, "J06.9"),
            new DiagnosisEntry(2, "R05.9")
        );
        var serviceLines = List.of(
            new ServiceLineLoop(1, "99213", List.of(), new BigDecimal("150.00"), 1, "UN", List.of(1, 2), "20260315"),
            new ServiceLineLoop(2, "87880", List.of(), new BigDecimal("100.00"), 1, "UN", List.of(1), "20260315")
        );
        return new ClaimLoop(claimId, totalCharge, "11", diagnoses, serviceLines);
    }

    private EDI837Claim buildClaim(List<SubscriberGroup> groups) {
        return new EDI837Claim(
            envelope, functionalGroup, transactionHeader,
            submitter, receiver, billingProvider, groups
        );
    }

    @Test
    void generate_singleSubscriberSingleClaim_producesValidISASegment() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

        String edi = generator.generate(claim);

        assertThat(edi).startsWith("ISA*");
        assertThat(edi).contains("*00501*");
        assertThat(edi).contains("*SENDER001      *");
    }

    @Test
    void generate_singleSubscriberSingleClaim_producesCorrectGSVersion() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

        String edi = generator.generate(claim);

        assertThat(edi).contains("005010X222A1");
    }

    @Test
    void generate_singleSubscriberSingleClaim_producesCorrectSTBHT() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

        String edi = generator.generate(claim);

        assertThat(edi).contains("ST*837*0001*005010X222A1~");
        assertThat(edi).contains("BHT*0019*00*000000001*20260315*1430*CH~");
    }

    @Test
    void generate_singleSubscriberSingleClaim_producesHIWithCorrectQualifiers() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

        String edi = generator.generate(claim);

        assertThat(edi).contains("ABK:J06.9");
        assertThat(edi).contains("ABF:R05.9");
    }

    @Test
    void generate_singleSubscriberSingleClaim_producesSV1WithDiagnosisPointers() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

        String edi = generator.generate(claim);

        assertThat(edi).contains("SV1*HC:99213*150.00*UN*1***1:2~");
        assertThat(edi).contains("SV1*HC:87880*100.00*UN*1***1~");
    }

    @Test
    void generate_singleSubscriberSingleClaim_IEAControlNumberMatchesISA() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

        String edi = generator.generate(claim);

        assertThat(edi).contains("IEA*1*000000001~");
    }

    @Test
    void generate_singleSubscriberSingleClaim_fullOutputContainsAllRequiredSegments() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claimLoop = makeClaimLoop("MEM987654321", new BigDecimal("250.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claimLoop))));

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

    @Test
    void generate_singleSubscriberTwoClaims_producesTwoCLMSegments() {
        var subscriber = makeSubscriber("SMITH", "JOHN", "MEM987654321");
        var claim1 = makeClaimLoop("CLM001", new BigDecimal("150.00"));
        var claim2 = makeClaimLoop("CLM002", new BigDecimal("100.00"));
        var claim = buildClaim(List.of(new SubscriberGroup(subscriber, List.of(claim1, claim2))));

        String edi = generator.generate(claim);

        // Should have exactly one HL*2 (subscriber) and two CLM segments
        assertThat(edi).contains("HL*2*1*22*0~");
        assertThat(edi).contains("CLM*CLM001*");
        assertThat(edi).contains("CLM*CLM002*");
        // Only one SBR segment (single subscriber)
        assertThat(countOccurrences(edi, "SBR*")).isEqualTo(1);
        // Two CLM segments
        assertThat(countOccurrences(edi, "CLM*")).isEqualTo(2);
    }

    @Test
    void generate_twoSubscribersOneClaimEach_producesTwoHLSubscriberLoops() {
        var subscriberA = makeSubscriber("SMITH", "JOHN", "MEMA");
        var subscriberB = makeSubscriber("DOE", "JANE", "MEMB");
        var claimA = makeClaimLoop("CLMA", new BigDecimal("150.00"));
        var claimB = makeClaimLoop("CLMB", new BigDecimal("200.00"));
        var claim = buildClaim(List.of(
            new SubscriberGroup(subscriberA, List.of(claimA)),
            new SubscriberGroup(subscriberB, List.of(claimB))
        ));

        String edi = generator.generate(claim);

        // Two subscriber HL segments: HL*2 and HL*3
        assertThat(edi).contains("HL*2*1*22*0~");
        assertThat(edi).contains("HL*3*1*22*0~");
        // Two SBR segments
        assertThat(countOccurrences(edi, "SBR*")).isEqualTo(2);
        // Two CLM segments
        assertThat(countOccurrences(edi, "CLM*")).isEqualTo(2);
        assertThat(edi).contains("CLM*CLMA*");
        assertThat(edi).contains("CLM*CLMB*");
        // Subscriber names present
        assertThat(edi).contains("NM1*IL*1*SMITH*JOHN");
        assertThat(edi).contains("NM1*IL*1*DOE*JANE");
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
