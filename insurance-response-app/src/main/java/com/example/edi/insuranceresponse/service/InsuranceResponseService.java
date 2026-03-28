package com.example.edi.insuranceresponse.service;

import com.example.edi.insuranceresponse.dto.VerificationResult;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class InsuranceResponseService {

    private final EDI271Service edi271Service;

    public InsuranceResponseService(EDI271Service edi271Service) {
        this.edi271Service = edi271Service;
    }

    public VerificationResult processEligibilityResponse(Path filePath) {
        return edi271Service.parse(filePath);
    }
}
