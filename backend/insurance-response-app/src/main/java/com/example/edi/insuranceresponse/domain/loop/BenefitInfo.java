package com.example.edi.insuranceresponse.domain.loop;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BenefitInfo(String benefitType, String coverageLevel, String serviceType, boolean inNetwork, BigDecimal amount, BigDecimal percent, String timePeriod, String message, LocalDate coverageStartDate, LocalDate coverageEndDate) {}
