package com.example.edi.insuranceresponse.controller;

import com.example.edi.insuranceresponse.dto.VerificationResult;
import com.example.edi.insuranceresponse.service.InsuranceResponseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceResponseController {

    private final InsuranceResponseService insuranceResponseService;

    public InsuranceResponseController(InsuranceResponseService insuranceResponseService) {
        this.insuranceResponseService = insuranceResponseService;
    }

    @PostMapping(value = "/eligibility-response", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationResult> processEligibilityResponse(
            @RequestParam("file") MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("edi271-", ".txt");
        try {
            file.transferTo(tempFile.toFile());
            VerificationResult result = insuranceResponseService.processEligibilityResponse(tempFile);
            return ResponseEntity.ok(result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
