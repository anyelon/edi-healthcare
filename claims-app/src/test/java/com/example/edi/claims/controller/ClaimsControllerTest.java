package com.example.edi.claims.controller;

import com.example.edi.claims.service.ClaimsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClaimsController.class)
class ClaimsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimsService claimsService;

    @Test
    void generateClaim_validRequest_returns200() throws Exception {
        when(claimsService.generateClaim("ENC001"))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/claims/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encounterId": "ENC001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=837_claim.edi"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    void generateClaim_missingEncounterId_returns400() throws Exception {
        mockMvc.perform(post("/api/claims/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateClaim_blankEncounterId_returns400() throws Exception {
        mockMvc.perform(post("/api/claims/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encounterId": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
