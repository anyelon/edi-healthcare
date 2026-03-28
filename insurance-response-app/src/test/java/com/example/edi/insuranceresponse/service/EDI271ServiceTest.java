package com.example.edi.insuranceresponse.service;

import com.example.edi.insuranceresponse.dto.VerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EDI271ServiceTest {

    private EDI271Service edi271Service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        edi271Service = new EDI271Service();
    }

    @Test
    void parse_activePatient_returnsActiveStatus() throws IOException {
        String edi271 = "ISA*00*          *00*          *ZZ*RECEIVERID     *ZZ*SENDERID       *260321*1300*^*00501*000000002*0*P*:~" +
                "GS*HB*RECEIVERID*SENDERID*20260321*1300*1*X*005010X279A1~" +
                "ST*271*0001*005010X279A1~" +
                "BHT*0022*11*TRACE001*20260321*1300~" +
                "HL*1**20*1~" +
                "NM1*PR*2*AETNA*****PI*PAYER01~" +
                "HL*2*1*21*1~" +
                "NM1*1P*2*TEST COMPANY*****XX*1234567890~" +
                "HL*3*2*22*0~" +
                "NM1*IL*1*Doe*John****MI*MEM001~" +
                "DMG*D8*19900115*M~" +
                "EB*1*IND*30**AETNA~" +
                "MSG*PATIENT IS ACTIVE AND ELIGIBLE~" +
                "SE*12*0001~" +
                "GE*1*1~" +
                "IEA*1*000000002~";

        Path file = tempDir.resolve("test271.edi");
        Files.writeString(file, edi271);

        VerificationResult result = edi271Service.parse(file);

        assertEquals("MEM001", result.patientId());
        assertEquals("ACTIVE", result.verificationStatus());
        assertEquals("PATIENT IS ACTIVE AND ELIGIBLE", result.verificationMessage());
    }

    @Test
    void parse_inactivePatient_returnsInactiveStatus() throws IOException {
        String edi271 = "ISA*00*stuff~" +
                "NM1*IL*1*Smith*Jane****MI*MEM002~" +
                "EB*6*IND*30~" +
                "MSG*COVERAGE TERMINATED~" +
                "SE*5*0001~" +
                "IEA*1*000000001~";

        Path file = tempDir.resolve("test271_inactive.edi");
        Files.writeString(file, edi271);

        VerificationResult result = edi271Service.parse(file);

        assertEquals("MEM002", result.patientId());
        assertEquals("INACTIVE", result.verificationStatus());
        assertEquals("COVERAGE TERMINATED", result.verificationMessage());
    }

    @Test
    void parse_code8_returnsInactive() throws IOException {
        String edi271 = "NM1*IL*1*Test*User****MI*MEM003~" +
                "EB*8*IND*30~" +
                "SE*3*0001~";

        Path file = tempDir.resolve("test271_code8.edi");
        Files.writeString(file, edi271);

        VerificationResult result = edi271Service.parse(file);

        assertEquals("MEM003", result.patientId());
        assertEquals("INACTIVE", result.verificationStatus());
        assertEquals("", result.verificationMessage());
    }

    @Test
    void parse_missingNM1IL_throwsException() throws IOException {
        String edi271 = "ISA*00*stuff~EB*1*IND*30~SE*2*0001~";

        Path file = tempDir.resolve("test271_no_nm1.edi");
        Files.writeString(file, edi271);

        assertThrows(RuntimeException.class, () -> edi271Service.parse(file));
    }

    @Test
    void parse_missingEB_throwsException() throws IOException {
        String edi271 = "NM1*IL*1*Doe*John****MI*MEM001~SE*1*0001~";

        Path file = tempDir.resolve("test271_no_eb.edi");
        Files.writeString(file, edi271);

        assertThrows(RuntimeException.class, () -> edi271Service.parse(file));
    }

    @Test
    void parse_noMSGSegment_returnsEmptyMessage() throws IOException {
        String edi271 = "NM1*IL*1*Doe*John****MI*MEM001~" +
                "EB*1*IND*30~" +
                "SE*2*0001~";

        Path file = tempDir.resolve("test271_no_msg.edi");
        Files.writeString(file, edi271);

        VerificationResult result = edi271Service.parse(file);
        assertEquals("", result.verificationMessage());
    }
}
