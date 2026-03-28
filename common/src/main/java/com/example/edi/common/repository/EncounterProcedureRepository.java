package com.example.edi.common.repository;

import com.example.edi.common.document.EncounterProcedure;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EncounterProcedureRepository extends MongoRepository<EncounterProcedure, String> {
    List<EncounterProcedure> findByEncounterIdOrderByLineNumberAsc(String encounterId);
}
