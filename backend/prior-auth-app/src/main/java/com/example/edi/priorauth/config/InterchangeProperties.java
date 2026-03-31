package com.example.edi.priorauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.interchange")
public record InterchangeProperties(
    String senderIdQualifier,
    String senderId,
    String receiverIdQualifier,
    String receiverId,
    String ackRequested,
    String usageIndicator
) {}
