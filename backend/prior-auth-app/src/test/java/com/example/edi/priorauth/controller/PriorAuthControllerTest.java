package com.example.edi.priorauth.controller;

import com.example.edi.common.exception.EncounterNotFoundException;
import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.service.PriorAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PriorAuthController.class)
class PriorAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriorAuthService priorAuthService;

    @Test
    void generatePriorAuth_validRequest_returns200() throws Exception {
        when(priorAuthService.generatePriorAuth(List.of("ENC001")))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"encounterIds\": [\"ENC001\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=278_prior_auth.edi"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    void generatePriorAuth_missingEncounterIds_returns400() throws Exception {
        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePriorAuth_emptyEncounterIds_returns400() throws Exception {
        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"encounterIds\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePriorAuth_encounterNotFound_returns404() throws Exception {
        when(priorAuthService.generatePriorAuth(List.of("ENC001")))
                .thenThrow(new EncounterNotFoundException("ENC001"));

        mockMvc.perform(post("/api/prior-auth/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"encounterIds\": [\"ENC001\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.entityType").value("Encounter"))
                .andExpect(jsonPath("$.entityId").value("ENC001"));
    }

    @Test
    void processResponse_validFile_returns200() throws Exception {
        InterchangeEnvelope env = new InterchangeEnvelope(
                "ZZ", "S", "ZZ", "R", "260415", "1200", "000000001", "0", "T");
        FunctionalGroup fg = new FunctionalGroup("S", "R", "20260415", "1200", "00001");
        TransactionHeader th = new TransactionHeader("00001", "278", "20260415", "1200");
        EDI278Response response = new EDI278Response(env, fg, th,
                "Test Payer", "PAY001", "John", "Doe", "MEM001",
                List.of(new AuthorizationDecision("CERTIFIED", "AUTH123", "ENC001")));

        when(priorAuthService.processResponse(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "278_response.edi",
                "text/plain", "ISA*00*...".getBytes());

        mockMvc.perform(multipart("/api/prior-auth/response").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payerName").value("Test Payer"))
                .andExpect(jsonPath("$.decisions[0].action").value("CERTIFIED"))
                .andExpect(jsonPath("$.decisions[0].authorizationNumber").value("AUTH123"));
    }
}
