package com.example.edi.claims.controller;

import com.example.edi.claims.dto.ClaimsRequest;
import com.example.edi.claims.service.ClaimsService;
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
@RequestMapping("/api/claims")
public class ClaimsController {

    private final ClaimsService claimsService;

    public ClaimsController(ClaimsService claimsService) {
        this.claimsService = claimsService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateClaim(@Valid @RequestBody ClaimsRequest request) {
        String ediContent = claimsService.generateClaim(request.encounterIds());
        byte[] bytes = ediContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=837_claim.edi")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
