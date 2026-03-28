package com.example.edi.insuranceresponse.service;

import com.example.edi.insuranceresponse.dto.VerificationResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class EDI271Service {

    public VerificationResult parse(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String[] segments = content.split("~");

            String patientId = null;
            String status = null;
            String verificationMessage = "";

            for (String segment : segments) {
                String trimmed = segment.trim();

                if (trimmed.startsWith("NM1*IL")) {
                    String[] elements = trimmed.split("\\*");
                    if (elements.length > 9) {
                        patientId = elements[9];
                    } else {
                        throw new RuntimeException("NM1*IL segment does not contain member ID at element 9");
                    }
                }

                if (status == null && trimmed.startsWith("EB")) {
                    String[] elements = trimmed.split("\\*");
                    if (elements.length > 1) {
                        status = switch (elements[1]) {
                            case "1" -> "ACTIVE";
                            case "6" -> "INACTIVE";
                            case "8" -> "INACTIVE";
                            default -> "UNKNOWN";
                        };
                    }
                }

                if (trimmed.startsWith("MSG")) {
                    String[] elements = trimmed.split("\\*");
                    if (elements.length > 1) {
                        verificationMessage = elements[1];
                    }
                }
            }

            if (patientId == null) {
                throw new RuntimeException("Required NM1*IL segment not found in EDI 271 file");
            }

            if (status == null) {
                throw new RuntimeException("Required EB segment not found in EDI 271 file");
            }

            return new VerificationResult(patientId, status, verificationMessage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read EDI 271 file: " + filePath, e);
        }
    }
}
