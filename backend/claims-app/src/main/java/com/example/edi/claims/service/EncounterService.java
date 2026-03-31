package com.example.edi.claims.service;

import com.example.edi.claims.dto.DiagnosisResponse;
import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.claims.dto.ProcedureResponse;
import com.example.edi.claims.dto.RequestedProcedureResponse;
import com.example.edi.common.document.RequestedProcedure;
import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final FacilityRepository facilityRepository;
    private final EncounterDiagnosisRepository encounterDiagnosisRepository;
    private final EncounterProcedureRepository encounterProcedureRepository;

    public EncounterService(EncounterRepository encounterRepository,
                            PatientRepository patientRepository,
                            ProviderRepository providerRepository,
                            FacilityRepository facilityRepository,
                            EncounterDiagnosisRepository encounterDiagnosisRepository,
                            EncounterProcedureRepository encounterProcedureRepository) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.facilityRepository = facilityRepository;
        this.encounterDiagnosisRepository = encounterDiagnosisRepository;
        this.encounterProcedureRepository = encounterProcedureRepository;
    }

    public List<EncounterResponse> getAllEncounters() {
        List<Encounter> encounters = encounterRepository.findAll();
        if (encounters.isEmpty()) {
            return List.of();
        }

        List<String> patientIds = encounters.stream().map(Encounter::getPatientId).distinct().toList();
        List<String> providerIds = encounters.stream().map(Encounter::getProviderId).distinct().toList();
        List<String> facilityIds = encounters.stream().map(Encounter::getFacilityId).distinct().toList();

        Map<String, Patient> patientsById = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));
        Map<String, Provider> providersById = providerRepository.findAllById(providerIds).stream()
                .collect(Collectors.toMap(Provider::getId, p -> p));
        Map<String, Facility> facilitiesById = facilityRepository.findAllById(facilityIds).stream()
                .collect(Collectors.toMap(Facility::getId, f -> f));

        Map<String, List<EncounterDiagnosis>> diagnosesByEncounterId = encounterDiagnosisRepository.findAll().stream()
                .collect(Collectors.groupingBy(EncounterDiagnosis::getEncounterId));
        Map<String, List<EncounterProcedure>> proceduresByEncounterId = encounterProcedureRepository.findAll().stream()
                .collect(Collectors.groupingBy(EncounterProcedure::getEncounterId));

        return encounters.stream().map(encounter -> {
            Patient patient = patientsById.get(encounter.getPatientId());
            Provider provider = providersById.get(encounter.getProviderId());
            Facility facility = facilitiesById.get(encounter.getFacilityId());

            String patientName = patient != null
                    ? patient.getFirstName() + " " + patient.getLastName()
                    : "Unknown";
            String providerName = provider != null
                    ? provider.getFirstName() + " " + provider.getLastName()
                    : "Unknown";
            String facilityName = facility != null ? facility.getName() : "Unknown";

            List<DiagnosisResponse> diagnoses = diagnosesByEncounterId
                    .getOrDefault(encounter.getId(), List.of()).stream()
                    .map(d -> new DiagnosisResponse(d.getDiagnosisCode(), d.getRank()))
                    .toList();

            List<ProcedureResponse> procedures = proceduresByEncounterId
                    .getOrDefault(encounter.getId(), List.of()).stream()
                    .map(p -> new ProcedureResponse(
                            p.getProcedureCode(),
                            p.getModifiers(),
                            p.getChargeAmount(),
                            p.getUnits()))
                    .toList();

            List<RequestedProcedureResponse> requestedProcedures = encounter.getRequestedProcedures() != null
                    ? encounter.getRequestedProcedures().stream()
                            .map(rp -> new RequestedProcedureResponse(rp.getProcedureCode(), rp.getClinicalReason()))
                            .toList()
                    : List.of();

            return new EncounterResponse(
                    encounter.getId(),
                    encounter.getPatientId(),
                    patientName,
                    encounter.getProviderId(),
                    providerName,
                    encounter.getFacilityId(),
                    facilityName,
                    encounter.getDateOfService(),
                    encounter.getAuthorizationNumber(),
                    diagnoses,
                    procedures,
                    requestedProcedures
            );
        }).toList();
    }
}
