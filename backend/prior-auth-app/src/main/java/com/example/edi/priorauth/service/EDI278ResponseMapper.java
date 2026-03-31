package com.example.edi.priorauth.service;

import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EDI278ResponseMapper {

    public Map<String, String> mapEncounterUpdates(EDI278Response response) {
        Map<String, String> updates = new LinkedHashMap<>();
        for (AuthorizationDecision decision : response.decisions()) {
            if ("CERTIFIED".equals(decision.action()) && decision.authorizationNumber() != null) {
                updates.put(decision.encounterId(), decision.authorizationNumber());
            }
        }
        return updates;
    }
}
