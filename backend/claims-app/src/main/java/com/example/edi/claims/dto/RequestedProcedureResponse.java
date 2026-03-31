package com.example.edi.claims.dto;

public record RequestedProcedureResponse(
        String procedureCode,
        String clinicalReason
) {}
