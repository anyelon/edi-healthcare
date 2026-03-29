package com.example.edi.insurancerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InsuranceRequestDTO(
        @NotEmpty List<@NotBlank String> patientIds
) {
}
