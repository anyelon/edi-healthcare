package com.example.edi.common.repository;

import com.example.edi.common.document.PatientInsurance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientInsuranceRepository extends MongoRepository<PatientInsurance, String> {
    Optional<PatientInsurance> findByPatientIdAndTerminationDateIsNull(String patientId);
}
