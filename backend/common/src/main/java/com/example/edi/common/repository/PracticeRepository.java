package com.example.edi.common.repository;

import com.example.edi.common.document.Practice;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PracticeRepository extends MongoRepository<Practice, String> {
}
