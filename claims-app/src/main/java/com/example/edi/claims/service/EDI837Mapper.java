package com.example.edi.claims.service;

import com.example.edi.claims.config.InterchangeProperties;
import com.example.edi.claims.domain.loop.ClaimLoop;
import com.example.edi.claims.domain.loop.DiagnosisEntry;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.ServiceLineLoop;
import com.example.edi.common.document.*;
import com.example.edi.common.edi.loop.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class EDI837Mapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HHmm");

    public EDI837Claim map(Practice practice,
                           Provider provider,
                           Patient patient,
                           PatientInsurance insurance,
                           Payer payer,
                           Encounter encounter,
                           List<EncounterDiagnosis> diagnoses,
                           List<EncounterProcedure> procedures,
                           Facility facility,
                           InterchangeProperties props) {

        LocalDateTime now = LocalDateTime.now();
        String nowDate   = now.format(YYYYMMDD);
        String nowYYMMDD = now.format(YYMMDD);
        String nowTime   = now.format(HHMM);

        long millis        = System.currentTimeMillis();
        String controlNum9 = String.format("%09d", millis % 1_000_000_000L);
        String controlNum5 = String.format("%05d", millis % 100_000L);

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                props.senderIdQualifier(),
                props.senderId(),
                props.receiverIdQualifier(),
                props.receiverId(),
                nowYYMMDD,
                nowTime,
                controlNum9,
                props.ackRequested(),
                props.usageIndicator()
        );

        FunctionalGroup functionalGroup = new FunctionalGroup(
                props.senderId(),
                props.receiverId(),
                nowDate,
                nowTime,
                controlNum5
        );

        TransactionHeader transactionHeader = new TransactionHeader(
                controlNum5,
                "837",
                nowDate,
                nowTime
        );

        Submitter submitter = new Submitter(
                practice.getName(),
                practice.getNpi(),
                practice.getContactPhone()
        );

        Receiver receiver = new Receiver(
                payer.getName(),
                payer.getPayerId()
        );

        BillingProviderLoop billingProvider = new BillingProviderLoop(
                practice.getName(),
                practice.getNpi(),
                practice.getTaxId(),
                practice.getAddress(),
                practice.getCity(),
                practice.getState(),
                practice.getZipCode()
        );

        SubscriberLoop subscriber = new SubscriberLoop(
                mapSubscriberRelationship(insurance.getSubscriberRelationship()),
                insurance.getGroupNumber(),
                insurance.getPolicyType(),
                patient.getLastName(),
                patient.getFirstName(),
                insurance.getMemberId(),
                patient.getAddress(),
                patient.getCity(),
                patient.getState(),
                patient.getZipCode(),
                formatDate(patient.getDateOfBirth()),
                mapGender(patient.getGender()),
                payer.getName(),
                payer.getPayerId()
        );

        List<DiagnosisEntry> diagnosisEntries = diagnoses.stream()
                .sorted(Comparator.comparingInt(EncounterDiagnosis::getRank))
                .map(d -> new DiagnosisEntry(d.getRank(), d.getDiagnosisCode()))
                .toList();

        String dateOfService = encounter.getDateOfService() != null
                ? encounter.getDateOfService().format(YYYYMMDD)
                : "";

        List<ServiceLineLoop> serviceLines = procedures.stream()
                .sorted(Comparator.comparingInt(EncounterProcedure::getLineNumber))
                .map(p -> new ServiceLineLoop(
                        p.getLineNumber(),
                        p.getProcedureCode(),
                        p.getModifiers(),
                        p.getChargeAmount(),
                        p.getUnits(),
                        p.getUnitType(),
                        p.getDiagnosisPointers(),
                        dateOfService
                ))
                .toList();

        BigDecimal totalCharge = procedures.stream()
                .map(EncounterProcedure::getChargeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ClaimLoop claimLoop = new ClaimLoop(
                insurance.getMemberId(),
                totalCharge,
                facility.getPlaceOfServiceCode(),
                diagnosisEntries,
                serviceLines
        );

        return new EDI837Claim(
                envelope,
                functionalGroup,
                transactionHeader,
                submitter,
                receiver,
                billingProvider,
                subscriber,
                claimLoop
        );
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(YYYYMMDD) : "";
    }

    private String mapGender(String gender) {
        if (gender == null) return "U";
        return switch (gender.toUpperCase()) {
            case "M", "MALE"   -> "M";
            case "F", "FEMALE" -> "F";
            default            -> "U";
        };
    }

    private String mapSubscriberRelationship(String relationship) {
        if (relationship == null) return "P";
        return switch (relationship.toLowerCase()) {
            case "self"   -> "P";
            case "spouse" -> "S";
            case "child"  -> "D";
            default       -> "P";
        };
    }
}
