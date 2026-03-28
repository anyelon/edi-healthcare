package com.example.edi.claims.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimsRequest(
        @NotBlank String encounterId
) {
}
