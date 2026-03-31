package com.example.edi.priorauth.domain;

import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.TransactionHeader;
import java.util.List;

public record EDI278Response(
        InterchangeEnvelope envelope,
        FunctionalGroup functionalGroup,
        TransactionHeader transactionHeader,
        String payerName,
        String payerId,
        String subscriberFirstName,
        String subscriberLastName,
        String memberId,
        List<AuthorizationDecision> decisions
) {}
