package com.example.edi.common.exception;

public class PracticeNotFoundException extends EntityNotFoundException {

    public PracticeNotFoundException(String id) {
        super("Practice", id);
    }
}
