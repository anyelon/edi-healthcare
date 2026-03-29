package com.example.edi.common.edi.loop;

public record BillingProviderLoop(
    String name,
    String npi,
    String taxId,
    String address,
    String city,
    String state,
    String zipCode
) {}
