package com.example.edi.insuranceresponse.service;

import com.example.edi.common.document.BenefitDetail;
import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.insuranceresponse.domain.loop.BenefitInfo;
import com.example.edi.insuranceresponse.domain.loop.EDI271Response;
import com.example.edi.insuranceresponse.domain.loop.PayerInfo;
import com.example.edi.insuranceresponse.domain.loop.SubscriberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EDI271MapperTest {

    private EDI271Mapper mapper;
    private EDI271Response response;

    private static final String FILE_PATH = "/archive/test.edi";
    private static final LocalDateTime RECEIVED_AT = LocalDateTime.of(2026, 3, 28, 10, 0, 0);

    @BeforeEach
    void setUp() {
        mapper = new EDI271Mapper();

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                "ZZ", "SENDERID", "ZZ", "RECEIVERID",
                "20260328", "1000", "000000001", "0", "P"
        );
        FunctionalGroup functionalGroup = new FunctionalGroup(
                "SENDERID", "RECEIVERID", "20260328", "1000", "1"
        );
        TransactionHeader transactionHeader = new TransactionHeader(
                "0001", "TRACE001", "20260328", "1000"
        );
        SubscriberInfo subscriberInfo = new SubscriberInfo(
                "John", "Doe", "MEM001", "GRP100", "ACTIVE"
        );
        PayerInfo payerInfo = new PayerInfo("AETNA", "PAYER01");

        BenefitInfo benefit1 = new BenefitInfo(
                "MEDICAL", "INDIVIDUAL", "30", true,
                new BigDecimal("1500.00"), new BigDecimal("80.00"),
                "CALENDAR_YEAR", "In-network deductible",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
        BenefitInfo benefit2 = new BenefitInfo(
                "PHARMACY", "FAMILY", "88", false,
                new BigDecimal("3000.00"), null,
                "CALENDAR_YEAR", null, null, null
        );

        response = new EDI271Response(
                envelope, functionalGroup, transactionHeader,
                subscriberInfo, payerInfo, List.of(benefit1, benefit2)
        );
    }

    @Test
    void map_setsCompletedStatus() {
        EligibilityResponse result = mapper.map(response, FILE_PATH, RECEIVED_AT);

        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void map_subscriberFieldsFromSubscriberInfo() {
        EligibilityResponse result = mapper.map(response, FILE_PATH, RECEIVED_AT);

        assertEquals("John", result.getSubscriberFirstName());
        assertEquals("Doe", result.getSubscriberLastName());
        assertEquals("MEM001", result.getMemberId());
        assertEquals("ACTIVE", result.getEligibilityStatus());
    }

    @Test
    void map_payerFieldsFromPayerInfo() {
        EligibilityResponse result = mapper.map(response, FILE_PATH, RECEIVED_AT);

        assertEquals("AETNA", result.getPayerName());
        assertEquals("PAYER01", result.getPayerId());
    }

    @Test
    void map_benefitsListMapped() {
        EligibilityResponse result = mapper.map(response, FILE_PATH, RECEIVED_AT);

        assertNotNull(result.getBenefits());
        assertEquals(2, result.getBenefits().size());

        BenefitDetail first = result.getBenefits().get(0);
        assertEquals("MEDICAL", first.getBenefitType());
        assertEquals(new BigDecimal("1500.00"), first.getAmount());
        assertEquals("INDIVIDUAL", first.getCoverageLevel());
    }

    @Test
    void map_setsFilePathAndReceivedAt() {
        EligibilityResponse result = mapper.map(response, FILE_PATH, RECEIVED_AT);

        assertEquals(FILE_PATH, result.getFilePath());
        assertEquals(RECEIVED_AT, result.getReceivedAt());
    }
}
