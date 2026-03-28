package com.example.edi.common.repository;

import com.example.edi.common.document.PlaceOfService;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PlaceOfServiceRepository extends MongoRepository<PlaceOfService, String> {

    Optional<PlaceOfService> findByCode(String code);
}
