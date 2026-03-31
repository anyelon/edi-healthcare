package com.example.edi.priorauth.dto;

import com.example.edi.common.document.*;
import java.util.List;

public record PriorAuthBundle(
        Encounter encounter,
        Patient patient,
        PatientInsurance insurance,
        Payer payer,
        Practice practice,
        List<EncounterProcedure> authProcedures
) {}
