package com.example.edi.insurancerequest.domain.loop;

import java.util.List;

public record InformationReceiverGroup(
    String providerName,
    String providerNpi,
    String providerTaxId,
    List<EligibilitySubscriber> subscribers
) {}
