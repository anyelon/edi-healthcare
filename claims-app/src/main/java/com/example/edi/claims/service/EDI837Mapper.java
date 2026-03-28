package com.example.edi.claims.service;

import com.example.edi.claims.config.InterchangeProperties;
import com.example.edi.claims.domain.loop.ClaimLoop;
import com.example.edi.claims.domain.loop.DiagnosisEntry;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.ServiceLineLoop;
import com.example.edi.claims.domain.loop.SubscriberGroup;
import com.example.edi.claims.dto.EncounterBundle;
import com.example.edi.common.document.*;
import com.example.edi.common.edi.loop.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EDI837Mapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HHmm");

    public EDI837Claim map(Practice practice,
                           List<EncounterBundle> bundles,
                           InterchangeProperties props) {

        LocalDateTime now = LocalDateTime.now();
        String nowDate   = now.format(YYYYMMDD);
        String nowYYMMDD = now.format(YYMMDD);
        String nowTime   = now.format(HHMM);

        long millis        = System.currentTimeMillis();
        String controlNum9 = String.format("%09d", millis % 1_000_000_000L);
        String controlNum5 = String.format("%05d", millis % 100_000L);

        // Use the first bundle's payer for the receiver
        EncounterBundle firstBundle = bundles.getFirst();

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
                firstBundle.payer().getName(),
                firstBundle.payer().getPayerId()
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

        // Group bundles by subscriber key (patientId + memberId)
        Map<String, List<EncounterBundle>> grouped = new LinkedHashMap<>();
        for (EncounterBundle bundle : bundles) {
            String key = bundle.patient().getId() + "|" + bundle.insurance().getMemberId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(bundle);
        }

        List<SubscriberGroup> subscriberGroups = new ArrayList<>();
        for (List<EncounterBundle> groupBundles : grouped.values()) {
            EncounterBundle representative = groupBundles.getFirst();
            Patient patient = representative.patient();
            PatientInsurance insurance = representative.insurance();
            Payer payer = representative.payer();

            SubscriberLoop subscriberLoop = new SubscriberLoop(
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

            List<ClaimLoop> claims = new ArrayList<>();
            for (EncounterBundle bundle : groupBundles) {
                claims.add(buildClaimLoop(bundle));
            }

            subscriberGroups.add(new SubscriberGroup(subscriberLoop, claims));
        }

        return new EDI837Claim(
                envelope,
                functionalGroup,
                transactionHeader,
                submitter,
                receiver,
                billingProvider,
                subscriberGroups
        );
    }

    private ClaimLoop buildClaimLoop(EncounterBundle bundle) {
        List<DiagnosisEntry> diagnosisEntries = bundle.diagnoses().stream()
                .sorted(Comparator.comparingInt(EncounterDiagnosis::getRank))
                .map(d -> new DiagnosisEntry(d.getRank(), d.getDiagnosisCode()))
                .toList();

        String dateOfService = bundle.encounter().getDateOfService() != null
                ? bundle.encounter().getDateOfService().format(YYYYMMDD)
                : "";

        List<ServiceLineLoop> serviceLines = bundle.procedures().stream()
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

        BigDecimal totalCharge = bundle.procedures().stream()
                .map(EncounterProcedure::getChargeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClaimLoop(
                bundle.insurance().getMemberId(),
                totalCharge,
                bundle.facility().getPlaceOfServiceCode(),
                diagnosisEntries,
                serviceLines
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
