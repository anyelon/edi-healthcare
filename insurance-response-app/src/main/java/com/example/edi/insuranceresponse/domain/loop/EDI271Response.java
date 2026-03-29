package com.example.edi.insuranceresponse.domain.loop;

import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.TransactionHeader;
import java.util.List;

public record EDI271Response(InterchangeEnvelope envelope, FunctionalGroup functionalGroup, TransactionHeader transactionHeader, SubscriberInfo subscriberInfo, PayerInfo payerInfo, List<BenefitInfo> benefits) {}
