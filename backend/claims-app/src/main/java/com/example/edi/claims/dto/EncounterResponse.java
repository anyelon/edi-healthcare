package com.example.edi.claims.dto;

import java.time.LocalDate;
import java.util.List;

public record EncounterResponse(
        String id,
        String patientId,
        String patientName,
        String providerId,
        String providerName,
        String facilityId,
        String facilityName,
        LocalDate dateOfService,
        String authorizationNumber,
        List<DiagnosisResponse> diagnoses,
        List<ProcedureResponse> procedures,
        List<RequestedProcedureResponse> requestedProcedures
) {}
