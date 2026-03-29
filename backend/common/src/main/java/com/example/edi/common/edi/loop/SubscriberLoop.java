package com.example.edi.common.edi.loop;

public record SubscriberLoop(
    String subscriberRelationship,
    String groupNumber,
    String policyType,
    String lastName,
    String firstName,
    String memberId,
    String address,
    String city,
    String state,
    String zipCode,
    String dateOfBirth,
    String genderCode,
    String payerName,
    String payerId
) {}
