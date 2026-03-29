package com.example.edi.common.edi.loop;

public record TransactionHeader(
    String transactionSetControlNumber,
    String referenceId,
    String creationDate,
    String creationTime
) {}
