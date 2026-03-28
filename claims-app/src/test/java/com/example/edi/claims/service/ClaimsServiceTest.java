package com.example.edi.claims.service;

import com.example.edi.claims.config.InterchangeProperties;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.dto.EncounterBundle;
import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimsServiceTest {

    @Mock private EncounterRepository encounterRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PatientInsuranceRepository patientInsuranceRepository;
    @Mock private PayerRepository payerRepository;
    @Mock private EncounterDiagnosisRepository encounterDiagnosisRepository;
    @Mock private EncounterProcedureRepository encounterProcedureRepository;
    @Mock private PracticeRepository practiceRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private FacilityRepository facilityRepository;
    @Mock private EDI837Mapper edi837Mapper;
    @Mock private EDI837Generator edi837Generator;
    @Mock private InterchangeProperties interchangeProperties;

    @InjectMocks private ClaimsService claimsService;

    @Test
    void generateClaim_success() {
        String encounterId = "ENC001";

        Encounter encounter = new Encounter();
        encounter.setId(encounterId);
        encounter.setPatientId("P001");
        encounter.setPracticeId("PRAC001");
        encounter.setProviderId("PROV001");
        encounter.setFacilityId("FAC001");

        Patient patient = new Patient();
        patient.setId("P001");

        PatientInsurance insurance = new PatientInsurance();
        insurance.setPayerId("PAY001");

        Payer payer = new Payer();
        Practice practice = new Practice();
        Provider provider = new Provider();
        Facility facility = new Facility();

        EDI837Claim claim = mock(EDI837Claim.class);

        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(encounter));
        when(patientRepository.findById("P001")).thenReturn(Optional.of(patient));
        when(patientInsuranceRepository.findByPatientIdAndTerminationDateIsNull("P001")).thenReturn(Optional.of(insurance));
        when(payerRepository.findById("PAY001")).thenReturn(Optional.of(payer));
        when(encounterDiagnosisRepository.findByEncounterIdOrderByRankAsc(encounterId)).thenReturn(List.of());
        when(encounterProcedureRepository.findByEncounterIdOrderByLineNumberAsc(encounterId)).thenReturn(List.of());
        when(practiceRepository.findById("PRAC001")).thenReturn(Optional.of(practice));
        when(facilityRepository.findById("FAC001")).thenReturn(Optional.of(facility));

        List<EncounterBundle> expectedBundles = List.of(
                new EncounterBundle(patient, insurance, payer, encounter, List.of(), List.of(), facility)
        );
        when(edi837Mapper.map(eq(practice), eq(expectedBundles), eq(interchangeProperties)))
                .thenReturn(claim);
        when(edi837Generator.generate(claim)).thenReturn("EDI_CONTENT");

        String result = claimsService.generateClaim(List.of(encounterId));

        assertEquals("EDI_CONTENT", result);
        verify(edi837Generator).generate(claim);
    }

    @Test
    void generateClaim_encounterNotFound_throwsException() {
        when(encounterRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> claimsService.generateClaim(List.of("ENC001")));
    }

    @Test
    void generateClaim_patientNotFound_throwsException() {
        Encounter encounter = new Encounter();
        encounter.setPatientId("P001");
        when(encounterRepository.findById(anyString())).thenReturn(Optional.of(encounter));
        when(patientRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> claimsService.generateClaim(List.of("ENC001")));
    }

    @Test
    void generateClaim_activeInsuranceNotFound_throwsException() {
        Encounter encounter = new Encounter();
        encounter.setPatientId("P001");
        Patient patient = new Patient();
        when(encounterRepository.findById(anyString())).thenReturn(Optional.of(encounter));
        when(patientRepository.findById(anyString())).thenReturn(Optional.of(patient));
        when(patientInsuranceRepository.findByPatientIdAndTerminationDateIsNull(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> claimsService.generateClaim(List.of("ENC001")));
    }
}
