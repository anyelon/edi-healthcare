package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.EligibilitySubscriber;
import com.example.edi.insurancerequest.domain.loop.InformationReceiverGroup;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.insurancerequest.dto.EligibilityBundle;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PatientInsurance;
import com.example.edi.common.document.Payer;
import com.example.edi.common.document.Practice;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EDI270Mapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HHmm");

    public EDI270Inquiry map(Practice practice,
                             List<EligibilityBundle> bundles,
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
                "270",
                nowDate,
                nowTime
        );

        String today = LocalDate.now().format(YYYYMMDD);

        // Group bundles by payer ID to produce one InformationSourceGroup per payer
        Map<String, List<EligibilityBundle>> grouped = new LinkedHashMap<>();
        for (EligibilityBundle bundle : bundles) {
            String key = bundle.payer().getPayerId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(bundle);
        }

        List<InformationSourceGroup> sourceGroups = new ArrayList<>();
        for (List<EligibilityBundle> payerBundles : grouped.values()) {
            EligibilityBundle representative = payerBundles.getFirst();
            Payer payer = representative.payer();

            List<EligibilitySubscriber> subscribers = new ArrayList<>();
            for (EligibilityBundle bundle : payerBundles) {
                Patient patient = bundle.patient();
                PatientInsurance ins = bundle.insurance();

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
                        payer.getPayerId()
                );

                subscribers.add(new EligibilitySubscriber(subscriberLoop, today));
            }

            InformationReceiverGroup receiverGroup = new InformationReceiverGroup(
                    practice.getName(),
                    practice.getNpi(),
                    practice.getTaxId(),
                    subscribers
            );

            sourceGroups.add(new InformationSourceGroup(
                    payer.getName(),
                    payer.getPayerId(),
                    receiverGroup
            ));
        }

        return new EDI270Inquiry(envelope, functionalGroup, transactionHeader, sourceGroups);
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
