package com.example.edi.insurancerequest.controller;

import com.example.edi.insurancerequest.service.InsuranceRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsuranceRequestController.class)
class InsuranceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuranceRequestService insuranceRequestService;

    @Test
    void requestEligibility_validRequest_returns200() throws Exception {
        when(insuranceRequestService.generateEligibilityInquiry(List.of("P001")))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": ["P001"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=270_inquiry.edi"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    void requestEligibility_multiplePatients_returns200() throws Exception {
        when(insuranceRequestService.generateEligibilityInquiry(List.of("P001", "P002")))
                .thenReturn("ISA*00*...");

        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": ["P001", "P002"]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void requestEligibility_emptyList_returns400() throws Exception {
        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestEligibility_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestEligibility_blankPatientId_returns400() throws Exception {
        mockMvc.perform(post("/api/insurance/eligibility-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientIds": [""]}
                                """))
                .andExpect(status().isBadRequest());
    }
}
