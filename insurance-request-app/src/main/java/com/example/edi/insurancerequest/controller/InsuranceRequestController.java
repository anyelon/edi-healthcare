package com.example.edi.insurancerequest.controller;

import com.example.edi.insurancerequest.dto.InsuranceRequestDTO;
import com.example.edi.insurancerequest.service.InsuranceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceRequestController {

    private final InsuranceRequestService insuranceRequestService;

    public InsuranceRequestController(InsuranceRequestService insuranceRequestService) {
        this.insuranceRequestService = insuranceRequestService;
    }

    @PostMapping("/eligibility-request")
    public ResponseEntity<byte[]> requestEligibility(@Valid @RequestBody InsuranceRequestDTO request) {
        String ediContent = insuranceRequestService.generateEligibilityInquiry(request.patientIds());
        byte[] bytes = ediContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=270_inquiry.edi")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
