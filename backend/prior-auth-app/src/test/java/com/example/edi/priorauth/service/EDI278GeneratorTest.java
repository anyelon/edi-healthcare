package com.example.edi.priorauth.service;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.ServiceReviewInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EDI278GeneratorTest {

    private final EDI278Generator generator = new EDI278Generator();

    @Test
    void generate_containsISAandIEA() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("ISA*"));
        assertTrue(edi.contains("IEA*"));
    }

    @Test
    void generate_containsGSWithHIFunctionalId() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("GS*HI*"));
        assertTrue(edi.contains("GE*"));
    }

    @Test
    void generate_containsSTandSE() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("ST*278*"));
        assertTrue(edi.contains("SE*"));
    }

    @Test
    void generate_containsBHTForRequest() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("BHT*0007*"));
    }

    @Test
    void generate_containsHLHierarchy() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("HL*1**20*1"));
        assertTrue(edi.contains("HL*2*1*21*1"));
        assertTrue(edi.contains("HL*3*2*22*1"));
        assertTrue(edi.contains("HL*4*3*EV*0"));
    }

    @Test
    void generate_containsTRNWithEncounterId() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("TRN*1*ENC001"));
    }

    @Test
    void generate_containsUMSegment() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("UM*"));
    }

    @Test
    void generate_containsSV1WithProcedureCode() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("SV1*HC:99213"));
    }

    @Test
    void generate_containsHIWithClinicalReason() {
        EDI278Request request = createTestRequest();
        String edi = generator.generate(request);
        assertTrue(edi.contains("HI*"));
        assertTrue(edi.contains("Chronic pain"));
    }

    private EDI278Request createTestRequest() {
        InterchangeEnvelope envelope = new InterchangeEnvelope(
                "ZZ", "SENDER_ID", "ZZ", "RECEIVER_ID",
                "260415", "1200", "000000001", "0", "T");
        FunctionalGroup fg = new FunctionalGroup(
                "SENDER_ID", "RECEIVER_ID", "20260415", "1200", "00001");
        TransactionHeader th = new TransactionHeader(
                "00001", "278", "20260415", "1200");
        SubscriberLoop subscriber = new SubscriberLoop(
                "P", "GRP001", "HMO", "Doe", "John", "MEM001",
                "123 Main St", "Springfield", "IL", "62701",
                "19900515", "M", "Test Payer", "PAY001");
        ServiceReviewInfo service = new ServiceReviewInfo(
                "99213", "Chronic pain management", "20260415");
        EDI278Request.SubscriberReviewGroup group =
                new EDI278Request.SubscriberReviewGroup(
                        subscriber, "ENC001", List.of(service));
        return new EDI278Request(envelope, fg, th,
                "Test Payer", "PAY001", "Test Practice",
                "1234567890", "123456789", List.of(group));
    }
}
