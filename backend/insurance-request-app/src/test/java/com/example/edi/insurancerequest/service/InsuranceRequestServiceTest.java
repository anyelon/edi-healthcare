package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import com.example.edi.common.repository.PatientInsuranceRepository;
import com.example.edi.common.repository.PatientRepository;
import com.example.edi.common.repository.PayerRepository;
import com.example.edi.common.repository.PracticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceRequestServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientInsuranceRepository patientInsuranceRepository;
    @Mock private PayerRepository payerRepository;
    @Mock private PracticeRepository practiceRepository;
    @Mock private EDI270Mapper edi270Mapper;
    @Mock private EDI270Generator edi270Generator;

    private InterchangeProperties props;
    private InsuranceRequestService service;

    @BeforeEach
    void setUp() {
        props = new InterchangeProperties("ZZ", "SENDER001", "ZZ", "RECEIVER01", "0", "T");
        service = new InsuranceRequestService(
                patientRepository, patientInsuranceRepository, payerRepository,
                practiceRepository, edi270Mapper, edi270Generator, props);
    }

    @Test
    void generateEligibilityInquiry_singlePatient_callsMapperAndGenerator() {
        Patient patient = new Patient();
        patient.setId("P001");

        PatientInsurance insurance = new PatientInsurance();
        insurance.setPayerId("payer1");

        Payer payer = new Payer();
        payer.setPayerId("BCBS12345");

        Practice practice = new Practice();

        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(patientInsuranceRepository.findByPatientIdAndTerminationDateIsNull("P001"))
                .thenReturn(Optional.of(insurance));
        when(payerRepository.findById("payer1")).thenReturn(Optional.of(payer));
        when(practiceRepository.findAll()).thenReturn(List.of(practice));
        when(edi270Mapper.map(any(), any(), any())).thenReturn(null);
        when(edi270Generator.generate(any())).thenReturn("EDI_CONTENT");

        String result = service.generateEligibilityInquiry(List.of("P001"));

        assertThat(result).isEqualTo("EDI_CONTENT");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EligibilityBundle>> bundlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(edi270Mapper).map(eq(practice), bundlesCaptor.capture(), eq(props));
        assertThat(bundlesCaptor.getValue()).hasSize(1);
        assertThat(bundlesCaptor.getValue().getFirst().patient()).isSameAs(patient);
    }

    @Test
    void generateEligibilityInquiry_patientNotFound_throwsException() {
        when(practiceRepository.findAll()).thenReturn(List.of(new Practice()));
        when(patientRepository.findById("P999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateEligibilityInquiry(List.of("P999")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void generateEligibilityInquiry_noActiveInsurance_throwsException() {
        Patient patient = new Patient();
        patient.setId("P001");

        when(practiceRepository.findAll()).thenReturn(List.of(new Practice()));
        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(patientInsuranceRepository.findByPatientIdAndTerminationDateIsNull("P001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateEligibilityInquiry(List.of("P001")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Active insurance not found");
    }

    @Test
    void generateEligibilityInquiry_noPractice_throwsException() {
        when(practiceRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateEligibilityInquiry(List.of("P001")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No practice found");
    }
}
