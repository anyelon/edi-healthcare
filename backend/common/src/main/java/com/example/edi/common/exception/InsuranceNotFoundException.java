package com.example.edi.common.exception;

public class InsuranceNotFoundException extends EntityNotFoundException {

    public InsuranceNotFoundException(String patientId) {
        super("Insurance", patientId);
    }
}
