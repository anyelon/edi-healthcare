package com.example.edi.common.repository;

import com.example.edi.common.document.EncounterDiagnosis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EncounterDiagnosisRepository extends MongoRepository<EncounterDiagnosis, String> {
    List<EncounterDiagnosis> findByEncounterIdOrderByRankAsc(String encounterId);
}
