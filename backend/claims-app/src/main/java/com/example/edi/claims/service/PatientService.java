package com.example.edi.claims.service;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.common.document.Patient;
import com.example.edi.common.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getAddress(),
                patient.getCity(),
                patient.getState(),
                patient.getZipCode(),
                patient.getPhone()
        );
    }
}
