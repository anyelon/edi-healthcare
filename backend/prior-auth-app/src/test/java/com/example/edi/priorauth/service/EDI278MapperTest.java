package com.example.edi.priorauth.service;

import com.example.edi.common.document.*;
import com.example.edi.priorauth.config.InterchangeProperties;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.dto.PriorAuthBundle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EDI278MapperTest {

    private final EDI278Mapper mapper = new EDI278Mapper();

    @Test
    void map_singleBundle_producesCorrectStructure() {
        InterchangeProperties props = new InterchangeProperties(
                "ZZ", "SENDER", "ZZ", "RECEIVER", "0", "T");
        PriorAuthBundle bundle = createTestBundle();

        EDI278Request result = mapper.map(List.of(bundle), props);

        assertNotNull(result.envelope());
        assertEquals("SENDER", result.envelope().senderId());
        assertEquals("RECEIVER", result.envelope().receiverId());
        assertEquals("278", result.transactionHeader().referenceId());
        assertEquals("Test Payer", result.payerName());
        assertEquals("PAY001", result.payerId());
        assertEquals("Test Practice", result.providerName());
        assertEquals(1, result.subscriberGroups().size());

        EDI278Request.SubscriberReviewGroup group = result.subscriberGroups().getFirst();
        assertEquals("ENC001", group.encounterId());
        assertEquals(1, group.services().size());
        assertEquals("99213", group.services().getFirst().procedureCode());
        assertEquals("Chronic pain management", group.services().getFirst().clinicalReason());
    }

    private PriorAuthBundle createTestBundle() {
        Encounter encounter = new Encounter();
        encounter.setId("ENC001");
        encounter.setPatientId("PAT001");
        encounter.setProviderId("PROV001");
        encounter.setPracticeId("PRAC001");
        encounter.setFacilityId("FAC001");
        encounter.setDateOfService(LocalDate.of(2026, 4, 15));
        encounter.setRequestedProcedures(List.of(
                new RequestedProcedure("99213", "Chronic pain management")
        ));

        Patient patient = new Patient();
        patient.setId("PAT001");
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 15));
        patient.setGender("M");
        patient.setAddress("123 Main St");
        patient.setCity("Springfield");
        patient.setState("IL");
        patient.setZipCode("62701");

        PatientInsurance insurance = new PatientInsurance();
        insurance.setMemberId("MEM001");
        insurance.setGroupNumber("GRP001");
        insurance.setPolicyType("HMO");
        insurance.setSubscriberRelationship("self");

        Payer payer = new Payer();
        payer.setPayerId("PAY001");
        payer.setName("Test Payer");

        Practice practice = new Practice();
        practice.setId("PRAC001");
        practice.setName("Test Practice");
        practice.setNpi("1234567890");
        practice.setTaxId("123456789");

        return new PriorAuthBundle(encounter, patient, insurance, payer, practice,
                encounter.getRequestedProcedures());
    }
}
