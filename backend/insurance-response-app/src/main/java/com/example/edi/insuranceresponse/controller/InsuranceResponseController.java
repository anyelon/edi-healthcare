package com.example.edi.insuranceresponse.controller;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.insuranceresponse.service.EligibilityResponseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceResponseController {

    private final EligibilityResponseService eligibilityResponseService;

    public InsuranceResponseController(EligibilityResponseService eligibilityResponseService) {
        this.eligibilityResponseService = eligibilityResponseService;
    }

    @PostMapping(value = "/eligibility-response", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EligibilityResponse> processEligibilityResponse(
            @RequestParam("file") MultipartFile file) throws Exception {
        EligibilityResponse result = eligibilityResponseService.processFile(file);
        return ResponseEntity.ok(result);
    }
}
