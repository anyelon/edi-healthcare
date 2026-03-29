package com.example.edi.claims.domain.loop;

import com.example.edi.common.edi.loop.BillingProviderLoop;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.Receiver;
import com.example.edi.common.edi.loop.Submitter;
import com.example.edi.common.edi.loop.TransactionHeader;

import java.util.List;

public record EDI837Claim(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    Submitter submitter,
    Receiver receiver,
    BillingProviderLoop billingProvider,
    List<SubscriberGroup> subscriberGroups
) {}
