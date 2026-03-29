package com.example.edi.claims.controller;

import com.example.edi.claims.dto.EncounterResponse;
import com.example.edi.claims.service.EncounterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @GetMapping
    public List<EncounterResponse> getEncounters() {
        return encounterService.getAllEncounters();
    }
}
