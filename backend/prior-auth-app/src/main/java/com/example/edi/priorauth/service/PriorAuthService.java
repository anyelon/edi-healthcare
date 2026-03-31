package com.example.edi.priorauth.service;

import com.example.edi.common.document.*;
import com.example.edi.common.exception.*;
import com.example.edi.common.repository.*;
import com.example.edi.priorauth.config.ArchiveProperties;
import com.example.edi.priorauth.config.InterchangeProperties;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.priorauth.dto.PriorAuthBundle;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PriorAuthService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final PayerRepository payerRepository;
    private final PracticeRepository practiceRepository;
    private final EncounterProcedureRepository encounterProcedureRepository;
    private final EDI278Mapper edi278Mapper;
    private final EDI278Generator edi278Generator;
    private final EDI278Parser edi278Parser;
    private final EDI278ResponseMapper edi278ResponseMapper;
    private final InterchangeProperties interchangeProperties;
    private final ArchiveProperties archiveProperties;

    public PriorAuthService(
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            PayerRepository payerRepository,
            PracticeRepository practiceRepository,
            EncounterProcedureRepository encounterProcedureRepository,
            EDI278Mapper edi278Mapper,
            EDI278Generator edi278Generator,
            EDI278Parser edi278Parser,
            EDI278ResponseMapper edi278ResponseMapper,
            InterchangeProperties interchangeProperties,
            ArchiveProperties archiveProperties) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.payerRepository = payerRepository;
        this.practiceRepository = practiceRepository;
        this.encounterProcedureRepository = encounterProcedureRepository;
        this.edi278Mapper = edi278Mapper;
        this.edi278Generator = edi278Generator;
        this.edi278Parser = edi278Parser;
        this.edi278ResponseMapper = edi278ResponseMapper;
        this.interchangeProperties = interchangeProperties;
        this.archiveProperties = archiveProperties;
    }

    public String generatePriorAuth(List<String> encounterIds) {
        List<PriorAuthBundle> bundles = new ArrayList<>();

        for (String encounterId : encounterIds) {
            Encounter encounter = encounterRepository.findById(encounterId)
                    .orElseThrow(() -> new EncounterNotFoundException(encounterId));

            CompletableFuture<Patient> patientFuture = CompletableFuture.supplyAsync(() ->
                    patientRepository.findById(encounter.getPatientId())
                            .orElseThrow(() -> new PatientNotFoundException(encounter.getPatientId())));

            CompletableFuture<PatientInsurance> insuranceFuture = CompletableFuture.supplyAsync(() ->
                    patientInsuranceRepository
                            .findByPatientIdAndTerminationDateIsNull(encounter.getPatientId())
                            .orElseThrow(() -> new InsuranceNotFoundException(encounter.getPatientId())));

            CompletableFuture<Practice> practiceFuture = CompletableFuture.supplyAsync(() ->
                    practiceRepository.findById(encounter.getPracticeId())
                            .orElseThrow(() -> new PracticeNotFoundException(encounter.getPracticeId())));

            Patient patient = patientFuture.join();
            PatientInsurance insurance = insuranceFuture.join();
            Practice practice = practiceFuture.join();

            Payer payer = payerRepository.findById(insurance.getPayerId())
                    .orElseThrow(() -> new PayerNotFoundException(insurance.getPayerId()));

            List<EncounterProcedure> authProcedures = encounterProcedureRepository
                    .findByEncounterIdOrderByLineNumberAsc(encounterId).stream()
                    .filter(EncounterProcedure::isNeedsAuth)
                    .toList();

            bundles.add(new PriorAuthBundle(encounter, patient, insurance, payer, practice, authProcedures));
        }

        EDI278Request request = edi278Mapper.map(bundles, interchangeProperties);
        return edi278Generator.generate(request);
    }

    public EDI278Response processResponse(MultipartFile file) throws Exception {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        var archiveDir = Paths.get(archiveProperties.path()).toAbsolutePath();
        Files.createDirectories(archiveDir);
        var archivePath = archiveDir.resolve(filename);
        file.transferTo(archivePath.toFile());

        EDI278Response parsed = edi278Parser.parse(archivePath);

        Map<String, String> updates = edi278ResponseMapper.mapEncounterUpdates(parsed);
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            encounterRepository.findById(entry.getKey()).ifPresent(encounter -> {
                encounter.setAuthorizationNumber(entry.getValue());
                encounterRepository.save(encounter);
            });
        }

        return parsed;
    }
}
