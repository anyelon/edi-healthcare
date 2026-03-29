package com.example.edi.insuranceresponse.service;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.common.repository.EligibilityResponseRepository;
import com.example.edi.insuranceresponse.config.ArchiveProperties;
import com.example.edi.insuranceresponse.domain.loop.EDI271Response;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class EligibilityResponseService {

    private final EDI271Parser edi271Parser;
    private final EDI271Mapper edi271Mapper;
    private final EligibilityResponseRepository eligibilityResponseRepository;
    private final ArchiveProperties archiveProperties;

    public EligibilityResponseService(EDI271Parser edi271Parser,
                                      EDI271Mapper edi271Mapper,
                                      EligibilityResponseRepository eligibilityResponseRepository,
                                      ArchiveProperties archiveProperties) {
        this.edi271Parser = edi271Parser;
        this.edi271Mapper = edi271Mapper;
        this.eligibilityResponseRepository = eligibilityResponseRepository;
        this.archiveProperties = archiveProperties;
    }

    public EligibilityResponse processFile(MultipartFile file) throws Exception {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        var archiveDir = Paths.get(archiveProperties.path()).toAbsolutePath();
        Files.createDirectories(archiveDir);
        var archivePath = archiveDir.resolve(filename);
        file.transferTo(archivePath.toFile());

        EDI271Response parsed = edi271Parser.parse(archivePath);
        EligibilityResponse result = edi271Mapper.map(parsed, archivePath.toString(), LocalDateTime.now());
        return eligibilityResponseRepository.save(result);
    }
}
