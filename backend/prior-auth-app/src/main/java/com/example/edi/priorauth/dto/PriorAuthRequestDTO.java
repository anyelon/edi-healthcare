package com.example.edi.priorauth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PriorAuthRequestDTO(
        @NotEmpty List<@NotBlank String> encounterIds
) {}
