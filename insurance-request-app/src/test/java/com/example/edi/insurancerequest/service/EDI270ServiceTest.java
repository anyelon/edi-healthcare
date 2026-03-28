package com.example.edi.insurancerequest.service;

import com.example.edi.common.document.Company;
import com.example.edi.common.document.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EDI270ServiceTest {

    private EDI270Service edi270Service;

    @BeforeEach
    void setUp() {
        edi270Service = new EDI270Service();
    }

    @Test
    void to270_generatesValidEdiString() {
        Company company = new Company();
        company.setName("TEST COMPANY");
        company.setNpi("1234567890");
        company.setTaxId("123456789");

        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        patient.setGender("M");
        patient.setMemberId("MEM001");
        patient.setInsurancePayerId("PAYER01");
        patient.setInsurancePayerName("AETNA");

        String result = edi270Service.to270(company, patient);

        assertNotNull(result);
        assertTrue(result.startsWith("ISA*"));
        assertTrue(result.contains("ST*270*0001*005010X279A1~"));
        assertTrue(result.contains("BHT*0022*13*"));
        assertTrue(result.contains("NM1*PR*2*AETNA*****PI*PAYER01~"));
        assertTrue(result.contains("NM1*1P*2*TEST COMPANY*****XX*1234567890~"));
        assertTrue(result.contains("REF*EI*123456789~"));
        assertTrue(result.contains("NM1*IL*1*Doe*John****MI*MEM001~"));
        assertTrue(result.contains("DMG*D8*19900115*M~"));
        assertTrue(result.contains("EQ*30~"));
        assertTrue(result.contains("GE*1*"));
        assertTrue(result.contains("IEA*1*"));
    }

    @Test
    void to270_femaleGender() {
        Company company = new Company();
        company.setName("TEST COMPANY");
        company.setNpi("1234567890");
        company.setTaxId("123456789");

        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Smith");
        patient.setDateOfBirth(LocalDate.of(1985, 5, 20));
        patient.setGender("F");
        patient.setMemberId("MEM002");
        patient.setInsurancePayerId("PAYER02");
        patient.setInsurancePayerName("BCBS");

        String result = edi270Service.to270(company, patient);

        assertTrue(result.contains("DMG*D8*19850520*F~"));
        assertTrue(result.contains("NM1*IL*1*Smith*Jane****MI*MEM002~"));
    }
}
