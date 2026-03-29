package com.example.edi.claims.dto;

import com.example.edi.common.document.*;

import java.util.List;

public record EncounterBundle(
    Patient patient,
    PatientInsurance insurance,
    Payer payer,
    Encounter encounter,
    List<EncounterDiagnosis> diagnoses,
    List<EncounterProcedure> procedures,
    Facility facility
) {}
