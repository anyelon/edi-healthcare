package com.example.edi.common.repository;

import com.example.edi.common.document.EligibilityResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EligibilityResponseRepository extends MongoRepository<EligibilityResponse, String> {
    List<EligibilityResponse> findByMemberIdOrderByReceivedAtDesc(String memberId);
}
