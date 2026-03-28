package com.example.edi.claims.service;

import com.example.edi.claims.domain.loop.ClaimLoop;
import com.example.edi.claims.domain.loop.DiagnosisEntry;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.ServiceLineLoop;
import com.example.edi.common.edi.loop.BillingProviderLoop;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.Receiver;
import com.example.edi.common.edi.loop.Submitter;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class EDI837Generator {

    public String generate(EDI837Claim claim) {
        try {
            var factory = EDIOutputFactory.newFactory();
            factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
            var baos = new ByteArrayOutputStream();
            var writer = factory.createEDIStreamWriter(baos);

            int segmentCount = 0;

            writer.startInterchange();

            writeISA(writer, claim.envelope());
            writeGS(writer, claim.functionalGroup());

            segmentCount += writeST(writer, claim.transactionHeader());
            segmentCount += writeBHT(writer, claim.transactionHeader());
            segmentCount += writeSubmitter(writer, claim.submitter());
            segmentCount += writeReceiver(writer, claim.receiver());
            segmentCount += writeBillingProviderHL(writer);
            segmentCount += writeBillingProvider(writer, claim.billingProvider());
            segmentCount += writeSubscriberHL(writer);
            segmentCount += writeSubscriber(writer, claim.subscriber());
            segmentCount += writePayer(writer, claim.subscriber());
            segmentCount += writeClaim(writer, claim.claim());
            segmentCount += writeDiagnoses(writer, claim.claim().diagnoses());
            segmentCount += writeServiceLines(writer, claim.claim().serviceLines());

            // SE counts itself
            segmentCount++;
            writeSE(writer, segmentCount, claim.transactionHeader().transactionSetControlNumber());

            writeGE(writer, claim.functionalGroup().controlNumber());
            writeIEA(writer, claim.envelope().controlNumber());

            writer.endInterchange();
            writer.close();

            return baos.toString(StandardCharsets.UTF_8);
        } catch (EDIStreamException e) {
            throw new RuntimeException("Failed to generate EDI 837P", e);
        }
    }

    private void writeISA(EDIStreamWriter writer, InterchangeEnvelope env) throws EDIStreamException {
        writer.writeStartSegment("ISA");
        writer.writeElement("00");                          // ISA01
        writer.writeElement(padRight("", 10));              // ISA02
        writer.writeElement("00");                          // ISA03
        writer.writeElement(padRight("", 10));              // ISA04
        writer.writeElement(env.senderIdQualifier());       // ISA05
        writer.writeElement(padRight(env.senderId(), 15));  // ISA06
        writer.writeElement(env.receiverIdQualifier());     // ISA07
        writer.writeElement(padRight(env.receiverId(), 15)); // ISA08
        writer.writeElement(env.date());                    // ISA09
        writer.writeElement(env.time());                    // ISA10
        writer.writeElement("^");                           // ISA11 repetition separator
        writer.writeElement("00501");                       // ISA12
        writer.writeElement(env.controlNumber());           // ISA13
        writer.writeElement(env.ackRequested());            // ISA14
        writer.writeElement(env.usageIndicator());          // ISA15
        writer.writeElement(":");                           // ISA16 component separator
        writer.writeEndSegment();
    }

    private void writeGS(EDIStreamWriter writer, FunctionalGroup fg) throws EDIStreamException {
        writer.writeStartSegment("GS");
        writer.writeElement("HC");                          // GS01
        writer.writeElement(fg.senderId());                 // GS02
        writer.writeElement(fg.receiverId());               // GS03
        writer.writeElement(fg.date());                     // GS04
        writer.writeElement(fg.time());                     // GS05
        writer.writeElement(fg.controlNumber());            // GS06
        writer.writeElement("X");                           // GS07
        writer.writeElement("005010X222A1");                // GS08
        writer.writeEndSegment();
    }

    private int writeST(EDIStreamWriter writer, TransactionHeader th) throws EDIStreamException {
        writer.writeStartSegment("ST");
        writer.writeElement("837");                         // ST01
        writer.writeElement(th.transactionSetControlNumber()); // ST02
        writer.writeElement("005010X222A1");                // ST03
        writer.writeEndSegment();
        return 1;
    }

    private int writeBHT(EDIStreamWriter writer, TransactionHeader th) throws EDIStreamException {
        writer.writeStartSegment("BHT");
        writer.writeElement("0019");                        // BHT01
        writer.writeElement("00");                          // BHT02
        writer.writeElement(th.referenceId());              // BHT03
        writer.writeElement(th.creationDate());             // BHT04
        writer.writeElement(th.creationTime());             // BHT05
        writer.writeElement("CH");                          // BHT06
        writer.writeEndSegment();
        return 1;
    }

    private int writeSubmitter(EDIStreamWriter writer, Submitter sub) throws EDIStreamException {
        // NM1*41 - Submitter Name
        writer.writeStartSegment("NM1");
        writer.writeElement("41");                          // NM101
        writer.writeElement("2");                           // NM102 entity type (non-person)
        writer.writeElement(sub.name());                    // NM103
        writer.writeElement("");                            // NM104
        writer.writeElement("");                            // NM105
        writer.writeElement("");                            // NM106
        writer.writeElement("");                            // NM107
        writer.writeElement("46");                          // NM108
        writer.writeElement(sub.identifier());              // NM109
        writer.writeEndSegment();

        // PER*IC - Submitter Contact
        writer.writeStartSegment("PER");
        writer.writeElement("IC");                          // PER01
        writer.writeElement(sub.name());                    // PER02
        writer.writeElement("TE");                          // PER03
        writer.writeElement(sub.contactPhone());            // PER04
        writer.writeEndSegment();

        return 2;
    }

    private int writeReceiver(EDIStreamWriter writer, Receiver rec) throws EDIStreamException {
        writer.writeStartSegment("NM1");
        writer.writeElement("40");                          // NM101
        writer.writeElement("2");                           // NM102
        writer.writeElement(rec.name());                    // NM103
        writer.writeElement("");                            // NM104
        writer.writeElement("");                            // NM105
        writer.writeElement("");                            // NM106
        writer.writeElement("");                            // NM107
        writer.writeElement("46");                          // NM108
        writer.writeElement(rec.identifier());              // NM109
        writer.writeEndSegment();
        return 1;
    }

    private int writeBillingProviderHL(EDIStreamWriter writer) throws EDIStreamException {
        writer.writeStartSegment("HL");
        writer.writeElement("1");                           // HL01
        writer.writeElement("");                            // HL02
        writer.writeElement("20");                          // HL03
        writer.writeElement("1");                           // HL04
        writer.writeEndSegment();
        return 1;
    }

    private int writeBillingProvider(EDIStreamWriter writer, BillingProviderLoop bp) throws EDIStreamException {
        // NM1*85
        writer.writeStartSegment("NM1");
        writer.writeElement("85");
        writer.writeElement("2");
        writer.writeElement(bp.name());
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("XX");
        writer.writeElement(bp.npi());
        writer.writeEndSegment();

        // N3
        writer.writeStartSegment("N3");
        writer.writeElement(bp.address());
        writer.writeEndSegment();

        // N4
        writer.writeStartSegment("N4");
        writer.writeElement(bp.city());
        writer.writeElement(bp.state());
        writer.writeElement(bp.zipCode());
        writer.writeEndSegment();

        // REF*EI
        writer.writeStartSegment("REF");
        writer.writeElement("EI");
        writer.writeElement(bp.taxId());
        writer.writeEndSegment();

        return 4;
    }

    private int writeSubscriberHL(EDIStreamWriter writer) throws EDIStreamException {
        writer.writeStartSegment("HL");
        writer.writeElement("2");
        writer.writeElement("1");
        writer.writeElement("22");
        writer.writeElement("0");
        writer.writeEndSegment();
        return 1;
    }

    private int writeSubscriber(EDIStreamWriter writer, SubscriberLoop sub) throws EDIStreamException {
        // SBR
        writer.writeStartSegment("SBR");
        writer.writeElement(sub.subscriberRelationship());
        writer.writeElement("18");
        writer.writeElement(sub.groupNumber());
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement(sub.policyType());
        writer.writeEndSegment();

        // NM1*IL
        writer.writeStartSegment("NM1");
        writer.writeElement("IL");
        writer.writeElement("1");
        writer.writeElement(sub.lastName());
        writer.writeElement(sub.firstName());
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("MI");
        writer.writeElement(sub.memberId());
        writer.writeEndSegment();

        // N3
        writer.writeStartSegment("N3");
        writer.writeElement(sub.address());
        writer.writeEndSegment();

        // N4
        writer.writeStartSegment("N4");
        writer.writeElement(sub.city());
        writer.writeElement(sub.state());
        writer.writeElement(sub.zipCode());
        writer.writeEndSegment();

        // DMG
        writer.writeStartSegment("DMG");
        writer.writeElement("D8");
        writer.writeElement(sub.dateOfBirth());
        writer.writeElement(sub.genderCode());
        writer.writeEndSegment();

        return 5;
    }

    private int writePayer(EDIStreamWriter writer, SubscriberLoop sub) throws EDIStreamException {
        writer.writeStartSegment("NM1");
        writer.writeElement("PR");
        writer.writeElement("2");
        writer.writeElement(sub.payerName());
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("");
        writer.writeElement("PI");
        writer.writeElement(sub.payerId());
        writer.writeEndSegment();
        return 1;
    }

    private int writeClaim(EDIStreamWriter writer, ClaimLoop claim) throws EDIStreamException {
        writer.writeStartSegment("CLM");
        writer.writeElement(claim.claimId());                       // CLM01
        writer.writeElement(claim.totalCharge().toPlainString());   // CLM02
        writer.writeElement("");                                    // CLM03
        writer.writeElement("");                                    // CLM04
        // CLM05 composite: placeOfService:B:1
        writer.writeStartElement();
        writer.writeComponent(claim.placeOfServiceCode());
        writer.writeComponent("B");
        writer.writeComponent("1");
        writer.endElement();
        writer.writeElement("Y");                                   // CLM06
        writer.writeElement("A");                                   // CLM07
        writer.writeElement("Y");                                   // CLM08
        writer.writeElement("Y");                                   // CLM09
        writer.writeEndSegment();
        return 1;
    }

    private int writeDiagnoses(EDIStreamWriter writer, List<DiagnosisEntry> diagnoses) throws EDIStreamException {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return 0;
        }
        writer.writeStartSegment("HI");
        for (int i = 0; i < diagnoses.size(); i++) {
            DiagnosisEntry diag = diagnoses.get(i);
            String qualifier = (diag.rank() == 1) ? "ABK" : "ABF";
            writer.writeStartElement();
            writer.writeComponent(qualifier);
            writer.writeComponent(diag.diagnosisCode());
            writer.endElement();
        }
        writer.writeEndSegment();
        return 1;
    }

    private int writeServiceLines(EDIStreamWriter writer, List<ServiceLineLoop> lines) throws EDIStreamException {
        int count = 0;
        for (ServiceLineLoop line : lines) {
            // LX
            writer.writeStartSegment("LX");
            writer.writeElement(String.valueOf(line.lineNumber()));
            writer.writeEndSegment();
            count++;

            // SV1
            writer.writeStartSegment("SV1");
            // SV101 composite: HC:procedureCode[:modifier1:modifier2...]
            writer.writeStartElement();
            writer.writeComponent("HC");
            writer.writeComponent(line.procedureCode());
            for (String mod : line.modifiers()) {
                writer.writeComponent(mod);
            }
            writer.endElement();
            writer.writeElement(line.chargeAmount().toPlainString()); // SV102
            writer.writeElement(line.unitType());                     // SV103
            writer.writeElement(String.valueOf(line.units()));        // SV104
            writer.writeElement("");                                  // SV105
            writer.writeElement("");                                  // SV106
            // SV107 composite: diagnosis pointers separated by component separator
            writer.writeStartElement();
            for (int i = 0; i < line.diagnosisPointers().size(); i++) {
                if (i == 0) {
                    writer.writeComponent(String.valueOf(line.diagnosisPointers().get(i)));
                } else {
                    writer.writeComponent(String.valueOf(line.diagnosisPointers().get(i)));
                }
            }
            writer.endElement();
            writer.writeEndSegment();
            count++;

            // DTP*472
            writer.writeStartSegment("DTP");
            writer.writeElement("472");
            writer.writeElement("D8");
            writer.writeElement(line.dateOfService());
            writer.writeEndSegment();
            count++;
        }
        return count;
    }

    private void writeSE(EDIStreamWriter writer, int segmentCount, String controlNumber) throws EDIStreamException {
        writer.writeStartSegment("SE");
        writer.writeElement(String.valueOf(segmentCount));
        writer.writeElement(controlNumber);
        writer.writeEndSegment();
    }

    private void writeGE(EDIStreamWriter writer, String controlNumber) throws EDIStreamException {
        writer.writeStartSegment("GE");
        writer.writeElement("1");
        writer.writeElement(controlNumber);
        writer.writeEndSegment();
    }

    private void writeIEA(EDIStreamWriter writer, String controlNumber) throws EDIStreamException {
        writer.writeStartSegment("IEA");
        writer.writeElement("1");
        writer.writeElement(controlNumber);
        writer.writeEndSegment();
    }

    private String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value);
    }
}
