package com.example.edi.claims.domain.loop;

import java.math.BigDecimal;
import java.util.List;

public record ServiceLineLoop(
    int lineNumber,
    String procedureCode,
    List<String> modifiers,
    BigDecimal chargeAmount,
    int units,
    String unitType,
    List<Integer> diagnosisPointers,
    String dateOfService
) {}
