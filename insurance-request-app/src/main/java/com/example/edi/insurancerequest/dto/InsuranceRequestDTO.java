package com.example.edi.insurancerequest.dto;

import jakarta.validation.constraints.NotBlank;

public record InsuranceRequestDTO(
        @NotBlank String patientId
) {
}
