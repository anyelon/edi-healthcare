package com.example.edi.priorauth.controller;

import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.priorauth.dto.PriorAuthRequestDTO;
import com.example.edi.priorauth.service.PriorAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/prior-auth")
public class PriorAuthController {

    private final PriorAuthService priorAuthService;

    public PriorAuthController(PriorAuthService priorAuthService) {
        this.priorAuthService = priorAuthService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generatePriorAuth(@Valid @RequestBody PriorAuthRequestDTO request) {
        String ediContent = priorAuthService.generatePriorAuth(request.encounterIds());
        byte[] bytes = ediContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=278_prior_auth.edi")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @PostMapping(value = "/response", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EDI278Response> processResponse(@RequestParam("file") MultipartFile file) throws Exception {
        EDI278Response result = priorAuthService.processResponse(file);
        return ResponseEntity.ok(result);
    }
}
