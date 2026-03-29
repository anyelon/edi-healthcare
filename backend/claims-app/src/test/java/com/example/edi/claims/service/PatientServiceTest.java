package com.example.edi.claims.service;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.common.document.Patient;
import com.example.edi.common.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    @Test
    void getAllPatients_returnsMappedResponses() {
        Patient patient = new Patient();
        patient.setId("P1");
        patient.setFirstName("JOHN");
        patient.setLastName("SMITH");
        patient.setDateOfBirth(LocalDate.of(1985, 7, 15));
        patient.setGender("M");
        patient.setAddress("456 OAK AVE");
        patient.setCity("ORLANDO");
        patient.setState("FL");
        patient.setZipCode("32806");
        patient.setPhone("5553334444");

        when(patientRepository.findAll()).thenReturn(List.of(patient));

        List<PatientResponse> result = patientService.getAllPatients();

        assertThat(result).hasSize(1);
        PatientResponse response = result.getFirst();
        assertThat(response.id()).isEqualTo("P1");
        assertThat(response.firstName()).isEqualTo("JOHN");
        assertThat(response.lastName()).isEqualTo("SMITH");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1985, 7, 15));
        assertThat(response.gender()).isEqualTo("M");
    }

    @Test
    void getAllPatients_emptyDatabase_returnsEmptyList() {
        when(patientRepository.findAll()).thenReturn(List.of());

        List<PatientResponse> result = patientService.getAllPatients();

        assertThat(result).isEmpty();
    }
}
