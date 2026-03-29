package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.EligibilitySubscriber;
import com.example.edi.insurancerequest.domain.loop.InformationReceiverGroup;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDI270GeneratorTest {

    private EDI270Generator generator;
    private InterchangeEnvelope envelope;
    private FunctionalGroup functionalGroup;
    private TransactionHeader transactionHeader;

    @BeforeEach
    void setUp() {
        generator = new EDI270Generator();

        envelope = new InterchangeEnvelope(
            "ZZ", "SENDER001", "ZZ", "RECEIVER01",
            "260315", "1430", "000000001", "0", "T"
        );

        functionalGroup = new FunctionalGroup(
            "SENDER001", "RECEIVER01", "20260315", "1430", "00001"
        );

        transactionHeader = new TransactionHeader(
            "00001", "270", "20260315", "1430"
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

    private InformationSourceGroup makeSourceGroup(String payerName, String payerId,
                                                    List<EligibilitySubscriber> subscribers) {
        var receiver = new InformationReceiverGroup(
            "SUNSHINE HEALTH CLINIC", "1234567890", "591234567", subscribers
        );
        return new InformationSourceGroup(payerName, payerId, receiver);
    }

    private EDI270Inquiry buildInquiry(List<InformationSourceGroup> groups) {
        return new EDI270Inquiry(envelope, functionalGroup, transactionHeader, groups);
    }

    @Test
    void generate_singleSubscriber_producesValidISASegment() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).startsWith("ISA*");
        assertThat(edi).contains("*00501*");
        assertThat(edi).contains("*SENDER001      *");
    }

    @Test
    void generate_singleSubscriber_producesCorrectGSVersion() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("GS*HS*");
        assertThat(edi).contains("005010X279A1");
    }

    @Test
    void generate_singleSubscriber_producesCorrectSTAndBHT() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("ST*270*00001*005010X279A1~");
        assertThat(edi).contains("BHT*0022*13*270*20260315*1430~");
    }

    @Test
    void generate_singleSubscriber_producesCorrectHLHierarchy() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("HL*1**20*1~");
        assertThat(edi).contains("HL*2*1*21*1~");
        assertThat(edi).contains("HL*3*2*22*0~");
    }

    @Test
    void generate_singleSubscriber_producesPayerNM1() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("NM1*PR*2*BLUE CROSS BLUE SHIELD*****PI*BCBS12345~");
    }

    @Test
    void generate_singleSubscriber_producesProviderNM1AndREF() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("NM1*1P*2*SUNSHINE HEALTH CLINIC*****XX*1234567890~");
        assertThat(edi).contains("REF*EI*591234567~");
    }

    @Test
    void generate_singleSubscriber_producesSubscriberNM1AndDMG() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("NM1*IL*1*SMITH*JOHN*****MI*MEM987654321~");
        assertThat(edi).contains("DMG*D8*19850715*M~");
    }

    @Test
    void generate_singleSubscriber_producesDTPAndEQ() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("DTP*291*D8*20260328~");
        assertThat(edi).contains("EQ*30~");
    }

    @Test
    void generate_singleSubscriber_producesTrailers() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BLUE CROSS BLUE SHIELD", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("SE*");
        assertThat(edi).contains("GE*1*00001~");
        assertThat(edi).contains("IEA*1*000000001~");
    }

    @Test
    void generate_twoSubscribersSamePayer_producesTwoSubscriberHLs() {
        var sub1 = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM001"), "20260328");
        var sub2 = new EligibilitySubscriber(makeSubscriber("DOE", "JANE", "MEM002"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BCBS", "BCBS12345", List.of(sub1, sub2))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("HL*1**20*1~");
        assertThat(edi).contains("HL*2*1*21*1~");
        assertThat(edi).contains("HL*3*2*22*0~");
        assertThat(edi).contains("HL*4*2*22*0~");
        assertThat(countOccurrences(edi, "NM1*IL*")).isEqualTo(2);
        assertThat(countOccurrences(edi, "EQ*30~")).isEqualTo(2);
    }

    @Test
    void generate_twoPayerGroups_producesCorrectHLSequence() {
        var sub1 = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM001"), "20260328");
        var sub2 = new EligibilitySubscriber(makeSubscriber("DOE", "JANE", "MEM002"), "20260328");

        var group1 = makeSourceGroup("BCBS", "BCBS12345", List.of(sub1));
        var group2 = makeSourceGroup("AETNA", "AETNA001", List.of(sub2));
        var inquiry = buildInquiry(List.of(group1, group2));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("HL*1**20*1~");
        assertThat(edi).contains("HL*2*1*21*1~");
        assertThat(edi).contains("HL*3*2*22*0~");
        assertThat(edi).contains("HL*4**20*1~");
        assertThat(edi).contains("HL*5*4*21*1~");
        assertThat(edi).contains("HL*6*5*22*0~");
        assertThat(countOccurrences(edi, "NM1*PR*")).isEqualTo(2);
        assertThat(countOccurrences(edi, "NM1*1P*")).isEqualTo(2);
    }

    @Test
    void generate_singleSubscriber_allRequiredSegmentsPresent() {
        var sub = new EligibilitySubscriber(makeSubscriber("SMITH", "JOHN", "MEM987654321"), "20260328");
        var inquiry = buildInquiry(List.of(makeSourceGroup("BCBS", "BCBS12345", List.of(sub))));

        String edi = generator.generate(inquiry);

        assertThat(edi).contains("ISA*");
        assertThat(edi).contains("GS*");
        assertThat(edi).contains("ST*");
        assertThat(edi).contains("BHT*");
        assertThat(edi).contains("HL*");
        assertThat(edi).contains("NM1*PR*");
        assertThat(edi).contains("NM1*1P*");
        assertThat(edi).contains("REF*EI*");
        assertThat(edi).contains("NM1*IL*");
        assertThat(edi).contains("DMG*");
        assertThat(edi).contains("DTP*291*");
        assertThat(edi).contains("EQ*30~");
        assertThat(edi).contains("SE*");
        assertThat(edi).contains("GE*");
        assertThat(edi).contains("IEA*");
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
