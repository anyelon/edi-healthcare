package com.example.edi.common.exception;

public class FacilityNotFoundException extends EntityNotFoundException {

    public FacilityNotFoundException(String id) {
        super("Facility", id);
    }
}
