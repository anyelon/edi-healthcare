package com.example.edi.insuranceresponse.service;

import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.common.edi.ack.FunctionalGroupStatus;
import com.example.edi.common.edi.ack.SegmentError;
import com.example.edi.common.edi.ack.TransactionSetStatus;
import com.example.edi.common.exception.EdiParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EDI999ParserTest {

    private EDI999Parser parser;

    @BeforeEach
    void setUp() {
        parser = new EDI999Parser();
    }

    @Test
    void parse_accepted_extractsEnvelopeFields() throws Exception {
        InputStream input = loadResource("edi/999_accepted.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        assertThat(results).hasSize(1);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.envelope().senderId()).isEqualTo("CLEARINGHOUSE01");
        assertThat(ack.envelope().receiverId()).isEqualTo("SENDER12345");
        assertThat(ack.functionalGroup().controlNumber()).isEqualTo("1");
    }

    @Test
    void parse_accepted_extractsGroupControlNumber() throws Exception {
        InputStream input = loadResource("edi/999_accepted.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        assertThat(results.getFirst().acknowledgedGroupControlNumber()).isEqualTo("1");
    }

    @Test
    void parse_accepted_extractsTransactionSetInfo() throws Exception {
        InputStream input = loadResource("edi/999_accepted.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.acknowledgedTransactionSetId()).isEqualTo("837");
        assertThat(ack.acknowledgedTransactionControlNumber()).isEqualTo("0001");
    }

    @Test
    void parse_accepted_mapsAcceptedStatus() throws Exception {
        InputStream input = loadResource("edi/999_accepted.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.transactionStatus()).isEqualTo(TransactionSetStatus.ACCEPTED);
        assertThat(ack.groupStatus()).isEqualTo(FunctionalGroupStatus.ACCEPTED);
    }

    @Test
    void parse_accepted_extractsGroupCounts() throws Exception {
        InputStream input = loadResource("edi/999_accepted.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.transactionSetsIncluded()).isEqualTo(1);
        assertThat(ack.transactionSetsReceived()).isEqualTo(1);
        assertThat(ack.transactionSetsAccepted()).isEqualTo(1);
    }

    @Test
    void parse_accepted_hasNoErrors() throws Exception {
        InputStream input = loadResource("edi/999_accepted.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        assertThat(results.getFirst().errors()).isEmpty();
    }

    @Test
    void parse_rejected_mapsRejectedStatus() throws Exception {
        InputStream input = loadResource("edi/999_rejected.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.transactionStatus()).isEqualTo(TransactionSetStatus.REJECTED);
        assertThat(ack.groupStatus()).isEqualTo(FunctionalGroupStatus.REJECTED);
    }

    @Test
    void parse_rejected_extractsSegmentErrors() throws Exception {
        InputStream input = loadResource("edi/999_rejected.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.errors()).hasSize(2);
        assertThat(ack.errors().get(0).segmentId()).isEqualTo("CLM");
        assertThat(ack.errors().get(0).segmentPosition()).isEqualTo(22);
        assertThat(ack.errors().get(0).segmentErrorCode()).isEqualTo("8");
        assertThat(ack.errors().get(1).segmentId()).isEqualTo("NM1");
        assertThat(ack.errors().get(1).segmentPosition()).isEqualTo(35);
        assertThat(ack.errors().get(1).segmentErrorCode()).isEqualTo("5");
    }

    @Test
    void parse_rejected_extractsElementErrors() throws Exception {
        InputStream input = loadResource("edi/999_rejected.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.errors().get(0).elementPosition()).isEqualTo(1);
        assertThat(ack.errors().get(0).elementErrorCode()).isEqualTo("1");
    }

    @Test
    void parse_acceptedWithErrors_mapsStatus() throws Exception {
        InputStream input = loadResource("edi/999_accepted_with_errors.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.transactionStatus()).isEqualTo(TransactionSetStatus.ACCEPTED_WITH_ERRORS);
        assertThat(ack.groupStatus()).isEqualTo(FunctionalGroupStatus.ACCEPTED_WITH_ERRORS);
        assertThat(ack.errors()).hasSize(1);
        assertThat(ack.errors().getFirst().segmentId()).isEqualTo("REF");
    }

    @Test
    void parse_multipleTransactions_returnsMultipleResults() throws Exception {
        InputStream input = loadResource("edi/999_multiple_transactions.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).acknowledgedTransactionControlNumber()).isEqualTo("0001");
        assertThat(results.get(0).transactionStatus()).isEqualTo(TransactionSetStatus.ACCEPTED);
        assertThat(results.get(0).errors()).isEmpty();
        assertThat(results.get(1).acknowledgedTransactionControlNumber()).isEqualTo("0002");
        assertThat(results.get(1).transactionStatus()).isEqualTo(TransactionSetStatus.REJECTED);
        assertThat(results.get(1).errors()).hasSize(1);
    }

    @Test
    void parse_multipleTransactions_sharesGroupStatus() throws Exception {
        InputStream input = loadResource("edi/999_multiple_transactions.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        assertThat(results.get(0).groupStatus()).isEqualTo(FunctionalGroupStatus.PARTIALLY_ACCEPTED);
        assertThat(results.get(1).groupStatus()).isEqualTo(FunctionalGroupStatus.PARTIALLY_ACCEPTED);
        assertThat(results.get(0).transactionSetsIncluded()).isEqualTo(2);
        assertThat(results.get(0).transactionSetsAccepted()).isEqualTo(1);
    }

    @Test
    void parse_malformedInput_throwsException() {
        InputStream input = new ByteArrayInputStream("NOT EDI DATA".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void parse_multipleIK4sPerIK3_createsOneErrorPerIK4() throws Exception {
        InputStream input = loadResource("edi/999_multiple_element_errors.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        EDI999Acknowledgment ack = results.getFirst();
        assertThat(ack.errors()).hasSize(2);
        SegmentError first = ack.errors().get(0);
        assertThat(first.segmentId()).isEqualTo("CLM");
        assertThat(first.segmentPosition()).isEqualTo(22);
        assertThat(first.segmentErrorCode()).isEqualTo("8");
        assertThat(first.elementPosition()).isEqualTo(1);
        assertThat(first.elementErrorCode()).isEqualTo("1");
        SegmentError second = ack.errors().get(1);
        assertThat(second.segmentId()).isEqualTo("CLM");
        assertThat(second.segmentPosition()).isEqualTo(22);
        assertThat(second.segmentErrorCode()).isEqualTo("8");
        assertThat(second.elementPosition()).isEqualTo(2);
        assertThat(second.elementErrorCode()).isEqualTo("6");
    }

    @Test
    void parse_missingAK9_throwsEdiParseException() {
        InputStream input = getClass().getClassLoader().getResourceAsStream("edi/999_missing_ak9.edi");
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(EdiParseException.class)
                .hasMessageContaining("AK9");
    }

    @Test
    void parse_ik3WithoutIk4_hasNullElementErrorCode() throws Exception {
        InputStream input = loadResource("edi/999_rejected.edi");
        List<EDI999Acknowledgment> results = parser.parse(input);
        // errors[1] is NM1 with no IK4 - bare segment error
        SegmentError bareError = results.getFirst().errors().get(1);
        assertThat(bareError.segmentId()).isEqualTo("NM1");
        assertThat(bareError.elementPosition()).isZero();
        assertThat(bareError.elementErrorCode()).isNull();
    }

    private InputStream loadResource(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Test resource not found: " + path);
        }
        return stream;
    }
}
