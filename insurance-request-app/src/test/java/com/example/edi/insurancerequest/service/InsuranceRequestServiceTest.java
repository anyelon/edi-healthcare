package com.example.edi.insurancerequest.service;

import com.example.edi.common.document.Company;
import com.example.edi.common.document.Patient;
import com.example.edi.common.repository.CompanyRepository;
import com.example.edi.common.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceRequestServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private EDI270Service edi270Service;

    @InjectMocks private InsuranceRequestService insuranceRequestService;

    @Test
    void generateEligibilityInquiry_success() {
        Patient patient = new Patient();
        patient.setId("P001");
        Company company = new Company();

        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(edi270Service.to270(company, patient)).thenReturn("EDI_270_CONTENT");

        String result = insuranceRequestService.generateEligibilityInquiry("P001");

        assertEquals("EDI_270_CONTENT", result);
        verify(edi270Service).to270(company, patient);
    }

    @Test
    void generateEligibilityInquiry_patientNotFound_throwsException() {
        when(patientRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                insuranceRequestService.generateEligibilityInquiry("P001"));
    }

    @Test
    void generateEligibilityInquiry_noCompany_throwsException() {
        Patient patient = new Patient();
        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(companyRepository.findAll()).thenReturn(List.of());

        assertThrows(RuntimeException.class, () ->
                insuranceRequestService.generateEligibilityInquiry("P001"));
    }
}
