package com.example.edi.insuranceresponse.controller;

import com.example.edi.insuranceresponse.dto.VerificationResult;
import com.example.edi.insuranceresponse.service.InsuranceResponseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsuranceResponseController.class)
class InsuranceResponseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuranceResponseService insuranceResponseService;

    @Test
    void processEligibilityResponse_validFile_returnsJson() throws Exception {
        VerificationResult result = new VerificationResult("MEM001", "ACTIVE", "Patient eligible");
        when(insuranceResponseService.processEligibilityResponse(any())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile("file", "271_response.edi",
                "text/plain", "NM1*IL*1*Doe*John****MI*MEM001~EB*1~".getBytes());

        mockMvc.perform(multipart("/api/insurance/eligibility-response").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.patientId").value("MEM001"))
                .andExpect(jsonPath("$.verificationStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.verificationMessage").value("Patient eligible"));
    }

    @Test
    void processEligibilityResponse_noFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/insurance/eligibility-response"))
                .andExpect(status().isBadRequest());
    }
}
