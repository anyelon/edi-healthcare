package com.example.edi.claims.controller;

import com.example.edi.claims.dto.PatientResponse;
import com.example.edi.claims.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    void getPatients_returns200WithPatientList() throws Exception {
        PatientResponse patient = new PatientResponse(
                "P1", "JOHN", "SMITH", LocalDate.of(1985, 7, 15), "M",
                "456 OAK AVE", "ORLANDO", "FL", "32806", "5553334444");

        when(patientService.getAllPatients()).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("P1"))
                .andExpect(jsonPath("$[0].firstName").value("JOHN"))
                .andExpect(jsonPath("$[0].lastName").value("SMITH"))
                .andExpect(jsonPath("$[0].dateOfBirth").value("1985-07-15"))
                .andExpect(jsonPath("$[0].gender").value("M"));
    }

    @Test
    void getPatients_emptyDatabase_returns200WithEmptyList() throws Exception {
        when(patientService.getAllPatients()).thenReturn(List.of());

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
