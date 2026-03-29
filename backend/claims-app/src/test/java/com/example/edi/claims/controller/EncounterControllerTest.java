package com.example.edi.claims.controller;

import com.example.edi.claims.dto.DiagnosisResponse;
import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.claims.dto.ProcedureResponse;
import com.example.edi.claims.service.EncounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EncounterController.class)
class EncounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EncounterService encounterService;

    @Test
    void getEncounters_returns200WithEnrichedList() throws Exception {
        EncounterResponse encounter = new EncounterResponse(
                "ENC1", "P1", "JOHN SMITH", "PROV1", "Sarah Johnson",
                "FAC1", "Main Office", LocalDate.of(2026, 3, 15), "AUTH001",
                List.of(new DiagnosisResponse("J06.9", 1)),
                List.of(new ProcedureResponse("99213", List.of(), new BigDecimal("150.00"), 1))
        );

        when(encounterService.getAllEncounters()).thenReturn(List.of(encounter));

        mockMvc.perform(get("/api/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ENC1"))
                .andExpect(jsonPath("$[0].patientName").value("JOHN SMITH"))
                .andExpect(jsonPath("$[0].providerName").value("Sarah Johnson"))
                .andExpect(jsonPath("$[0].facilityName").value("Main Office"))
                .andExpect(jsonPath("$[0].dateOfService").value("2026-03-15"))
                .andExpect(jsonPath("$[0].diagnoses[0].diagnosisCode").value("J06.9"))
                .andExpect(jsonPath("$[0].diagnoses[0].rank").value(1))
                .andExpect(jsonPath("$[0].procedures[0].procedureCode").value("99213"))
                .andExpect(jsonPath("$[0].procedures[0].chargeAmount").value(150.00));
    }

    @Test
    void getEncounters_emptyDatabase_returns200WithEmptyList() throws Exception {
        when(encounterService.getAllEncounters()).thenReturn(List.of());

        mockMvc.perform(get("/api/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
