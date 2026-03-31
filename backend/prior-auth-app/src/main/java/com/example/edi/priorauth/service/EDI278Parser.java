package com.example.edi.priorauth.service;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.domain.AuthorizationDecision;
import com.example.edi.priorauth.domain.EDI278Response;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class EDI278Parser {

    public EDI278Response parse(Path filePath) throws Exception {
        var factory = EDIInputFactory.newFactory();

        String currentSegment = "";
        int elementPosition = 0;

        String isaSenderQual = "", isaSenderId = "";
        String isaReceiverQual = "", isaReceiverId = "";
        String isaDate = "", isaTime = "", isaControlNum = "";
        String isaAck = "", isaUsage = "";

        String gsSenderId = "", gsReceiverId = "";
        String gsDate = "", gsTime = "", gsControlNum = "";

        String stControlNum = "";
        String bhtDate = "", bhtTime = "";

        String nm1Qualifier = "";
        String payerName = "", payerId = "";
        String subscriberFirst = "", subscriberLast = "";
        String memberId = "";

        String currentEncounterId = "";
        String hcrAction = "";
        String hcrAuthNum = null;

        List<AuthorizationDecision> decisions = new ArrayList<>();

        try (var stream = new FileInputStream(filePath.toFile());
             var reader = factory.createEDIStreamReader(stream)) {

            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();

                switch (event) {
                    case START_SEGMENT -> {
                        currentSegment = reader.getText();
                        elementPosition = 0;
                        nm1Qualifier = "";
                        hcrAction = "";
                        hcrAuthNum = null;
                    }
                    case ELEMENT_DATA -> {
                        elementPosition++;
                        String value = reader.getText();

                        switch (currentSegment) {
                            case "ISA" -> {
                                switch (elementPosition) {
                                    case 5 -> isaSenderQual = value.trim();
                                    case 6 -> isaSenderId = value.trim();
                                    case 7 -> isaReceiverQual = value.trim();
                                    case 8 -> isaReceiverId = value.trim();
                                    case 9 -> isaDate = value.trim();
                                    case 10 -> isaTime = value.trim();
                                    case 13 -> isaControlNum = value.trim();
                                    case 14 -> isaAck = value.trim();
                                    case 15 -> isaUsage = value.trim();
                                }
                            }
                            case "GS" -> {
                                switch (elementPosition) {
                                    case 2 -> gsSenderId = value.trim();
                                    case 3 -> gsReceiverId = value.trim();
                                    case 4 -> gsDate = value.trim();
                                    case 5 -> gsTime = value.trim();
                                    case 6 -> gsControlNum = value.trim();
                                }
                            }
                            case "ST" -> {
                                if (elementPosition == 2) stControlNum = value.trim();
                            }
                            case "BHT" -> {
                                switch (elementPosition) {
                                    case 4 -> bhtDate = value.trim();
                                    case 5 -> bhtTime = value.trim();
                                }
                            }
                            case "NM1" -> {
                                switch (elementPosition) {
                                    case 1 -> nm1Qualifier = value.trim();
                                    case 3 -> {
                                        if ("X3".equals(nm1Qualifier)) payerName = value.trim();
                                        if ("IL".equals(nm1Qualifier)) subscriberLast = value.trim();
                                    }
                                    case 4 -> {
                                        if ("IL".equals(nm1Qualifier)) subscriberFirst = value.trim();
                                    }
                                    case 9 -> {
                                        if ("X3".equals(nm1Qualifier)) payerId = value.trim();
                                        if ("IL".equals(nm1Qualifier)) memberId = value.trim();
                                    }
                                }
                            }
                            case "TRN" -> {
                                if (elementPosition == 2) currentEncounterId = value.trim();
                            }
                            case "HCR" -> {
                                switch (elementPosition) {
                                    case 1 -> hcrAction = value.trim();
                                    case 2 -> hcrAuthNum = value.trim();
                                }
                            }
                        }
                    }
                    case END_SEGMENT -> {
                        if ("HCR".equals(currentSegment)) {
                            String action = mapHcrAction(hcrAction);
                            String authNum = hcrAuthNum != null && !hcrAuthNum.isEmpty() ? hcrAuthNum : null;
                            decisions.add(new AuthorizationDecision(action, authNum, currentEncounterId));
                        }
                    }
                    default -> {}
                }
            }
        }

        InterchangeEnvelope envelope = new InterchangeEnvelope(
                isaSenderQual, isaSenderId, isaReceiverQual, isaReceiverId,
                isaDate, isaTime, isaControlNum, isaAck, isaUsage);
        FunctionalGroup fg = new FunctionalGroup(gsSenderId, gsReceiverId, gsDate, gsTime, gsControlNum);
        TransactionHeader th = new TransactionHeader(stControlNum, "278", bhtDate, bhtTime);

        return new EDI278Response(envelope, fg, th,
                payerName, payerId, subscriberFirst, subscriberLast, memberId, decisions);
    }

    private String mapHcrAction(String code) {
        return switch (code) {
            case "A1" -> "CERTIFIED";
            case "A2" -> "DENIED";
            case "A3" -> "PENDED";
            case "A4" -> "MODIFIED";
            case "A6" -> "CANCELLED";
            default   -> code;
        };
    }
}
