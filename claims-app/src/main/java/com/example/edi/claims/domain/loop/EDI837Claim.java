package com.example.edi.claims.domain.loop;

import com.example.edi.common.edi.loop.BillingProviderLoop;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.Receiver;
import com.example.edi.common.edi.loop.Submitter;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;

public record EDI837Claim(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    Submitter submitter,
    Receiver receiver,
    BillingProviderLoop billingProvider,
    SubscriberLoop subscriber,
    ClaimLoop claim
) {}
