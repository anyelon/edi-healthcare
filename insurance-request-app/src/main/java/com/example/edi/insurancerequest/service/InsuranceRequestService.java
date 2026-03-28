package com.example.edi.insurancerequest.service;

import com.example.edi.common.document.Company;
import com.example.edi.common.document.Patient;
import com.example.edi.common.repository.CompanyRepository;
import com.example.edi.common.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class InsuranceRequestService {

    private final PatientRepository patientRepository;
    private final CompanyRepository companyRepository;
    private final EDI270Service edi270Service;

    public InsuranceRequestService(PatientRepository patientRepository,
                                   CompanyRepository companyRepository,
                                   EDI270Service edi270Service) {
        this.patientRepository = patientRepository;
        this.companyRepository = companyRepository;
        this.edi270Service = edi270Service;
    }

    public String generateEligibilityInquiry(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));

        Company company = companyRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No company found in the system"));

        return edi270Service.to270(company, patient);
    }
}
