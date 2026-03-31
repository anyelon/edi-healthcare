package com.example.edi.priorauth.domain;

public record ServiceReviewInfo(
        String procedureCode,
        String clinicalReason,
        String serviceDate
) {}
