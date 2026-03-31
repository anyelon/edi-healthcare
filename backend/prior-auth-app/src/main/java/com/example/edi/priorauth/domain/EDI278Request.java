package com.example.edi.priorauth.domain;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import java.util.List;

public record EDI278Request(
        InterchangeEnvelope envelope,
        FunctionalGroup functionalGroup,
        TransactionHeader transactionHeader,
        String payerName,
        String payerId,
        String providerName,
        String providerNpi,
        String providerTaxId,
        List<SubscriberReviewGroup> subscriberGroups
) {
    public record SubscriberReviewGroup(
            SubscriberLoop subscriber,
            String encounterId,
            List<ServiceReviewInfo> services
    ) {}
}
