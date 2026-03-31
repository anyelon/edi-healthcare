package com.example.edi.priorauth.service;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import com.example.edi.priorauth.domain.EDI278Request;
import com.example.edi.priorauth.domain.ServiceReviewInfo;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class EDI278Generator {

    public String generate(EDI278Request request) {
        try {
            var factory = EDIOutputFactory.newFactory();
            factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
            var baos = new ByteArrayOutputStream();
            var writer = factory.createEDIStreamWriter(baos);

            int segmentCount = 0;
            writer.startInterchange();

            writeISA(writer, request.envelope());
            writeGS(writer, request.functionalGroup());

            segmentCount += writeST(writer, request.transactionHeader());
            segmentCount += writeBHT(writer, request.transactionHeader());

            int hlCounter = 1;

            int payerHL = hlCounter++;
            segmentCount += writeHL(writer, payerHL, 0, "20", "1");
            segmentCount += writePayerNM1(writer, request.payerName(), request.payerId());

            int providerHL = hlCounter++;
            segmentCount += writeHL(writer, providerHL, payerHL, "21", "1");
            segmentCount += writeProviderNM1(writer, request.providerName(), request.providerNpi());
            segmentCount += writeProviderREF(writer, request.providerNpi(), request.providerTaxId());

            for (EDI278Request.SubscriberReviewGroup group : request.subscriberGroups()) {
                int subscriberHL = hlCounter++;
                segmentCount += writeHL(writer, subscriberHL, providerHL, "22", "1");
                segmentCount += writeTRN(writer, group.encounterId());
                segmentCount += writeSubscriberNM1(writer, group.subscriber());
                segmentCount += writeDMG(writer, group.subscriber());

                int eventHL = hlCounter++;
                segmentCount += writeHL(writer, eventHL, subscriberHL, "EV", "0");
                segmentCount += writeUM(writer);

                for (ServiceReviewInfo service : group.services()) {
                    segmentCount += writeDTP(writer, service.serviceDate());
                    segmentCount += writeSV1(writer, service.procedureCode());
                    segmentCount += writeHI(writer, service.clinicalReason());
                }
            }

            segmentCount++;
            writeSE(writer, segmentCount, request.transactionHeader().transactionSetControlNumber());
            writeGE(writer, request.functionalGroup().controlNumber());
            writeIEA(writer, request.envelope().controlNumber());

            writer.endInterchange();
            writer.close();

            return baos.toString(StandardCharsets.UTF_8);
        } catch (EDIStreamException e) {
            throw new RuntimeException("Failed to generate EDI 278", e);
        }
    }

    private void writeISA(EDIStreamWriter w, InterchangeEnvelope env) throws EDIStreamException {
        w.writeStartSegment("ISA");
        elem(w, "00"); elem(w, padRight("", 10)); elem(w, "00"); elem(w, padRight("", 10));
        elem(w, env.senderIdQualifier()); elem(w, padRight(env.senderId(), 15));
        elem(w, env.receiverIdQualifier()); elem(w, padRight(env.receiverId(), 15));
        elem(w, env.date()); elem(w, env.time()); elem(w, "^"); elem(w, "00501");
        elem(w, env.controlNumber()); elem(w, env.ackRequested()); elem(w, env.usageIndicator());
        elem(w, ":"); w.writeEndSegment();
    }

    private void writeGS(EDIStreamWriter w, FunctionalGroup fg) throws EDIStreamException {
        w.writeStartSegment("GS");
        elem(w, "HI"); elem(w, fg.senderId()); elem(w, fg.receiverId());
        elem(w, fg.date()); elem(w, fg.time()); elem(w, fg.controlNumber());
        elem(w, "X"); elem(w, "005010X217"); w.writeEndSegment();
    }

    private int writeST(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("ST"); elem(w, "278"); elem(w, th.transactionSetControlNumber());
        elem(w, "005010X217"); w.writeEndSegment(); return 1;
    }

    private int writeBHT(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("BHT"); elem(w, "0007"); elem(w, "13");
        elem(w, th.transactionSetControlNumber()); elem(w, th.creationDate());
        elem(w, th.creationTime()); w.writeEndSegment(); return 1;
    }

    private int writeHL(EDIStreamWriter w, int id, int parentId, String levelCode, String childCode) throws EDIStreamException {
        w.writeStartSegment("HL"); elem(w, String.valueOf(id));
        elem(w, parentId > 0 ? String.valueOf(parentId) : "");
        elem(w, levelCode); elem(w, childCode); w.writeEndSegment(); return 1;
    }

    private int writePayerNM1(EDIStreamWriter w, String payerName, String payerId) throws EDIStreamException {
        w.writeStartSegment("NM1"); elem(w, "X3"); elem(w, "2"); elem(w, payerName);
        elem(w, ""); elem(w, ""); elem(w, ""); elem(w, ""); elem(w, "PI"); elem(w, payerId);
        w.writeEndSegment(); return 1;
    }

    private int writeProviderNM1(EDIStreamWriter w, String providerName, String npi) throws EDIStreamException {
        w.writeStartSegment("NM1"); elem(w, "1P"); elem(w, "2"); elem(w, providerName);
        elem(w, ""); elem(w, ""); elem(w, ""); elem(w, ""); elem(w, "XX"); elem(w, npi);
        w.writeEndSegment(); return 1;
    }

    private int writeProviderREF(EDIStreamWriter w, String npi, String taxId) throws EDIStreamException {
        int count = 0;
        w.writeStartSegment("REF"); elem(w, "1J"); elem(w, npi); w.writeEndSegment(); count++;
        w.writeStartSegment("REF"); elem(w, "EI"); elem(w, taxId); w.writeEndSegment(); count++;
        return count;
    }

    private int writeTRN(EDIStreamWriter w, String encounterId) throws EDIStreamException {
        w.writeStartSegment("TRN"); elem(w, "1"); elem(w, encounterId);
        elem(w, "9SENDER_ID"); w.writeEndSegment(); return 1;
    }

    private int writeSubscriberNM1(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("NM1"); elem(w, "IL"); elem(w, "1"); elem(w, sub.lastName());
        elem(w, sub.firstName()); elem(w, ""); elem(w, ""); elem(w, ""); elem(w, "MI");
        elem(w, sub.memberId()); w.writeEndSegment(); return 1;
    }

    private int writeDMG(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("DMG"); elem(w, "D8"); elem(w, sub.dateOfBirth());
        elem(w, sub.genderCode()); w.writeEndSegment(); return 1;
    }

    private int writeUM(EDIStreamWriter w) throws EDIStreamException {
        w.writeStartSegment("UM"); elem(w, "HS"); elem(w, "I"); elem(w, "");
        elem(w, ""); elem(w, "11"); w.writeEndSegment(); return 1;
    }

    private int writeDTP(EDIStreamWriter w, String serviceDate) throws EDIStreamException {
        w.writeStartSegment("DTP"); elem(w, "472"); elem(w, "D8"); elem(w, serviceDate);
        w.writeEndSegment(); return 1;
    }

    private int writeSV1(EDIStreamWriter w, String procedureCode) throws EDIStreamException {
        w.writeStartSegment("SV1");
        w.writeStartElement(); w.writeComponent("HC"); w.writeComponent(procedureCode); w.endElement();
        w.writeEmptyElement();
        elem(w, "UN"); elem(w, "1"); w.writeEndSegment(); return 1;
    }

    private int writeHI(EDIStreamWriter w, String clinicalReason) throws EDIStreamException {
        w.writeStartSegment("HI");
        w.writeStartElement(); w.writeComponent("BF"); w.writeComponent(clinicalReason); w.endElement();
        w.writeEndSegment(); return 1;
    }

    private void writeSE(EDIStreamWriter w, int segmentCount, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("SE"); elem(w, String.valueOf(segmentCount)); elem(w, controlNumber);
        w.writeEndSegment();
    }

    private void writeGE(EDIStreamWriter w, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("GE"); elem(w, "1"); elem(w, controlNumber); w.writeEndSegment();
    }

    private void writeIEA(EDIStreamWriter w, String controlNumber) throws EDIStreamException {
        w.writeStartSegment("IEA"); elem(w, "1"); elem(w, controlNumber); w.writeEndSegment();
    }

    private void elem(EDIStreamWriter w, String value) throws EDIStreamException {
        w.writeElement(value != null ? value : "");
    }

    private String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value);
    }
}
