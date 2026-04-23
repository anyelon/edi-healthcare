package com.example.edi.common.edi.ack;

import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import java.util.List;

public record EDI999Acknowledgment(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    String acknowledgedGroupControlNumber,
    String acknowledgedTransactionSetId,
    String acknowledgedTransactionControlNumber,
    TransactionSetStatus transactionStatus,
    FunctionalGroupStatus groupStatus,
    int transactionSetsIncluded,
    int transactionSetsReceived,
    int transactionSetsAccepted,
    List<SegmentError> errors
) {}
