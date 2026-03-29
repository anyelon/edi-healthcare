package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDI270MapperTest {

    private EDI270Mapper mapper;
    private InterchangeProperties props;
    private Practice practice;
    private Patient patient;
    private PatientInsurance insurance;
    private Payer payer;

    @BeforeEach
    void setUp() {
        mapper = new EDI270Mapper();
        props = new InterchangeProperties("ZZ", "SENDER001", "ZZ", "RECEIVER01", "0", "T");

        practice = new Practice();
        practice.setName("SUNSHINE HEALTH CLINIC");
        practice.setNpi("1234567890");
        practice.setTaxId("591234567");

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
        insurance.setPatientId("P001");
        insurance.setPayerId("payer1");
        insurance.setMemberId("MEM987654321");
        insurance.setGroupNumber("GRP100234");
        insurance.setPolicyType("MC");
        insurance.setSubscriberRelationship("self");
    }

    private EligibilityBundle makeBundle() {
        return new EligibilityBundle(patient, insurance, payer);
    }

    private EDI270Inquiry doMap() {
        return mapper.map(practice, List.of(makeBundle()), props);
    }

    @Test
    void map_envelopeUsesConfigProperties() {
        EDI270Inquiry inquiry = doMap();

        assertThat(inquiry.envelope().senderIdQualifier()).isEqualTo("ZZ");
        assertThat(inquiry.envelope().senderId()).isEqualTo("SENDER001");
        assertThat(inquiry.envelope().receiverIdQualifier()).isEqualTo("ZZ");
        assertThat(inquiry.envelope().receiverId()).isEqualTo("RECEIVER01");
        assertThat(inquiry.envelope().ackRequested()).isEqualTo("0");
        assertThat(inquiry.envelope().usageIndicator()).isEqualTo("T");
    }

    @Test
    void map_singlePatient_producesOneInformationSourceGroup() {
        EDI270Inquiry inquiry = doMap();

        assertThat(inquiry.informationSourceGroups()).hasSize(1);
        InformationSourceGroup group = inquiry.informationSourceGroups().getFirst();
        assertThat(group.payerName()).isEqualTo("BLUE CROSS BLUE SHIELD");
        assertThat(group.payerId()).isEqualTo("BCBS12345");
    }

    @Test
    void map_informationReceiverFromPractice() {
        EDI270Inquiry inquiry = doMap();

        var receiver = inquiry.informationSourceGroups().getFirst().informationReceiver();
        assertThat(receiver.providerName()).isEqualTo("SUNSHINE HEALTH CLINIC");
        assertThat(receiver.providerNpi()).isEqualTo("1234567890");
        assertThat(receiver.providerTaxId()).isEqualTo("591234567");
    }

    @Test
    void map_subscriberFromPatientAndInsurance() {
        EDI270Inquiry inquiry = doMap();

        var subscriber = inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber();
        assertThat(subscriber.firstName()).isEqualTo("JOHN");
        assertThat(subscriber.lastName()).isEqualTo("SMITH");
        assertThat(subscriber.memberId()).isEqualTo("MEM987654321");
        assertThat(subscriber.groupNumber()).isEqualTo("GRP100234");
        assertThat(subscriber.address()).isEqualTo("456 OAK AVENUE");
        assertThat(subscriber.city()).isEqualTo("ORLANDO");
        assertThat(subscriber.state()).isEqualTo("FL");
        assertThat(subscriber.zipCode()).isEqualTo("32806");
    }

    @Test
    void map_dateOfBirthFormattedAsYYYYMMDD() {
        EDI270Inquiry inquiry = doMap();

        var subscriber = inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber();
        assertThat(subscriber.dateOfBirth()).isEqualTo("19850715");
    }

    @Test
    void map_genderMappedCorrectly() {
        EDI270Inquiry inquiryM = doMap();
        assertThat(inquiryM.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber().genderCode()).isEqualTo("M");

        patient.setGender("F");
        EDI270Inquiry inquiryF = doMap();
        assertThat(inquiryF.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber().genderCode()).isEqualTo("F");

        patient.setGender(null);
        EDI270Inquiry inquiryNull = doMap();
        assertThat(inquiryNull.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().subscriber().genderCode()).isEqualTo("U");
    }

    @Test
    void map_eligibilityDateIsToday() {
        EDI270Inquiry inquiry = doMap();

        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        var eligDate = inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers().getFirst().eligibilityDate();
        assertThat(eligDate).isEqualTo(today);
    }

    @Test
    void map_twoPatientsSamePayer_producesOneGroupWithTwoSubscribers() {
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
        insurance2.setPatientId("P002");
        insurance2.setPayerId("payer1");
        insurance2.setMemberId("MEM111222333");
        insurance2.setGroupNumber("GRP200345");
        insurance2.setPolicyType("MC");
        insurance2.setSubscriberRelationship("self");

        EligibilityBundle bundle1 = makeBundle();
        EligibilityBundle bundle2 = new EligibilityBundle(patient2, insurance2, payer);

        EDI270Inquiry inquiry = mapper.map(practice, List.of(bundle1, bundle2), props);

        assertThat(inquiry.informationSourceGroups()).hasSize(1);
        assertThat(inquiry.informationSourceGroups().getFirst()
                .informationReceiver().subscribers()).hasSize(2);
    }

    @Test
    void map_twoPatientsDifferentPayers_producesTwoGroups() {
        Payer payer2 = new Payer();
        payer2.setName("AETNA");
        payer2.setPayerId("AETNA001");

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
        insurance2.setPatientId("P002");
        insurance2.setPayerId("payer2");
        insurance2.setMemberId("MEM444555666");
        insurance2.setGroupNumber("GRP300456");
        insurance2.setPolicyType("MC");
        insurance2.setSubscriberRelationship("self");

        EligibilityBundle bundle1 = makeBundle();
        EligibilityBundle bundle2 = new EligibilityBundle(patient2, insurance2, payer2);

        EDI270Inquiry inquiry = mapper.map(practice, List.of(bundle1, bundle2), props);

        assertThat(inquiry.informationSourceGroups()).hasSize(2);
        assertThat(inquiry.informationSourceGroups().get(0).payerName()).isEqualTo("BLUE CROSS BLUE SHIELD");
        assertThat(inquiry.informationSourceGroups().get(1).payerName()).isEqualTo("AETNA");
        assertThat(inquiry.informationSourceGroups().get(0)
                .informationReceiver().subscribers()).hasSize(1);
        assertThat(inquiry.informationSourceGroups().get(1)
                .informationReceiver().subscribers()).hasSize(1);
    }
}
