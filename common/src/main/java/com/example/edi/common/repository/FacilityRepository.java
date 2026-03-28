package com.example.edi.common.repository;

import com.example.edi.common.document.Facility;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FacilityRepository extends MongoRepository<Facility, String> {
}
