package com.example.edi.common.repository;

import com.example.edi.common.document.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<Patient, String> {
}
