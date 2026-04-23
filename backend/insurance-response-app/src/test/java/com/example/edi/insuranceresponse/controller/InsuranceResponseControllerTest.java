package com.example.edi.insuranceresponse.controller;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.common.edi.ack.FunctionalGroupStatus;
import com.example.edi.common.edi.ack.TransactionSetStatus;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.exception.EdiParseException;
import com.example.edi.insuranceresponse.service.EDI999Service;
import com.example.edi.insuranceresponse.service.EligibilityResponseService;
import java.util.List;
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

    @MockitoBean
    private EDI999Service edi999Service;

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

    @Test
    void processEligibilityResponse_parseError_returns400() throws Exception {
        when(eligibilityResponseService.processFile(any()))
                .thenThrow(new EdiParseException("Failed to parse EDI 271 file", null));

        MockMultipartFile file = new MockMultipartFile("file", "bad_271.edi",
                "text/plain", "INVALID EDI".getBytes());

        mockMvc.perform(multipart("/api/insurance/eligibility-response").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Failed to parse EDI 271 file"));
    }

    @Test
    void processAcknowledgment_validFile_returns200() throws Exception {
        var envelope = new InterchangeEnvelope("ZZ", "CLEARINGHOUSE01", "ZZ", "SENDER12345",
                "260401", "1200", "000000001", "0", "T");
        var group = new FunctionalGroup("CLEARINGHOUSE01", "SENDER12345", "20260401", "1200", "1");
        var ack = new EDI999Acknowledgment(envelope, group, "1", "837", "0001",
                TransactionSetStatus.ACCEPTED, FunctionalGroupStatus.ACCEPTED,
                1, 1, 1, List.of());

        when(edi999Service.processFile(any())).thenReturn(List.of(ack));

        MockMultipartFile file = new MockMultipartFile("file", "999_ack.edi",
                "text/plain", "AK1*HC*1~".getBytes());

        mockMvc.perform(multipart("/api/insurance/acknowledgment").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$[0].transactionStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].acknowledgedTransactionSetId").value("837"));
    }

    @Test
    void processAcknowledgment_parseError_returns400() throws Exception {
        when(edi999Service.processFile(any()))
                .thenThrow(new EdiParseException("Failed to parse EDI 999 file", null));

        MockMultipartFile file = new MockMultipartFile("file", "bad_999.edi",
                "text/plain", "INVALID EDI".getBytes());

        mockMvc.perform(multipart("/api/insurance/acknowledgment").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
