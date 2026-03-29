package com.example.edi.common.repository;

import com.example.edi.common.document.Payer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PayerRepository extends MongoRepository<Payer, String> {
}
