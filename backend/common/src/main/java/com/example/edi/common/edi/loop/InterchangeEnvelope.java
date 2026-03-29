package com.example.edi.common.edi.loop;

public record InterchangeEnvelope(
    String senderIdQualifier,
    String senderId,
    String receiverIdQualifier,
    String receiverId,
    String date,
    String time,
    String controlNumber,
    String ackRequested,
    String usageIndicator
) {}
