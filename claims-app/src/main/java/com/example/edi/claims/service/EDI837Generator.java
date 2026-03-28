package com.example.edi.claims.service;

import com.example.edi.claims.domain.loop.ClaimLoop;
import com.example.edi.claims.domain.loop.DiagnosisEntry;
import com.example.edi.claims.domain.loop.EDI837Claim;
import com.example.edi.claims.domain.loop.ServiceLineLoop;
import com.example.edi.claims.domain.loop.SubscriberGroup;
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

            int hlCounter = 2;
            for (SubscriberGroup group : claim.subscriberGroups()) {
                segmentCount += writeSubscriberHL(writer, hlCounter);
                hlCounter++;
                segmentCount += writeSubscriber(writer, group.subscriber());
                segmentCount += writePayer(writer, group.subscriber());

                for (ClaimLoop claimLoop : group.claims()) {
                    segmentCount += writeClaim(writer, claimLoop);
                    segmentCount += writeDiagnoses(writer, claimLoop.diagnoses());
                    segmentCount += writeServiceLines(writer, claimLoop.serviceLines());
                }
            }

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

    private void writeISA(EDIStreamWriter w, InterchangeEnvelope env) throws EDIStreamException {
        w.writeStartSegment("ISA");
        elem(w, "00");
        elem(w, padRight("", 10));
        elem(w, "00");
        elem(w, padRight("", 10));
        elem(w, env.senderIdQualifier());
        elem(w, padRight(env.senderId(), 15));
        elem(w, env.receiverIdQualifier());
        elem(w, padRight(env.receiverId(), 15));
        elem(w, env.date());
        elem(w, env.time());
        elem(w, "^");
        elem(w, "00501");
        elem(w, env.controlNumber());
        elem(w, env.ackRequested());
        elem(w, env.usageIndicator());
        elem(w, ":");
        w.writeEndSegment();
    }

    private void writeGS(EDIStreamWriter w, FunctionalGroup fg) throws EDIStreamException {
        w.writeStartSegment("GS");
        elem(w, "HC");
        elem(w, fg.senderId());
        elem(w, fg.receiverId());
        elem(w, fg.date());
        elem(w, fg.time());
        elem(w, fg.controlNumber());
        elem(w, "X");
        elem(w, "005010X222A1");
        w.writeEndSegment();
    }

    private int writeST(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("ST");
        elem(w, "837");
        elem(w, th.transactionSetControlNumber());
        elem(w, "005010X222A1");
        w.writeEndSegment();
        return 1;
    }

    private int writeBHT(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("BHT");
        elem(w, "0019");
        elem(w, "00");
        elem(w, th.referenceId());
        elem(w, th.creationDate());
        elem(w, th.creationTime());
        elem(w, "CH");
        w.writeEndSegment();
        return 1;
    }

    private int writeSubmitter(EDIStreamWriter w, Submitter sub) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "41");
        elem(w, "2");
        elem(w, sub.name());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "46");
        elem(w, sub.identifier());
        w.writeEndSegment();

        w.writeStartSegment("PER");
        elem(w, "IC");
        elem(w, sub.name());
        elem(w, "TE");
        elem(w, sub.contactPhone());
        w.writeEndSegment();

        return 2;
    }

    private int writeReceiver(EDIStreamWriter w, Receiver rec) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "40");
        elem(w, "2");
        elem(w, rec.name());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "46");
        elem(w, rec.identifier());
        w.writeEndSegment();
        return 1;
    }

    private int writeBillingProviderHL(EDIStreamWriter w) throws EDIStreamException {
        w.writeStartSegment("HL");
        elem(w, "1");
        elem(w, "");
        elem(w, "20");
        elem(w, "1");
        w.writeEndSegment();
        return 1;
    }

    private int writeBillingProvider(EDIStreamWriter w, BillingProviderLoop bp) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "85");
        elem(w, "2");
        elem(w, bp.name());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "XX");
        elem(w, bp.npi());
        w.writeEndSegment();

        w.writeStartSegment("N3");
        elem(w, bp.address());
        w.writeEndSegment();

        w.writeStartSegment("N4");
        elem(w, bp.city());
        elem(w, bp.state());
        elem(w, bp.zipCode());
        w.writeEndSegment();

        w.writeStartSegment("REF");
        elem(w, "EI");
        elem(w, bp.taxId());
        w.writeEndSegment();

        return 4;
    }

    private int writeSubscriberHL(EDIStreamWriter w, int hlNumber) throws EDIStreamException {
        w.writeStartSegment("HL");
        elem(w, String.valueOf(hlNumber));
        elem(w, "1");
        elem(w, "22");
        elem(w, "0");
        w.writeEndSegment();
        return 1;
    }

    private int writeSubscriber(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("SBR");
        elem(w, sub.subscriberRelationship());
        elem(w, "18");
        elem(w, sub.groupNumber());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, sub.policyType());
        w.writeEndSegment();

        w.writeStartSegment("NM1");
        elem(w, "IL");
        elem(w, "1");
        elem(w, sub.lastName());
        elem(w, sub.firstName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "MI");
        elem(w, sub.memberId());
        w.writeEndSegment();

        w.writeStartSegment("N3");
        elem(w, sub.address());
        w.writeEndSegment();

        w.writeStartSegment("N4");
        elem(w, sub.city());
        elem(w, sub.state());
        elem(w, sub.zipCode());
        w.writeEndSegment();

        w.writeStartSegment("DMG");
        elem(w, "D8");
        elem(w, sub.dateOfBirth());
        elem(w, sub.genderCode());
        w.writeEndSegment();

        return 5;
    }

    private int writePayer(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "PR");
        elem(w, "2");
        elem(w, sub.payerName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "PI");
        elem(w, sub.payerId());
        w.writeEndSegment();
        return 1;
    }

    private int writeClaim(EDIStreamWriter w, ClaimLoop claim) throws EDIStreamException {
        w.writeStartSegment("CLM");
        elem(w, claim.claimId());
        elem(w, claim.totalCharge() != null ? claim.totalCharge().toPlainString() : "0");
        elem(w, "");
        elem(w, "");
        w.writeStartElement();
        comp(w, claim.placeOfServiceCode());
        comp(w, "B");
        comp(w, "1");
        w.endElement();
        elem(w, "Y");
        elem(w, "A");
        elem(w, "Y");
        elem(w, "Y");
        w.writeEndSegment();
        return 1;
    }

    private int writeDiagnoses(EDIStreamWriter w, List<DiagnosisEntry> diagnoses) throws EDIStreamException {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return 0;
        }
        w.writeStartSegment("HI");
        for (DiagnosisEntry diag : diagnoses) {
            String qualifier = (diag.rank() == 1) ? "ABK" : "ABF";
            w.writeStartElement();
            comp(w, qualifier);
            comp(w, diag.diagnosisCode());
            w.endElement();
        }
        w.writeEndSegment();
        return 1;
    }

    private int writeServiceLines(EDIStreamWriter w, List<ServiceLineLoop> lines) throws EDIStreamException {
        int count = 0;
        for (ServiceLineLoop line : lines) {
            w.writeStartSegment("LX");
            elem(w, String.valueOf(line.lineNumber()));
            w.writeEndSegment();
            count++;

            w.writeStartSegment("SV1");
            w.writeStartElement();
            comp(w, "HC");
            comp(w, line.procedureCode());
            if (line.modifiers() != null) {
                for (String mod : line.modifiers()) {
                    comp(w, mod);
                }
            }
            w.endElement();
            elem(w, line.chargeAmount() != null ? line.chargeAmount().toPlainString() : "0");
            elem(w, line.unitType());
            elem(w, String.valueOf(line.units()));
            elem(w, "");
            elem(w, "");
            if (line.diagnosisPointers() != null && !line.diagnosisPointers().isEmpty()) {
                w.writeStartElement();
                for (Integer ptr : line.diagnosisPointers()) {
                    comp(w, String.valueOf(ptr));
                }
                w.endElement();
            }
            w.writeEndSegment();
            count++;

            w.writeStartSegment("DTP");
            elem(w, "472");
            elem(w, "D8");
            elem(w, line.dateOfService());
            w.writeEndSegment();
            count++;
        }
        return count;
    }

    private void writeSE(EDIStreamWriter w, int segmentCount, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("SE");
        elem(w, String.valueOf(segmentCount));
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeGE(EDIStreamWriter w, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("GE");
        elem(w, "1");
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeIEA(EDIStreamWriter w, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("IEA");
        elem(w, "1");
        elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void elem(EDIStreamWriter w, String value) throws EDIStreamException {
        w.writeElement(value != null ? value : "");
    }

    private void comp(EDIStreamWriter w, String value) throws EDIStreamException {
        w.writeComponent(value != null ? value : "");
    }

    private String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value);
    }
}
