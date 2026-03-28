package com.example.edi.insuranceresponse.service;

import com.example.edi.insuranceresponse.dto.VerificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceResponseServiceTest {

    @Mock
    private EDI271Service edi271Service;

    @InjectMocks
    private InsuranceResponseService insuranceResponseService;

    @Test
    void processEligibilityResponse_delegatesToEDI271Service() {
        Path path = Path.of("/tmp/test.edi");
        VerificationResult expected = new VerificationResult("MEM001", "ACTIVE", "Patient eligible");

        when(edi271Service.parse(path)).thenReturn(expected);

        VerificationResult result = insuranceResponseService.processEligibilityResponse(path);

        assertEquals(expected, result);
        verify(edi271Service).parse(path);
    }
}
