package com.example.edi.claims.service;

import com.example.edi.claims.config.InterchangeProperties;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.SubscriberGroup;
import com.example.edi.claims.dto.EncounterBundle;
import com.example.edi.common.document.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDI837MapperTest {

    private EDI837Mapper mapper;

    private InterchangeProperties props;
    private Practice practice;
    private Patient patient;
    private Payer payer;
    private PatientInsurance insurance;
    private Encounter encounter;
    private Facility facility;
    private List<EncounterDiagnosis> diagnoses;
    private List<EncounterProcedure> procedures;

    @BeforeEach
    void setUp() {
        mapper = new EDI837Mapper();

        props = new InterchangeProperties("ZZ", "SENDER001", "ZZ", "RECEIVER01", "0", "T");

        practice = new Practice();
        practice.setName("SUNSHINE HEALTH CLINIC");
        practice.setNpi("1234567890");
        practice.setTaxId("591234567");
        practice.setAddress("100 MEDICAL PLAZA DR");
        practice.setCity("ORLANDO");
        practice.setState("FL");
        practice.setZipCode("32801");
        practice.setContactPhone("5551234567");

        patient = new Patient();
        patient.setId("P001");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");
        patient.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient.setGender("M");
        patient.setAddress("456 OAK AVENUE");
        patient.setCity("ORLANDO");
        patient.setState("FL");
        patient.setZipCode("32806");

        payer = new Payer();
        payer.setName("BLUE CROSS BLUE SHIELD");
        payer.setPayerId("BCBS12345");

        insurance = new PatientInsurance();
        insurance.setMemberId("MEM987654321");
        insurance.setGroupNumber("GRP100234");
        insurance.setPolicyType("MC");
        insurance.setSubscriberRelationship("self");

        encounter = new Encounter();
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));

        facility = new Facility();
        facility.setPlaceOfServiceCode("11");

        EncounterDiagnosis diag1 = new EncounterDiagnosis();
        diag1.setRank(1);
        diag1.setDiagnosisCode("J06.9");

        EncounterDiagnosis diag2 = new EncounterDiagnosis();
        diag2.setRank(2);
        diag2.setDiagnosisCode("R05.9");

        diagnoses = List.of(diag1, diag2);

        EncounterProcedure proc1 = new EncounterProcedure();
        proc1.setLineNumber(1);
        proc1.setProcedureCode("99213");
        proc1.setModifiers(List.of());
        proc1.setChargeAmount(new BigDecimal("150.00"));
        proc1.setUnits(1);
        proc1.setUnitType("UN");
        proc1.setDiagnosisPointers(List.of(1, 2));

        EncounterProcedure proc2 = new EncounterProcedure();
        proc2.setLineNumber(2);
        proc2.setProcedureCode("87880");
        proc2.setModifiers(List.of());
        proc2.setChargeAmount(new BigDecimal("100.00"));
        proc2.setUnits(1);
        proc2.setUnitType("UN");
        proc2.setDiagnosisPointers(List.of(1));

        procedures = List.of(proc1, proc2);
    }

    private EncounterBundle makeBundle() {
        return new EncounterBundle(patient, insurance, payer, encounter, diagnoses, procedures, facility);
    }

    private EDI837Claim doMap() {
        return mapper.map(practice, List.of(makeBundle()), props);
    }

    @Test
    void map_billingProviderFromPractice() {
        EDI837Claim claim = doMap();

        assertThat(claim.billingProvider().name()).isEqualTo("SUNSHINE HEALTH CLINIC");
        assertThat(claim.billingProvider().npi()).isEqualTo("1234567890");
        assertThat(claim.billingProvider().taxId()).isEqualTo("591234567");
        assertThat(claim.billingProvider().address()).isEqualTo("100 MEDICAL PLAZA DR");
        assertThat(claim.billingProvider().city()).isEqualTo("ORLANDO");
        assertThat(claim.billingProvider().state()).isEqualTo("FL");
        assertThat(claim.billingProvider().zipCode()).isEqualTo("32801");
    }

    @Test
    void map_subscriberFromPatientAndInsurance() {
        EDI837Claim claim = doMap();

        assertThat(claim.subscriberGroups()).hasSize(1);
        var subscriber = claim.subscriberGroups().getFirst().subscriber();
        assertThat(subscriber.firstName()).isEqualTo("JOHN");
        assertThat(subscriber.lastName()).isEqualTo("SMITH");
        assertThat(subscriber.memberId()).isEqualTo("MEM987654321");
        assertThat(subscriber.groupNumber()).isEqualTo("GRP100234");
        assertThat(subscriber.policyType()).isEqualTo("MC");
        assertThat(subscriber.payerName()).isEqualTo("BLUE CROSS BLUE SHIELD");
        assertThat(subscriber.payerId()).isEqualTo("BCBS12345");
        assertThat(subscriber.address()).isEqualTo("456 OAK AVENUE");
        assertThat(subscriber.city()).isEqualTo("ORLANDO");
        assertThat(subscriber.state()).isEqualTo("FL");
        assertThat(subscriber.zipCode()).isEqualTo("32806");
    }

    @Test
    void map_dateOfBirthFormattedAsYYYYMMDD() {
        EDI837Claim claim = doMap();

        assertThat(claim.subscriberGroups().getFirst().subscriber().dateOfBirth()).isEqualTo("19850715");
    }

    @Test
    void map_genderMappedCorrectly() {
        EDI837Claim claimM = doMap();
        assertThat(claimM.subscriberGroups().getFirst().subscriber().genderCode()).isEqualTo("M");

        patient.setGender("F");
        EDI837Claim claimF = doMap();
        assertThat(claimF.subscriberGroups().getFirst().subscriber().genderCode()).isEqualTo("F");

        patient.setGender(null);
        EDI837Claim claimNull = doMap();
        assertThat(claimNull.subscriberGroups().getFirst().subscriber().genderCode()).isEqualTo("U");
    }

    @Test
    void map_diagnosesOrderedByRank() {
        EDI837Claim claim = doMap();

        var claimDiagnoses = claim.subscriberGroups().getFirst().claims().getFirst().diagnoses();
        assertThat(claimDiagnoses).hasSize(2);
        assertThat(claimDiagnoses.get(0).rank()).isEqualTo(1);
        assertThat(claimDiagnoses.get(0).diagnosisCode()).isEqualTo("J06.9");
        assertThat(claimDiagnoses.get(1).rank()).isEqualTo(2);
        assertThat(claimDiagnoses.get(1).diagnosisCode()).isEqualTo("R05.9");
    }

    @Test
    void map_totalChargeCalculatedFromProcedures() {
        EDI837Claim claim = doMap();

        assertThat(claim.subscriberGroups().getFirst().claims().getFirst().totalCharge())
                .isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void map_envelopeUsesConfigProperties() {
        EDI837Claim claim = doMap();

        assertThat(claim.envelope().senderIdQualifier()).isEqualTo("ZZ");
        assertThat(claim.envelope().senderId()).isEqualTo("SENDER001");
        assertThat(claim.envelope().receiverIdQualifier()).isEqualTo("ZZ");
        assertThat(claim.envelope().receiverId()).isEqualTo("RECEIVER01");
        assertThat(claim.envelope().ackRequested()).isEqualTo("0");
        assertThat(claim.envelope().usageIndicator()).isEqualTo("T");
    }

    @Test
    void map_placeOfServiceFromFacility() {
        EDI837Claim claim = doMap();

        assertThat(claim.subscriberGroups().getFirst().claims().getFirst().placeOfServiceCode()).isEqualTo("11");
    }

    @Test
    void map_singleEncounter_producesSingleSubscriberGroupWithOneClaim() {
        EDI837Claim claim = doMap();

        assertThat(claim.subscriberGroups()).hasSize(1);
        assertThat(claim.subscriberGroups().getFirst().claims()).hasSize(1);
    }

    @Test
    void map_twoEncountersSamePatient_producesSingleSubscriberGroupWithTwoClaims() {
        Encounter encounter2 = new Encounter();
        encounter2.setDateOfService(LocalDate.of(2026, 3, 16));

        EncounterDiagnosis diag = new EncounterDiagnosis();
        diag.setRank(1);
        diag.setDiagnosisCode("Z00.00");

        EncounterProcedure proc = new EncounterProcedure();
        proc.setLineNumber(1);
        proc.setProcedureCode("99214");
        proc.setModifiers(List.of());
        proc.setChargeAmount(new BigDecimal("200.00"));
        proc.setUnits(1);
        proc.setUnitType("UN");
        proc.setDiagnosisPointers(List.of(1));

        Facility facility2 = new Facility();
        facility2.setPlaceOfServiceCode("11");

        EncounterBundle bundle1 = makeBundle();
        EncounterBundle bundle2 = new EncounterBundle(patient, insurance, payer, encounter2, List.of(diag), List.of(proc), facility2);

        EDI837Claim claim = mapper.map(practice, List.of(bundle1, bundle2), props);

        assertThat(claim.subscriberGroups()).hasSize(1);
        assertThat(claim.subscriberGroups().getFirst().claims()).hasSize(2);
    }

    @Test
    void map_twoEncountersDifferentPatients_producesTwoSubscriberGroups() {
        Patient patient2 = new Patient();
        patient2.setId("P002");
        patient2.setFirstName("JANE");
        patient2.setLastName("DOE");
        patient2.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient2.setGender("F");
        patient2.setAddress("789 ELM ST");
        patient2.setCity("ORLANDO");
        patient2.setState("FL");
        patient2.setZipCode("32807");

        PatientInsurance insurance2 = new PatientInsurance();
        insurance2.setMemberId("MEM111222333");
        insurance2.setGroupNumber("GRP200345");
        insurance2.setPolicyType("MC");
        insurance2.setSubscriberRelationship("self");

        Encounter encounter2 = new Encounter();
        encounter2.setDateOfService(LocalDate.of(2026, 3, 16));

        EncounterDiagnosis diag = new EncounterDiagnosis();
        diag.setRank(1);
        diag.setDiagnosisCode("Z00.00");

        EncounterProcedure proc = new EncounterProcedure();
        proc.setLineNumber(1);
        proc.setProcedureCode("99214");
        proc.setModifiers(List.of());
        proc.setChargeAmount(new BigDecimal("200.00"));
        proc.setUnits(1);
        proc.setUnitType("UN");
        proc.setDiagnosisPointers(List.of(1));

        Facility facility2 = new Facility();
        facility2.setPlaceOfServiceCode("11");

        EncounterBundle bundle1 = makeBundle();
        EncounterBundle bundle2 = new EncounterBundle(patient2, insurance2, payer, encounter2, List.of(diag), List.of(proc), facility2);

        EDI837Claim claim = mapper.map(practice, List.of(bundle1, bundle2), props);

        assertThat(claim.subscriberGroups()).hasSize(2);
        assertThat(claim.subscriberGroups().get(0).claims()).hasSize(1);
        assertThat(claim.subscriberGroups().get(1).claims()).hasSize(1);
        assertThat(claim.subscriberGroups().get(0).subscriber().lastName()).isEqualTo("SMITH");
        assertThat(claim.subscriberGroups().get(1).subscriber().lastName()).isEqualTo("DOE");
    }
}
