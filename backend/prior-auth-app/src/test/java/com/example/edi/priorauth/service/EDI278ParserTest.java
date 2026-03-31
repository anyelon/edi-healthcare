package com.example.edi.priorauth.service;

import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EDI278ParserTest {

    private final EDI278Parser parser = new EDI278Parser();

    @Test
    void parse_certifiedResponse_returnsApprovedDecision() throws Exception {
        Path path = Path.of("src/test/resources/278_response_certified.edi");
        EDI278Response response = parser.parse(path);

        assertNotNull(response);
        assertEquals("Test Payer", response.payerName());
        assertEquals("PAY001", response.payerId());
        assertEquals("John", response.subscriberFirstName());
        assertEquals("Doe", response.subscriberLastName());
        assertEquals("MEM001", response.memberId());

        assertEquals(1, response.decisions().size());
        AuthorizationDecision decision = response.decisions().getFirst();
        assertEquals("CERTIFIED", decision.action());
        assertEquals("AUTH12345", decision.authorizationNumber());
        assertEquals("ENC001", decision.encounterId());
    }

    @Test
    void parse_deniedResponse_returnsDeniedDecision() throws Exception {
        Path path = Path.of("src/test/resources/278_response_denied.edi");
        EDI278Response response = parser.parse(path);

        assertNotNull(response);
        assertEquals("Jane", response.subscriberFirstName());
        assertEquals("Smith", response.subscriberLastName());
        assertEquals("MEM002", response.memberId());

        assertEquals(1, response.decisions().size());
        AuthorizationDecision decision = response.decisions().getFirst();
        assertEquals("DENIED", decision.action());
        assertNull(decision.authorizationNumber());
        assertEquals("ENC002", decision.encounterId());
    }
}
