package com.example.edi.insuranceresponse.controller;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.insuranceresponse.service.EDI999Service;
import com.example.edi.insuranceresponse.service.EligibilityResponseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceResponseController {

    private final EligibilityResponseService eligibilityResponseService;
    private final EDI999Service edi999Service;

    public InsuranceResponseController(EligibilityResponseService eligibilityResponseService,
                                       EDI999Service edi999Service) {
        this.eligibilityResponseService = eligibilityResponseService;
        this.edi999Service = edi999Service;
    }

    @PostMapping(value = "/eligibility-response", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EligibilityResponse> processEligibilityResponse(
            @RequestParam("file") MultipartFile file) throws Exception {
        EligibilityResponse result = eligibilityResponseService.processFile(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/acknowledgment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EDI999Acknowledgment>> processAcknowledgment(
            @RequestParam("file") MultipartFile file) throws Exception {
        List<EDI999Acknowledgment> results = edi999Service.processFile(file);
        return ResponseEntity.ok(results);
    }
}
