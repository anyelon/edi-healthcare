package com.example.edi.claims.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProcedureResponse(
        String procedureCode,
        List<String> modifiers,
        BigDecimal chargeAmount,
        int units,
        boolean needsAuth,
        String clinicalReason
) {}
