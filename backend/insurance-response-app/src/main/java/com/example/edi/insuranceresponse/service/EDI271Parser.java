package com.example.edi.insuranceresponse.service;

import com.example.edi.common.exception.EdiParseException;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.insuranceresponse.domain.loop.BenefitInfo;
import com.example.edi.insuranceresponse.domain.loop.EDI271Response;
import com.example.edi.insuranceresponse.domain.loop.PayerInfo;
import com.example.edi.insuranceresponse.domain.loop.SubscriberInfo;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EDI271Parser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public EDI271Response parse(Path filePath) {
        try (var inputStream = new FileInputStream(filePath.toFile())) {
            var factory = EDIInputFactory.newFactory();
            var reader = factory.createEDIStreamReader(inputStream);

            String currentSegment = null;
            int elementPosition = 0;
            boolean inComposite = false;

            // Subscriber fields
            String subscriberFirstName = "";
            String subscriberLastName = "";
            String subscriberMemberId = "";
            String subscriberGroupNumber = "";
            String eligibilityStatus = "UNKNOWN";
            boolean eligibilityStatusSet = false;

            // Payer fields
            String payerName = "";
            String payerId = "";

            // NM1 qualifier tracking
            String nm1Qualifier = "";

            // Benefit tracking
            List<BenefitInfo> benefits = new ArrayList<>();
            String ebCode = "";
            String ebCoverageLevel = "";
            String ebServiceType = "";
            String ebTimePeriod = "";
            String ebAmount = "";
            boolean inEbSegment = false;

            // Coverage dates
            LocalDate coverageStartDate = null;
            LocalDate coverageEndDate = null;

            // DTP tracking
            String dtpQualifier = "";

            // MSG tracking
            String lastMessage = "";

            // Envelope fields
            String isaSenderQualifier = "";
            String isaSenderId = "";
            String isaReceiverQualifier = "";
            String isaReceiverId = "";
            String isaDate = "";
            String isaTime = "";
            String isaControlNumber = "";
            String isaAckRequested = "";
            String isaUsageIndicator = "";

            String gsSenderId = "";
            String gsReceiverId = "";
            String gsDate = "";
            String gsTime = "";
            String gsControlNumber = "";

            String stControlNumber = "";
            String bhtReferenceId = "";
            String bhtDate = "";
            String bhtTime = "";

            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();
                switch (event) {
                    case START_SEGMENT -> {
                        currentSegment = reader.getText();
                        elementPosition = 0;
                        if ("EB".equals(currentSegment)) {
                            if (inEbSegment) {
                                benefits.add(buildBenefit(ebCode, ebCoverageLevel, ebServiceType,
                                        ebTimePeriod, ebAmount, lastMessage, coverageStartDate, coverageEndDate));
                                lastMessage = "";
                            }
                            inEbSegment = true;
                            ebCode = "";
                            ebCoverageLevel = "";
                            ebServiceType = "";
                            ebTimePeriod = "";
                            ebAmount = "";
                        }
                    }
                    case START_COMPOSITE -> {
                        inComposite = true;
                    }
                    case END_COMPOSITE -> {
                        inComposite = false;
                    }
                    case ELEMENT_DATA -> {
                        if (!inComposite) {
                            elementPosition++;
                        }
                        String value = reader.getText();
                        if (currentSegment == null) {
                            continue;
                        }

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
                            case "ST" -> {
                                if (elementPosition == 2) {
                                    stControlNumber = value;
                                }
                            }
                            case "BHT" -> {
                                switch (elementPosition) {
                                    case 3 -> bhtReferenceId = value;
                                    case 4 -> bhtDate = value;
                                    case 5 -> bhtTime = value;
                                }
                            }
                            case "NM1" -> {
                                switch (elementPosition) {
                                    case 1 -> nm1Qualifier = value;
                                    case 3 -> {
                                        if ("IL".equals(nm1Qualifier)) {
                                            subscriberLastName = value;
                                        } else if ("PR".equals(nm1Qualifier)) {
                                            payerName = value;
                                        }
                                    }
                                    case 4 -> {
                                        if ("IL".equals(nm1Qualifier)) {
                                            subscriberFirstName = value;
                                        }
                                    }
                                    case 9 -> {
                                        if ("IL".equals(nm1Qualifier)) {
                                            subscriberMemberId = value;
                                        } else if ("PR".equals(nm1Qualifier)) {
                                            payerId = value;
                                        }
                                    }
                                }
                            }
                            case "EB" -> {
                                switch (elementPosition) {
                                    case 1 -> {
                                        ebCode = value;
                                        if (!eligibilityStatusSet) {
                                            eligibilityStatus = mapEligibilityCode(value);
                                            eligibilityStatusSet = true;
                                        }
                                    }
                                    case 2 -> ebCoverageLevel = value;
                                    case 3 -> ebServiceType = value;
                                    case 6 -> ebTimePeriod = value;
                                    case 7 -> ebAmount = value;
                                }
                            }
                            case "DTP" -> {
                                switch (elementPosition) {
                                    case 1 -> dtpQualifier = value;
                                    case 3 -> {
                                        LocalDate date = LocalDate.parse(value, DATE_FORMAT);
                                        if ("346".equals(dtpQualifier)) {
                                            coverageStartDate = date;
                                        } else if ("347".equals(dtpQualifier)) {
                                            coverageEndDate = date;
                                        }
                                    }
                                }
                            }
                            case "MSG" -> {
                                if (elementPosition == 1) {
                                    lastMessage = value;
                                }
                            }
                            default -> {}
                        }
                    }
                    case END_SEGMENT -> {
                        if ("NM1".equals(currentSegment)) {
                            nm1Qualifier = "";
                        }
                        if ("DTP".equals(currentSegment)) {
                            dtpQualifier = "";
                        }
                    }
                    default -> {}
                }
            }

            // Finalize last EB if pending
            if (inEbSegment) {
                benefits.add(buildBenefit(ebCode, ebCoverageLevel, ebServiceType,
                        ebTimePeriod, ebAmount, lastMessage, coverageStartDate, coverageEndDate));
            }

            // Apply coverage dates to the first benefit if it lacks them
            if (!benefits.isEmpty() && benefits.getFirst().coverageStartDate() == null && coverageStartDate != null) {
                BenefitInfo first = benefits.getFirst();
                benefits.set(0, new BenefitInfo(first.benefitType(), first.coverageLevel(), first.serviceType(),
                        first.inNetwork(), first.amount(), first.percent(), first.timePeriod(), first.message(),
                        coverageStartDate, coverageEndDate));
            }

            reader.close();

            var envelope = new InterchangeEnvelope(isaSenderQualifier, isaSenderId, isaReceiverQualifier,
                    isaReceiverId, isaDate, isaTime, isaControlNumber, isaAckRequested, isaUsageIndicator);
            var functionalGroup = new FunctionalGroup(gsSenderId, gsReceiverId, gsDate, gsTime, gsControlNumber);
            var transactionHeader = new TransactionHeader(stControlNumber, bhtReferenceId, bhtDate, bhtTime);
            var subscriberInfo = new SubscriberInfo(subscriberFirstName, subscriberLastName, subscriberMemberId,
                    subscriberGroupNumber, eligibilityStatus);
            var payerInfo = new PayerInfo(payerName, payerId);

            return new EDI271Response(envelope, functionalGroup, transactionHeader, subscriberInfo, payerInfo, benefits);

        } catch (EdiParseException e) {
            throw e;
        } catch (Exception e) {
            throw new EdiParseException("Failed to parse EDI 271 file: " + filePath, e);
        }
    }

    private String mapEligibilityCode(String code) {
        return switch (code) {
            case "1" -> "ACTIVE";
            case "6", "8" -> "INACTIVE";
            default -> "UNKNOWN";
        };
    }

    private String mapBenefitType(String code) {
        return switch (code) {
            case "1" -> "ACTIVE_COVERAGE";
            case "A" -> "CO-INSURANCE";
            case "B" -> "CO-PAYMENT";
            case "C" -> "DEDUCTIBLE";
            case "G" -> "OUT_OF_POCKET";
            case "F" -> "LIMITATIONS";
            default -> code;
        };
    }

    private BenefitInfo buildBenefit(String ebCode, String coverageLevel, String serviceType,
                                     String timePeriod, String amount, String message,
                                     LocalDate coverageStartDate, LocalDate coverageEndDate) {
        String benefitType = mapBenefitType(ebCode);
        BigDecimal amountValue = null;
        BigDecimal percentValue = null;

        if (amount != null && !amount.isEmpty()) {
            BigDecimal val = new BigDecimal(amount);
            if ("A".equals(ebCode)) {
                percentValue = val;
                amountValue = val;
            } else {
                amountValue = val;
            }
        }

        return new BenefitInfo(benefitType, coverageLevel, serviceType, false,
                amountValue, percentValue, timePeriod, message, coverageStartDate, coverageEndDate);
    }
}
