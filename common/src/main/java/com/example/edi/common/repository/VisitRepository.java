package com.example.edi.common.repository;

import com.example.edi.common.document.Visit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface VisitRepository extends MongoRepository<Visit, String> {

    Optional<Visit> findByPatientIdAndDateOfService(String patientId, LocalDate dateOfService);
}
