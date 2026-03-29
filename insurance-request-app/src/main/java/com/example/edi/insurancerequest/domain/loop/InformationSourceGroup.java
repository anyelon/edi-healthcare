package com.example.edi.insurancerequest.domain.loop;

public record InformationSourceGroup(
    String payerName,
    String payerId,
    InformationReceiverGroup informationReceiver
) {}
