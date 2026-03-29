package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import com.example.edi.common.repository.PatientInsuranceRepository;
import com.example.edi.common.repository.PatientRepository;
import com.example.edi.common.repository.PayerRepository;
import com.example.edi.common.repository.PracticeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InsuranceRequestService {

    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final PayerRepository payerRepository;
    private final PracticeRepository practiceRepository;
    private final EDI270Mapper edi270Mapper;
    private final EDI270Generator edi270Generator;
    private final InterchangeProperties interchangeProperties;

    public InsuranceRequestService(PatientRepository patientRepository,
                                   PatientInsuranceRepository patientInsuranceRepository,
                                   PayerRepository payerRepository,
                                   PracticeRepository practiceRepository,
                                   EDI270Mapper edi270Mapper,
                                   EDI270Generator edi270Generator,
                                   InterchangeProperties interchangeProperties) {
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.payerRepository = payerRepository;
        this.practiceRepository = practiceRepository;
        this.edi270Mapper = edi270Mapper;
        this.edi270Generator = edi270Generator;
        this.interchangeProperties = interchangeProperties;
    }

    public String generateEligibilityInquiry(List<String> patientIds) {
        Practice practice = practiceRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No practice found in the system"));

        List<EligibilityBundle> bundles = new ArrayList<>();

        for (String patientId : patientIds) {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

            PatientInsurance insurance = patientInsuranceRepository
                    .findByPatientIdAndTerminationDateIsNull(patientId)
                    .orElseThrow(() -> new RuntimeException(
                            "Active insurance not found for patient: " + patientId));

            Payer payer = payerRepository.findById(insurance.getPayerId())
                    .orElseThrow(() -> new RuntimeException("Payer not found: " + insurance.getPayerId()));

            bundles.add(new EligibilityBundle(patient, insurance, payer));
        }

        var inquiry = edi270Mapper.map(practice, bundles, interchangeProperties);
        return edi270Generator.generate(inquiry);
    }
}
