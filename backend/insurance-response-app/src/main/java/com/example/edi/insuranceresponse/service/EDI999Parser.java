package com.example.edi.insuranceresponse.service;

import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.common.edi.ack.FunctionalGroupStatus;
import com.example.edi.common.edi.ack.SegmentError;
import com.example.edi.common.edi.ack.TransactionSetStatus;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.exception.EdiParseException;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class EDI999Parser {

    public List<EDI999Acknowledgment> parse(InputStream inputStream) {
        try {
            var factory = EDIInputFactory.newFactory();
            var reader = factory.createEDIStreamReader(inputStream);

            String currentSegment = null;
            int elementPosition = 0;

            // ISA envelope fields
            String isaSenderQualifier = "";
            String isaSenderId = "";
            String isaReceiverQualifier = "";
            String isaReceiverId = "";
            String isaDate = "";
            String isaTime = "";
            String isaControlNumber = "";
            String isaAckRequested = "";
            String isaUsageIndicator = "";

            // GS functional group fields
            String gsSenderId = "";
            String gsReceiverId = "";
            String gsDate = "";
            String gsTime = "";
            String gsControlNumber = "";

            // AK1 - acknowledged group control number
            String acknowledgedGroupControlNumber = "";

            // AK2 - current transaction set being acknowledged
            String currentAk2TransactionSetId = "";
            String currentAk2ControlNumber = "";

            // IK3 - pending segment error being built
            String ik3SegmentId = "";
            int ik3SegmentPosition = 0;
            String ik3SegmentErrorCode = "";

            // IK4 - element error data (captured during ELEMENT_DATA, applied at IK4 END_SEGMENT)
            int ik4ElementPosition = 0;
            String ik4ElementErrorCode = "";

            // AK9 - group-level acknowledgment (populated after all IK5s)
            FunctionalGroupStatus groupStatus = null;
            int txSetsIncluded = 0;
            int txSetsReceived = 0;
            int txSetsAccepted = 0;

            // Accumulate results across all transactions
            List<EDI999Acknowledgment> results = new ArrayList<>();

            // Per-transaction state
            List<SegmentError> currentErrors = new ArrayList<>();
            TransactionSetStatus currentTransactionStatus = null;

            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();
                switch (event) {
                    case START_SEGMENT -> {
                        currentSegment = reader.getText();
                        elementPosition = 0;
                        if ("IK4".equals(currentSegment)) {
                            ik4ElementPosition = 0;
                            ik4ElementErrorCode = "";
                        }
                    }
                    case ELEMENT_DATA -> {
                        elementPosition++;
                        String value = reader.getText();
                        if (currentSegment == null) continue;

                        switch (currentSegment) {
                            case "ISA" -> {
                                switch (elementPosition) {
                                    case 5 -> isaSenderQualifier = value;
                                    case 6 -> isaSenderId = value.trim();
                                    case 7 -> isaReceiverQualifier = value;
                                    case 8 -> isaReceiverId = value.trim();
                                    case 9 -> isaDate = value;
                                    case 10 -> isaTime = value;
                                    case 13 -> isaControlNumber = value;
                                    case 14 -> isaAckRequested = value;
                                    case 15 -> isaUsageIndicator = value;
                                }
                            }
                            case "GS" -> {
                                switch (elementPosition) {
                                    case 2 -> gsSenderId = value;
                                    case 3 -> gsReceiverId = value;
                                    case 4 -> gsDate = value;
                                    case 5 -> gsTime = value;
                                    case 6 -> gsControlNumber = value;
                                }
                            }
                            case "AK1" -> {
                                if (elementPosition == 2) acknowledgedGroupControlNumber = value;
                            }
                            case "AK2" -> {
                                switch (elementPosition) {
                                    case 1 -> currentAk2TransactionSetId = value;
                                    case 2 -> currentAk2ControlNumber = value;
                                }
                            }
                            case "IK3" -> {
                                switch (elementPosition) {
                                    case 1 -> ik3SegmentId = value;
                                    case 2 -> ik3SegmentPosition = parseIntSafe(value);
                                    case 4 -> ik3SegmentErrorCode = value;
                                }
                            }
                            case "IK4" -> {
                                switch (elementPosition) {
                                    case 1 -> ik4ElementPosition = parseIntSafe(value);
                                    case 3 -> ik4ElementErrorCode = value;
                                }
                            }
                            case "IK5" -> {
                                if (elementPosition == 1) currentTransactionStatus = TransactionSetStatus.fromCode(value);
                            }
                            case "AK9" -> {
                                switch (elementPosition) {
                                    case 1 -> groupStatus = FunctionalGroupStatus.fromCode(value);
                                    case 2 -> txSetsIncluded = parseIntSafe(value);
                                    case 3 -> txSetsReceived = parseIntSafe(value);
                                    case 4 -> txSetsAccepted = parseIntSafe(value);
                                }
                            }
                            default -> {}
                        }
                    }
                    case END_SEGMENT -> {
                        if ("IK3".equals(currentSegment)) {
                            // Finalize IK3 with no element error (IK4 not yet seen)
                            // If IK4 follows, we will replace the last error with element info
                            currentErrors.add(new SegmentError(
                                    ik3SegmentId,
                                    ik3SegmentPosition,
                                    ik3SegmentErrorCode,
                                    0,
                                    "",
                                    describeSegmentError(ik3SegmentErrorCode)
                            ));
                            // Reset IK3 fields
                            ik3SegmentId = "";
                            ik3SegmentPosition = 0;
                            ik3SegmentErrorCode = "";
                        } else if ("IK4".equals(currentSegment)) {
                            // Replace the last error (the IK3 just added) with element info attached
                            if (!currentErrors.isEmpty()) {
                                SegmentError prev = currentErrors.removeLast();
                                currentErrors.add(new SegmentError(
                                        prev.segmentId(),
                                        prev.segmentPosition(),
                                        prev.segmentErrorCode(),
                                        ik4ElementPosition,
                                        ik4ElementErrorCode,
                                        prev.errorDescription()
                                ));
                            }
                        } else if ("IK5".equals(currentSegment)) {
                            var envelope = new InterchangeEnvelope(
                                    isaSenderQualifier, isaSenderId,
                                    isaReceiverQualifier, isaReceiverId,
                                    isaDate, isaTime, isaControlNumber,
                                    isaAckRequested, isaUsageIndicator);
                            var functionalGroup = new FunctionalGroup(
                                    gsSenderId, gsReceiverId, gsDate, gsTime, gsControlNumber);
                            results.add(new EDI999Acknowledgment(
                                    envelope,
                                    functionalGroup,
                                    acknowledgedGroupControlNumber,
                                    currentAk2TransactionSetId,
                                    currentAk2ControlNumber,
                                    currentTransactionStatus,
                                    null, // groupStatus backfilled after AK9
                                    txSetsIncluded,
                                    txSetsReceived,
                                    txSetsAccepted,
                                    List.copyOf(currentErrors)
                            ));
                            // Reset per-transaction state
                            currentErrors = new ArrayList<>();
                            currentTransactionStatus = null;
                            currentAk2TransactionSetId = "";
                            currentAk2ControlNumber = "";
                        }
                    }
                    default -> {}
                }
            }

            reader.close();

            // Backfill groupStatus and counts into all results (AK9 comes after all IK5s)
            final FunctionalGroupStatus finalGroupStatus = groupStatus;
            final int finalIncluded = txSetsIncluded;
            final int finalReceived = txSetsReceived;
            final int finalAccepted = txSetsAccepted;

            return results.stream()
                    .map(ack -> new EDI999Acknowledgment(
                            ack.envelope(),
                            ack.functionalGroup(),
                            ack.acknowledgedGroupControlNumber(),
                            ack.acknowledgedTransactionSetId(),
                            ack.acknowledgedTransactionControlNumber(),
                            ack.transactionStatus(),
                            finalGroupStatus,
                            finalIncluded,
                            finalReceived,
                            finalAccepted,
                            ack.errors()
                    ))
                    .toList();

        } catch (EdiParseException e) {
            throw e;
        } catch (Exception e) {
            throw new EdiParseException("Failed to parse EDI 999 stream", e);
        }
    }

    private int parseIntSafe(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String describeSegmentError(String code) {
        return switch (code) {
            case "1" -> "Unrecognized segment ID";
            case "2" -> "Unexpected segment";
            case "3" -> "Mandatory segment missing";
            case "4" -> "Loop occurs over maximum times";
            case "5" -> "Segment exceeds maximum use";
            case "6" -> "Segment not in defined transaction set";
            case "7" -> "Segment not in proper sequence";
            case "8" -> "Segment has data element errors";
            default -> "Segment error code " + code;
        };
    }
}
