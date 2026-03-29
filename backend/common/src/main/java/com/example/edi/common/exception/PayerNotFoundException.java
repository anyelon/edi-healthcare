package com.example.edi.common.exception;

public class PayerNotFoundException extends EntityNotFoundException {

    public PayerNotFoundException(String id) {
        super("Payer", id);
    }
}
