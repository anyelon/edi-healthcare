package com.example.edi.insuranceresponse.service;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.common.repository.EligibilityResponseRepository;
import com.example.edi.insuranceresponse.config.ArchiveProperties;
import com.example.edi.insuranceresponse.domain.loop.EDI271Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EligibilityResponseServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private EDI271Parser edi271Parser;

    @Mock
    private EDI271Mapper edi271Mapper;

    @Mock
    private EligibilityResponseRepository eligibilityResponseRepository;

    @Mock
    private ArchiveProperties archiveProperties;

    @Test
    void processFile_success_archivesAndSaves() throws Exception {
        when(archiveProperties.path()).thenReturn(tempDir.toString());

        EDI271Response parsedResponse = mock(EDI271Response.class);
        when(edi271Parser.parse(any(Path.class))).thenReturn(parsedResponse);

        EligibilityResponse mappedResponse = new EligibilityResponse();
        mappedResponse.setStatus("COMPLETED");
        when(edi271Mapper.map(any(EDI271Response.class), any(String.class), any(LocalDateTime.class)))
                .thenReturn(mappedResponse);

        EligibilityResponse savedResponse = new EligibilityResponse();
        savedResponse.setStatus("COMPLETED");
        savedResponse.setId("saved-id");
        when(eligibilityResponseRepository.save(any(EligibilityResponse.class))).thenReturn(savedResponse);

        MockMultipartFile file = new MockMultipartFile("file", "271_response.edi",
                "text/plain", "ISA*00*".getBytes());

        EligibilityResponseService service = new EligibilityResponseService(
                edi271Parser, edi271Mapper, eligibilityResponseRepository, archiveProperties);

        EligibilityResponse result = service.processFile(file);

        verify(edi271Parser).parse(any(Path.class));
        verify(edi271Mapper).map(eq(parsedResponse), any(String.class), any(LocalDateTime.class));
        verify(eligibilityResponseRepository).save(mappedResponse);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void processFile_parseFailure_savesErrorDocument() throws Exception {
        when(archiveProperties.path()).thenReturn(tempDir.toString());

        when(edi271Parser.parse(any(Path.class)))
                .thenThrow(new RuntimeException("Parse failed"));

        ArgumentCaptor<EligibilityResponse> captor = ArgumentCaptor.forClass(EligibilityResponse.class);
        EligibilityResponse errorResponse = new EligibilityResponse();
        errorResponse.setStatus("ERROR");
        errorResponse.setErrorMessage("Parse failed");
        when(eligibilityResponseRepository.save(captor.capture())).thenReturn(errorResponse);

        MockMultipartFile file = new MockMultipartFile("file", "bad_271.edi",
                "text/plain", "INVALID EDI".getBytes());

        EligibilityResponseService service = new EligibilityResponseService(
                edi271Parser, edi271Mapper, eligibilityResponseRepository, archiveProperties);

        EligibilityResponse result = service.processFile(file);

        verify(eligibilityResponseRepository).save(any(EligibilityResponse.class));
        EligibilityResponse captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo("ERROR");
        assertThat(captured.getErrorMessage()).isEqualTo("Parse failed");
        assertThat(captured.getFilePath()).isNotNull();
        assertThat(captured.getReceivedAt()).isNotNull();
    }
}
