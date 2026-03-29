package com.example.edi.insurancerequest.controller;

import com.example.edi.common.dto.ErrorResponse;
import com.example.edi.common.exception.EdiParseException;
import com.example.edi.common.exception.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ErrorResponse.notFound(ex.getEntityType(), ex.getEntityId()));
    }

    @ExceptionHandler(EdiParseException.class)
    public ResponseEntity<ErrorResponse> handleEdiParse(EdiParseException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(ex.getMessage()));
    }
}
