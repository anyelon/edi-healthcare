package com.example.edi.insuranceresponse.controller;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.insuranceresponse.service.EligibilityResponseService;
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
    private EligibilityResponseService eligibilityResponseService;

    @Test
    void processEligibilityResponse_validFile_returns200() throws Exception {
        EligibilityResponse response = new EligibilityResponse();
        response.setStatus("COMPLETED");
        when(eligibilityResponseService.processFile(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "271_response.edi",
                "text/plain", "NM1*IL*1*Doe*John****MI*MEM001~EB*1~".getBytes());

        mockMvc.perform(multipart("/api/insurance/eligibility-response").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void processEligibilityResponse_noFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/insurance/eligibility-response"))
                .andExpect(status().isBadRequest());
    }
}
