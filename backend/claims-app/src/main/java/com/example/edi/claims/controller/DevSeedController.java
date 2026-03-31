package com.example.edi.claims.controller;

import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevSeedController {

    private final PracticeRepository practiceRepository;
    private final ProviderRepository providerRepository;
    private final PayerRepository payerRepository;
    private final FacilityRepository facilityRepository;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final EncounterRepository encounterRepository;
    private final EncounterDiagnosisRepository encounterDiagnosisRepository;
    private final EncounterProcedureRepository encounterProcedureRepository;

    public DevSeedController(
            PracticeRepository practiceRepository,
            ProviderRepository providerRepository,
            PayerRepository payerRepository,
            FacilityRepository facilityRepository,
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            EncounterRepository encounterRepository,
            EncounterDiagnosisRepository encounterDiagnosisRepository,
            EncounterProcedureRepository encounterProcedureRepository) {
        this.practiceRepository = practiceRepository;
        this.providerRepository = providerRepository;
        this.payerRepository = payerRepository;
        this.facilityRepository = facilityRepository;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.encounterRepository = encounterRepository;
        this.encounterDiagnosisRepository = encounterDiagnosisRepository;
        this.encounterProcedureRepository = encounterProcedureRepository;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        if (practiceRepository.count() > 0) {
            return Map.of("message", "Data already seeded");
        }

        // Practice
        Practice practice = new Practice();
        practice.setName("SUNSHINE HEALTH CLINIC");
        practice.setNpi("1234567890");
        practice.setTaxId("591234567");
        practice.setTaxonomyCode("207Q00000X");
        practice.setAddress("100 MEDICAL PLAZA DR");
        practice.setCity("ORLANDO");
        practice.setState("FL");
        practice.setZipCode("32801");
        practice.setContactPhone("5551234567");
        practice = practiceRepository.save(practice);

        // Providers
        Provider provider1 = new Provider();
        provider1.setFirstName("Sarah");
        provider1.setLastName("Johnson");
        provider1.setNpi("9876543210");
        provider1.setTaxonomyCode("207R00000X");
        provider1.setPracticeId(practice.getId());
        provider1 = providerRepository.save(provider1);

        Provider provider2 = new Provider();
        provider2.setFirstName("Michael");
        provider2.setLastName("Chen");
        provider2.setNpi("5678901234");
        provider2.setTaxonomyCode("208D00000X");
        provider2.setPracticeId(practice.getId());
        provider2 = providerRepository.save(provider2);

        // Payer
        Payer payer = new Payer();
        payer.setName("BLUE CROSS BLUE SHIELD");
        payer.setPayerId("BCBS12345");
        payer.setAddress("PO BOX 105187");
        payer.setCity("ATLANTA");
        payer.setState("GA");
        payer.setZipCode("30348");
        payer = payerRepository.save(payer);

        // Facilities
        Facility facility1 = new Facility();
        facility1.setName("Main Office");
        facility1.setPracticeId(practice.getId());
        facility1.setPlaceOfServiceCode("11");
        facility1.setAddress("100 MEDICAL PLAZA DR");
        facility1.setCity("ORLANDO");
        facility1.setState("FL");
        facility1.setZipCode("32801");
        facility1 = facilityRepository.save(facility1);

        Facility facility2 = new Facility();
        facility2.setName("Outpatient Center");
        facility2.setPracticeId(practice.getId());
        facility2.setPlaceOfServiceCode("22");
        facility2.setAddress("200 HOSPITAL BLVD");
        facility2.setCity("ORLANDO");
        facility2.setState("FL");
        facility2.setZipCode("32801");
        facility2 = facilityRepository.save(facility2);

        Facility facility3 = new Facility();
        facility3.setName("City Hospital ER");
        facility3.setPracticeId(practice.getId());
        facility3.setPlaceOfServiceCode("23");
        facility3.setAddress("300 EMERGENCY WAY");
        facility3.setCity("ORLANDO");
        facility3.setState("FL");
        facility3.setZipCode("32801");
        facility3 = facilityRepository.save(facility3);

        // Patients
        Patient patient1 = new Patient();
        patient1.setFirstName("JOHN");
        patient1.setLastName("SMITH");
        patient1.setGender("M");
        patient1.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient1.setAddress("456 OAK AVENUE");
        patient1.setCity("ORLANDO");
        patient1.setState("FL");
        patient1.setZipCode("32806");
        patient1.setPhone("5553334444");
        patient1 = patientRepository.save(patient1);

        Patient patient2 = new Patient();
        patient2.setFirstName("JANE");
        patient2.setLastName("DOE");
        patient2.setGender("F");
        patient2.setDateOfBirth(LocalDate.of(1990, 3, 22));
        patient2.setAddress("789 PINE STREET");
        patient2.setCity("ORLANDO");
        patient2.setState("FL");
        patient2.setZipCode("32807");
        patient2.setPhone("5555556666");
        patient2 = patientRepository.save(patient2);

        // Patient Insurance
        PatientInsurance insurance1 = new PatientInsurance();
        insurance1.setPatientId(patient1.getId());
        insurance1.setPayerId(payer.getId());
        insurance1.setSubscriberRelationship("self");
        insurance1.setPolicyType("MC");
        insurance1.setEffectiveDate(LocalDate.of(2025, 1, 1));
        insurance1.setMemberId("MEM987654321");
        insurance1.setGroupNumber("GRP100234");
        insurance1 = patientInsuranceRepository.save(insurance1);

        PatientInsurance insurance2 = new PatientInsurance();
        insurance2.setPatientId(patient2.getId());
        insurance2.setPayerId(payer.getId());
        insurance2.setSubscriberRelationship("self");
        insurance2.setPolicyType("MC");
        insurance2.setEffectiveDate(LocalDate.of(2025, 1, 1));
        insurance2.setMemberId("MEM123456789");
        insurance2.setGroupNumber("GRP100234");
        insurance2 = patientInsuranceRepository.save(insurance2);

        // Encounter 1
        Encounter encounter1 = new Encounter();
        encounter1.setPatientId(patient1.getId());
        encounter1.setProviderId(provider1.getId());
        encounter1.setPracticeId(practice.getId());
        encounter1.setFacilityId(facility1.getId());
        encounter1.setDateOfService(LocalDate.of(2026, 3, 15));
        encounter1.setRequestedProcedures(List.of(
                new RequestedProcedure("99213", "Acute upper respiratory infection follow-up"),
                new RequestedProcedure("87880", "Rapid strep test for persistent symptoms")
        ));
        encounter1 = encounterRepository.save(encounter1);

        EncounterDiagnosis diag1a = new EncounterDiagnosis();
        diag1a.setEncounterId(encounter1.getId());
        diag1a.setDiagnosisCode("J06.9");
        diag1a.setRank(1);
        encounterDiagnosisRepository.save(diag1a);

        EncounterDiagnosis diag1b = new EncounterDiagnosis();
        diag1b.setEncounterId(encounter1.getId());
        diag1b.setDiagnosisCode("R05.9");
        diag1b.setRank(2);
        encounterDiagnosisRepository.save(diag1b);

        EncounterProcedure proc1a = new EncounterProcedure();
        proc1a.setEncounterId(encounter1.getId());
        proc1a.setLineNumber(1);
        proc1a.setProcedureCode("99213");
        proc1a.setModifiers(List.of());
        proc1a.setChargeAmount(new BigDecimal("150.00"));
        proc1a.setUnits(1);
        proc1a.setUnitType("UN");
        proc1a.setDiagnosisPointers(List.of(1, 2));
        encounterProcedureRepository.save(proc1a);

        EncounterProcedure proc1b = new EncounterProcedure();
        proc1b.setEncounterId(encounter1.getId());
        proc1b.setLineNumber(2);
        proc1b.setProcedureCode("87880");
        proc1b.setModifiers(List.of());
        proc1b.setChargeAmount(new BigDecimal("100.00"));
        proc1b.setUnits(1);
        proc1b.setUnitType("UN");
        proc1b.setDiagnosisPointers(List.of(1));
        encounterProcedureRepository.save(proc1b);

        // Encounter 2
        Encounter encounter2 = new Encounter();
        encounter2.setPatientId(patient2.getId());
        encounter2.setProviderId(provider2.getId());
        encounter2.setPracticeId(practice.getId());
        encounter2.setFacilityId(facility2.getId());
        encounter2.setDateOfService(LocalDate.of(2026, 3, 16));
        encounter2.setRequestedProcedures(List.of(
                new RequestedProcedure("99214", "Chronic low back pain evaluation"),
                new RequestedProcedure("97140", "Manual therapy for lumbar spine dysfunction")
        ));
        encounter2 = encounterRepository.save(encounter2);

        EncounterDiagnosis diag2a = new EncounterDiagnosis();
        diag2a.setEncounterId(encounter2.getId());
        diag2a.setDiagnosisCode("M54.5");
        diag2a.setRank(1);
        encounterDiagnosisRepository.save(diag2a);

        EncounterProcedure proc2a = new EncounterProcedure();
        proc2a.setEncounterId(encounter2.getId());
        proc2a.setLineNumber(1);
        proc2a.setProcedureCode("99214");
        proc2a.setModifiers(List.of("25"));
        proc2a.setChargeAmount(new BigDecimal("200.00"));
        proc2a.setUnits(1);
        proc2a.setUnitType("UN");
        proc2a.setDiagnosisPointers(List.of(1));
        encounterProcedureRepository.save(proc2a);

        EncounterProcedure proc2b = new EncounterProcedure();
        proc2b.setEncounterId(encounter2.getId());
        proc2b.setLineNumber(2);
        proc2b.setProcedureCode("97140");
        proc2b.setModifiers(List.of());
        proc2b.setChargeAmount(new BigDecimal("75.00"));
        proc2b.setUnits(2);
        proc2b.setUnitType("UN");
        proc2b.setDiagnosisPointers(List.of(1));
        encounterProcedureRepository.save(proc2b);

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("practiceId", practice.getId());
        result.put("providerIds", List.of(provider1.getId(), provider2.getId()));
        result.put("payerId", payer.getId());
        result.put("facilityIds", List.of(facility1.getId(), facility2.getId(), facility3.getId()));
        result.put("patientIds", List.of(patient1.getId(), patient2.getId()));
        result.put("insuranceIds", List.of(insurance1.getId(), insurance2.getId()));
        result.put("encounterIds", List.of(encounter1.getId(), encounter2.getId()));
        return result;
    }
}
