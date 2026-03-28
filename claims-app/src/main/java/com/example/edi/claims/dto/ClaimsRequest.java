package com.example.edi.claims.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ClaimsRequest(
        @NotEmpty List<String> encounterIds
) {}
