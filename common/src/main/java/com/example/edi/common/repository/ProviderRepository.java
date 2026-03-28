package com.example.edi.common.repository;

import com.example.edi.common.document.Provider;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProviderRepository extends MongoRepository<Provider, String> {
}
