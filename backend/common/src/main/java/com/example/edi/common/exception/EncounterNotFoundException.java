package com.example.edi.common.exception;

public class EncounterNotFoundException extends EntityNotFoundException {

    public EncounterNotFoundException(String id) {
        super("Encounter", id);
    }
}
