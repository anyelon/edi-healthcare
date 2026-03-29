package com.example.edi.claims.dto;

import java.time.LocalDate;

public record PatientResponse(
        String id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String city,
        String state,
        String zipCode,
        String phone
) {}
