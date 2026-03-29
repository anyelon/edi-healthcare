package com.example.edi.claims.service;

import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.common.document.*;
import com.example.edi.common.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private EncounterDiagnosisRepository encounterDiagnosisRepository;

    @Mock
    private EncounterProcedureRepository encounterProcedureRepository;

    @InjectMocks
    private EncounterService encounterService;

    @Test
    void getAllEncounters_returnsEnrichedResponses() {
        Encounter encounter = new Encounter();
        encounter.setId("ENC1");
        encounter.setPatientId("P1");
        encounter.setProviderId("PROV1");
        encounter.setFacilityId("FAC1");
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));
        encounter.setAuthorizationNumber("AUTH001");

        Patient patient = new Patient();
        patient.setId("P1");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");

        Provider provider = new Provider();
        provider.setId("PROV1");
        provider.setFirstName("Sarah");
        provider.setLastName("Johnson");

        Facility facility = new Facility();
        facility.setId("FAC1");
        facility.setName("Main Office");

        EncounterDiagnosis diagnosis = new EncounterDiagnosis();
        diagnosis.setEncounterId("ENC1");
        diagnosis.setDiagnosisCode("J06.9");
        diagnosis.setRank(1);

        EncounterProcedure procedure = new EncounterProcedure();
        procedure.setEncounterId("ENC1");
        procedure.setProcedureCode("99213");
        procedure.setModifiers(List.of());
        procedure.setChargeAmount(new BigDecimal("150.00"));
        procedure.setUnits(1);

        when(encounterRepository.findAll()).thenReturn(List.of(encounter));
        when(patientRepository.findAllById(List.of("P1"))).thenReturn(List.of(patient));
        when(providerRepository.findAllById(List.of("PROV1"))).thenReturn(List.of(provider));
        when(facilityRepository.findAllById(List.of("FAC1"))).thenReturn(List.of(facility));
        when(encounterDiagnosisRepository.findAll()).thenReturn(List.of(diagnosis));
        when(encounterProcedureRepository.findAll()).thenReturn(List.of(procedure));

        List<EncounterResponse> result = encounterService.getAllEncounters();

        assertThat(result).hasSize(1);
        EncounterResponse response = result.getFirst();
        assertThat(response.id()).isEqualTo("ENC1");
        assertThat(response.patientName()).isEqualTo("JOHN SMITH");
        assertThat(response.providerName()).isEqualTo("Sarah Johnson");
        assertThat(response.facilityName()).isEqualTo("Main Office");
        assertThat(response.dateOfService()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(response.authorizationNumber()).isEqualTo("AUTH001");
        assertThat(response.diagnoses()).hasSize(1);
        assertThat(response.diagnoses().getFirst().diagnosisCode()).isEqualTo("J06.9");
        assertThat(response.procedures()).hasSize(1);
        assertThat(response.procedures().getFirst().procedureCode()).isEqualTo("99213");
    }

    @Test
    void getAllEncounters_emptyDatabase_returnsEmptyList() {
        when(encounterRepository.findAll()).thenReturn(List.of());

        List<EncounterResponse> result = encounterService.getAllEncounters();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllEncounters_missingRelatedEntity_usesUnknownFallback() {
        Encounter encounter = new Encounter();
        encounter.setId("ENC1");
        encounter.setPatientId("P1");
        encounter.setProviderId("PROV_MISSING");
        encounter.setFacilityId("FAC_MISSING");
        encounter.setDateOfService(LocalDate.of(2026, 3, 15));

        Patient patient = new Patient();
        patient.setId("P1");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");

        when(encounterRepository.findAll()).thenReturn(List.of(encounter));
        when(patientRepository.findAllById(List.of("P1"))).thenReturn(List.of(patient));
        when(providerRepository.findAllById(List.of("PROV_MISSING"))).thenReturn(List.of());
        when(facilityRepository.findAllById(List.of("FAC_MISSING"))).thenReturn(List.of());
        when(encounterDiagnosisRepository.findAll()).thenReturn(List.of());
        when(encounterProcedureRepository.findAll()).thenReturn(List.of());

        List<EncounterResponse> result = encounterService.getAllEncounters();

        assertThat(result).hasSize(1);
        EncounterResponse response = result.getFirst();
        assertThat(response.providerName()).isEqualTo("Unknown");
        assertThat(response.facilityName()).isEqualTo("Unknown");
        assertThat(response.patientName()).isEqualTo("JOHN SMITH");
    }
}
