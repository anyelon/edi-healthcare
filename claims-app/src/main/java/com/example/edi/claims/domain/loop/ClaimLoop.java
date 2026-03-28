package com.example.edi.claims.domain.loop;

import java.math.BigDecimal;
import java.util.List;

public record ClaimLoop(
    String claimId,
    BigDecimal totalCharge,
    String placeOfServiceCode,
    List<DiagnosisEntry> diagnoses,
    List<ServiceLineLoop> serviceLines
) {}
