package com.example.edi.priorauth.service;

import com.example.edi.common.document.EncounterProcedure;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.config.InterchangeProperties;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.ServiceReviewInfo;
import com.example.edi.priorauth.dto.PriorAuthBundle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EDI278Mapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HHmm");

    public EDI278Request map(List<PriorAuthBundle> bundles, InterchangeProperties props) {
        LocalDateTime now = LocalDateTime.now();
        String nowDate   = now.format(YYYYMMDD);
        String nowYYMMDD = now.format(YYMMDD);
        String nowTime   = now.format(HHMM);

        long millis        = System.currentTimeMillis();
        String controlNum9 = String.format("%09d", millis % 1_000_000_000L);
        String controlNum5 = String.format("%05d", millis % 100_000L);

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                props.senderIdQualifier(), props.senderId(),
                props.receiverIdQualifier(), props.receiverId(),
                nowYYMMDD, nowTime, controlNum9,
                props.ackRequested(), props.usageIndicator());

        FunctionalGroup functionalGroup = new FunctionalGroup(
                props.senderId(), props.receiverId(),
                nowDate, nowTime, controlNum5);

        TransactionHeader transactionHeader = new TransactionHeader(
                controlNum5, "278", nowDate, nowTime);

        PriorAuthBundle first = bundles.getFirst();
        String payerName = first.payer().getName();
        String payerId   = first.payer().getPayerId();
        String providerName = first.practice().getName();
        String providerNpi  = first.practice().getNpi();
        String providerTaxId = first.practice().getTaxId();

        List<EDI278Request.SubscriberReviewGroup> subscriberGroups = new ArrayList<>();
        for (PriorAuthBundle bundle : bundles) {
            Patient patient = bundle.patient();
            PatientInsurance ins = bundle.insurance();
            Payer payer = bundle.payer();

            SubscriberLoop subscriberLoop = new SubscriberLoop(
                    mapSubscriberRelationship(ins.getSubscriberRelationship()),
                    ins.getGroupNumber(),
                    ins.getPolicyType(),
                    patient.getLastName(),
                    patient.getFirstName(),
                    ins.getMemberId(),
                    patient.getAddress(),
                    patient.getCity(),
                    patient.getState(),
                    patient.getZipCode(),
                    formatDate(patient.getDateOfBirth()),
                    mapGender(patient.getGender()),
                    payer.getName(),
                    payer.getPayerId());

            String serviceDate = formatDate(bundle.encounter().getDateOfService());

            List<ServiceReviewInfo> services = bundle.authProcedures().stream()
                    .map(ep -> new ServiceReviewInfo(
                            ep.getProcedureCode(), ep.getClinicalReason(), serviceDate))
                    .toList();

            subscriberGroups.add(new EDI278Request.SubscriberReviewGroup(
                    subscriberLoop, bundle.encounter().getId(), services));
        }

        return new EDI278Request(envelope, functionalGroup, transactionHeader,
                payerName, payerId, providerName, providerNpi, providerTaxId,
                subscriberGroups);
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
