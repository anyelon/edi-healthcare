package com.example.edi.insurancerequest.domain.loop;

import com.example.edi.common.edi.loop.SubscriberLoop;

public record EligibilitySubscriber(
    SubscriberLoop subscriber,
    String eligibilityDate
) {}
