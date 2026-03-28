package com.example.edi.insurancerequest.service;

import com.example.edi.common.document.Company;
import com.example.edi.common.document.Patient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EDI270Service {

    private static final String SEGMENT_TERMINATOR = "~";
    private static final String ELEMENT_SEPARATOR = "*";
    private static final String SUB_ELEMENT_SEPARATOR = ":";

    public String to270(Company company, Patient patient) {
        StringBuilder sb = new StringBuilder();

        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String longDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmm"));
        String controlNumber = String.format("%09d", 1);
        String groupControlNumber = "1";
        String transactionControlNumber = "0001";
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // ISA - Interchange Control Header
        sb.append("ISA").append(ELEMENT_SEPARATOR)
                .append(pad("00", 2)).append(ELEMENT_SEPARATOR)
                .append(pad("", 10)).append(ELEMENT_SEPARATOR)
                .append(pad("00", 2)).append(ELEMENT_SEPARATOR)
                .append(pad("", 10)).append(ELEMENT_SEPARATOR)
                .append(pad("ZZ", 2)).append(ELEMENT_SEPARATOR)
                .append(pad(company.getNpi(), 15)).append(ELEMENT_SEPARATOR)
                .append(pad("ZZ", 2)).append(ELEMENT_SEPARATOR)
                .append(pad(patient.getInsurancePayerId(), 15)).append(ELEMENT_SEPARATOR)
                .append(date).append(ELEMENT_SEPARATOR)
                .append(time).append(ELEMENT_SEPARATOR)
                .append("^").append(ELEMENT_SEPARATOR)
                .append("00501").append(ELEMENT_SEPARATOR)
                .append(controlNumber).append(ELEMENT_SEPARATOR)
                .append("0").append(ELEMENT_SEPARATOR)
                .append("T").append(ELEMENT_SEPARATOR)
                .append(SUB_ELEMENT_SEPARATOR)
                .append(SEGMENT_TERMINATOR);

        // GS - Functional Group Header
        sb.append("GS").append(ELEMENT_SEPARATOR)
                .append("HS").append(ELEMENT_SEPARATOR)
                .append(company.getNpi()).append(ELEMENT_SEPARATOR)
                .append(patient.getInsurancePayerId()).append(ELEMENT_SEPARATOR)
                .append(longDate).append(ELEMENT_SEPARATOR)
                .append(time).append(ELEMENT_SEPARATOR)
                .append(groupControlNumber).append(ELEMENT_SEPARATOR)
                .append("X").append(ELEMENT_SEPARATOR)
                .append("005010X279A1")
                .append(SEGMENT_TERMINATOR);

        // ST - Transaction Set Header
        sb.append("ST").append(ELEMENT_SEPARATOR)
                .append("270").append(ELEMENT_SEPARATOR)
                .append(transactionControlNumber).append(ELEMENT_SEPARATOR)
                .append("005010X279A1")
                .append(SEGMENT_TERMINATOR);

        // BHT - Beginning of Hierarchical Transaction
        sb.append("BHT").append(ELEMENT_SEPARATOR)
                .append("0022").append(ELEMENT_SEPARATOR)
                .append("13").append(ELEMENT_SEPARATOR)
                .append(transactionControlNumber).append(ELEMENT_SEPARATOR)
                .append(longDate).append(ELEMENT_SEPARATOR)
                .append(time)
                .append(SEGMENT_TERMINATOR);

        int segmentCount = 2; // ST and BHT

        // HL*1 - Information Source (Payer)
        sb.append("HL").append(ELEMENT_SEPARATOR)
                .append("1").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("20").append(ELEMENT_SEPARATOR)
                .append("1")
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // NM1 - Payer Name
        sb.append("NM1").append(ELEMENT_SEPARATOR)
                .append("PR").append(ELEMENT_SEPARATOR)
                .append("2").append(ELEMENT_SEPARATOR)
                .append(patient.getInsurancePayerName()).append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("PI").append(ELEMENT_SEPARATOR)
                .append(patient.getInsurancePayerId())
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // HL*2 - Information Receiver (Provider)
        sb.append("HL").append(ELEMENT_SEPARATOR)
                .append("2").append(ELEMENT_SEPARATOR)
                .append("1").append(ELEMENT_SEPARATOR)
                .append("21").append(ELEMENT_SEPARATOR)
                .append("1")
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // NM1 - Provider Name
        sb.append("NM1").append(ELEMENT_SEPARATOR)
                .append("1P").append(ELEMENT_SEPARATOR)
                .append("2").append(ELEMENT_SEPARATOR)
                .append(company.getName()).append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("XX").append(ELEMENT_SEPARATOR)
                .append(company.getNpi())
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // REF - Provider Tax ID
        sb.append("REF").append(ELEMENT_SEPARATOR)
                .append("EI").append(ELEMENT_SEPARATOR)
                .append(company.getTaxId())
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // HL*3 - Subscriber
        sb.append("HL").append(ELEMENT_SEPARATOR)
                .append("3").append(ELEMENT_SEPARATOR)
                .append("2").append(ELEMENT_SEPARATOR)
                .append("22").append(ELEMENT_SEPARATOR)
                .append("0")
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // NM1 - Subscriber Name
        sb.append("NM1").append(ELEMENT_SEPARATOR)
                .append("IL").append(ELEMENT_SEPARATOR)
                .append("1").append(ELEMENT_SEPARATOR)
                .append(patient.getLastName()).append(ELEMENT_SEPARATOR)
                .append(patient.getFirstName()).append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("").append(ELEMENT_SEPARATOR)
                .append("MI").append(ELEMENT_SEPARATOR)
                .append(patient.getMemberId())
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // DMG - Subscriber Demographics
        String dob = patient.getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String genderCode = mapGender(patient.getGender());
        sb.append("DMG").append(ELEMENT_SEPARATOR)
                .append("D8").append(ELEMENT_SEPARATOR)
                .append(dob).append(ELEMENT_SEPARATOR)
                .append(genderCode)
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // DTP - Date Range
        sb.append("DTP").append(ELEMENT_SEPARATOR)
                .append("291").append(ELEMENT_SEPARATOR)
                .append("RD8").append(ELEMENT_SEPARATOR)
                .append(today).append("-").append(today)
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // EQ - Eligibility/Benefit Inquiry
        sb.append("EQ").append(ELEMENT_SEPARATOR)
                .append("30")
                .append(SEGMENT_TERMINATOR);
        segmentCount++;

        // SE - Transaction Set Trailer
        segmentCount++; // count the SE segment itself
        sb.append("SE").append(ELEMENT_SEPARATOR)
                .append(segmentCount).append(ELEMENT_SEPARATOR)
                .append(transactionControlNumber)
                .append(SEGMENT_TERMINATOR);

        // GE - Functional Group Trailer
        sb.append("GE").append(ELEMENT_SEPARATOR)
                .append("1").append(ELEMENT_SEPARATOR)
                .append(groupControlNumber)
                .append(SEGMENT_TERMINATOR);

        // IEA - Interchange Control Trailer
        sb.append("IEA").append(ELEMENT_SEPARATOR)
                .append("1").append(ELEMENT_SEPARATOR)
                .append(controlNumber)
                .append(SEGMENT_TERMINATOR);

        return sb.toString();
    }

    private String pad(String value, int length) {
        if (value == null) {
            value = "";
        }
        return String.format("%-" + length + "s", value);
    }

    private String mapGender(String gender) {
        if (gender == null) return "U";
        return switch (gender.toUpperCase()) {
            case "MALE", "M" -> "M";
            case "FEMALE", "F" -> "F";
            default -> "U";
        };
    }
}
