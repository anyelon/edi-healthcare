package com.example.edi.priorauth.domain;

public record AuthorizationDecision(
        String action,
        String authorizationNumber,
        String encounterId
) {}
