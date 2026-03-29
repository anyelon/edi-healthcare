package com.example.edi.claims.service;

import com.example.edi.claims.config.InterchangeProperties;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.dto.EncounterBundle;
import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClaimsService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final PayerRepository payerRepository;
    private final EncounterDiagnosisRepository encounterDiagnosisRepository;
    private final EncounterProcedureRepository encounterProcedureRepository;
    private final PracticeRepository practiceRepository;
    private final ProviderRepository providerRepository;
    private final FacilityRepository facilityRepository;
    private final EDI837Mapper edi837Mapper;
    private final EDI837Generator edi837Generator;
    private final InterchangeProperties interchangeProperties;

    public ClaimsService(EncounterRepository encounterRepository,
                         PatientRepository patientRepository,
                         PatientInsuranceRepository patientInsuranceRepository,
                         PayerRepository payerRepository,
                         EncounterDiagnosisRepository encounterDiagnosisRepository,
                         EncounterProcedureRepository encounterProcedureRepository,
                         PracticeRepository practiceRepository,
                         ProviderRepository providerRepository,
                         FacilityRepository facilityRepository,
                         EDI837Mapper edi837Mapper,
                         EDI837Generator edi837Generator,
                         InterchangeProperties interchangeProperties) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.payerRepository = payerRepository;
        this.encounterDiagnosisRepository = encounterDiagnosisRepository;
        this.encounterProcedureRepository = encounterProcedureRepository;
        this.practiceRepository = practiceRepository;
        this.providerRepository = providerRepository;
        this.facilityRepository = facilityRepository;
        this.edi837Mapper = edi837Mapper;
        this.edi837Generator = edi837Generator;
        this.interchangeProperties = interchangeProperties;
    }

    public String generateClaim(List<String> encounterIds) {
        List<EncounterBundle> bundles = new ArrayList<>();
        Practice practice = null;

        for (String encounterId : encounterIds) {
            Encounter encounter = encounterRepository.findById(encounterId)
                    .orElseThrow(() -> new RuntimeException("Encounter not found: " + encounterId));

            Patient patient = patientRepository.findById(encounter.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + encounter.getPatientId()));

            PatientInsurance insurance = patientInsuranceRepository
                    .findByPatientIdAndTerminationDateIsNull(encounter.getPatientId())
                    .orElseThrow(() -> new RuntimeException(
                            "Active insurance not found for patient: " + encounter.getPatientId()));

            Payer payer = payerRepository.findById(insurance.getPayerId())
                    .orElseThrow(() -> new RuntimeException("Payer not found: " + insurance.getPayerId()));

            List<EncounterDiagnosis> diagnoses =
                    encounterDiagnosisRepository.findByEncounterIdOrderByRankAsc(encounterId);

            List<EncounterProcedure> procedures =
                    encounterProcedureRepository.findByEncounterIdOrderByLineNumberAsc(encounterId);

            if (practice == null) {
                practice = practiceRepository.findById(encounter.getPracticeId())
                        .orElseThrow(() -> new RuntimeException("Practice not found: " + encounter.getPracticeId()));
            }

            Facility facility = facilityRepository.findById(encounter.getFacilityId())
                    .orElseThrow(() -> new RuntimeException("Facility not found: " + encounter.getFacilityId()));

            bundles.add(new EncounterBundle(patient, insurance, payer, encounter, diagnoses, procedures, facility));
        }

        EDI837Claim claim = edi837Mapper.map(practice, bundles, interchangeProperties);

        return edi837Generator.generate(claim);
    }
}
