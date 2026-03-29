package com.example.edi.common.exception;

public class PatientNotFoundException extends EntityNotFoundException {

    public PatientNotFoundException(String id) {
        super("Patient", id);
    }
}
