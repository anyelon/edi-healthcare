package com.example.edi.common.repository;

import com.example.edi.common.document.Encounter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EncounterRepository extends MongoRepository<Encounter, String> {
}
