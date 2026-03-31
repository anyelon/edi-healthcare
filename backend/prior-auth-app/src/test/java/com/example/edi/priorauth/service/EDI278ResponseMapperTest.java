package com.example.edi.priorauth.service;

import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EDI278ResponseMapperTest {

    private final EDI278ResponseMapper mapper = new EDI278ResponseMapper();

    @Test
    void mapEncounterUpdates_certifiedDecision_returnsAuthNumber() {
        EDI278Response response = createResponse(
                new AuthorizationDecision("CERTIFIED", "AUTH123", "ENC001"));
        Map<String, String> updates = mapper.mapEncounterUpdates(response);
        assertEquals(1, updates.size());
        assertEquals("AUTH123", updates.get("ENC001"));
    }

    @Test
    void mapEncounterUpdates_deniedDecision_returnsEmpty() {
        EDI278Response response = createResponse(
                new AuthorizationDecision("DENIED", null, "ENC001"));
        Map<String, String> updates = mapper.mapEncounterUpdates(response);
        assertTrue(updates.isEmpty());
    }

    @Test
    void mapEncounterUpdates_multipleDecisions_returnsCertifiedOnly() {
        EDI278Response response = createResponseMultiple(List.of(
                new AuthorizationDecision("CERTIFIED", "AUTH001", "ENC001"),
                new AuthorizationDecision("DENIED", null, "ENC002")));
        Map<String, String> updates = mapper.mapEncounterUpdates(response);
        assertEquals(1, updates.size());
        assertEquals("AUTH001", updates.get("ENC001"));
        assertFalse(updates.containsKey("ENC002"));
    }

    private EDI278Response createResponse(AuthorizationDecision decision) {
        return createResponseMultiple(List.of(decision));
    }

    private EDI278Response createResponseMultiple(List<AuthorizationDecision> decisions) {
        InterchangeEnvelope env = new InterchangeEnvelope(
                "ZZ", "S", "ZZ", "R", "260415", "1200", "000000001", "0", "T");
        FunctionalGroup fg = new FunctionalGroup("S", "R", "20260415", "1200", "00001");
        TransactionHeader th = new TransactionHeader("00001", "278", "20260415", "1200");
        return new EDI278Response(env, fg, th, "Payer", "PAY001", "John", "Doe", "MEM001", decisions);
    }
}
