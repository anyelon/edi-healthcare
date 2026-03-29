package com.example.edi.insurancerequest.dto;

import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;

public record EligibilityBundle(
    Patient patient,
    PatientInsurance insurance,
    Payer payer
) {}
