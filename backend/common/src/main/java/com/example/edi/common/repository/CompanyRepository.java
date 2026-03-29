package com.example.edi.common.repository;

import com.example.edi.common.document.Company;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyRepository extends MongoRepository<Company, String> {
}
