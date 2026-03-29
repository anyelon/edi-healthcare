package com.example.edi.insurancerequest.service;

import com.example.edi.insurancerequest.domain.loop.EDI270Inquiry;
import com.example.edi.insurancerequest.domain.loop.EligibilitySubscriber;
import com.example.edi.insurancerequest.domain.loop.InformationReceiverGroup;
import com.example.edi.insurancerequest.domain.loop.InformationSourceGroup;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.SubscriberLoop;
import com.example.edi.common.edi.loop.TransactionHeader;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class EDI270Generator {

    public String generate(EDI270Inquiry inquiry) {
        try {
            var factory = EDIOutputFactory.newFactory();
            factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
            var baos = new ByteArrayOutputStream();
            var writer = factory.createEDIStreamWriter(baos);

            int segmentCount = 0;

            writer.startInterchange();

            writeISA(writer, inquiry.envelope());
            writeGS(writer, inquiry.functionalGroup());

            segmentCount += writeST(writer, inquiry.transactionHeader());
            segmentCount += writeBHT(writer, inquiry.transactionHeader());

            int hlCounter = 1;
            for (InformationSourceGroup sourceGroup : inquiry.informationSourceGroups()) {
                int payerHL = hlCounter++;
                segmentCount += writeHL(writer, payerHL, 0, "20", "1");
                segmentCount += writePayerNM1(writer, sourceGroup);

                InformationReceiverGroup receiver = sourceGroup.informationReceiver();
                int providerHL = hlCounter++;
                segmentCount += writeHL(writer, providerHL, payerHL, "21", "1");
                segmentCount += writeProviderNM1(writer, receiver);
                segmentCount += writeProviderREF(writer, receiver);

                for (EligibilitySubscriber eligSub : receiver.subscribers()) {
                    int subscriberHL = hlCounter++;
                    segmentCount += writeHL(writer, subscriberHL, providerHL, "22", "0");
                    segmentCount += writeSubscriberNM1(writer, eligSub.subscriber());
                    segmentCount += writeDMG(writer, eligSub.subscriber());
                    segmentCount += writeDTP(writer, eligSub.eligibilityDate());
                    segmentCount += writeEQ(writer);
                }
            }

            segmentCount++;
            writeSE(writer, segmentCount, inquiry.transactionHeader().transactionSetControlNumber());
            writeGE(writer, inquiry.functionalGroup().controlNumber());
            writeIEA(writer, inquiry.envelope().controlNumber());

            writer.endInterchange();
            writer.close();

            return baos.toString(StandardCharsets.UTF_8);
        } catch (EDIStreamException e) {
            throw new RuntimeException("Failed to generate EDI 270", e);
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
        elem(w, "HS");
        elem(w, fg.senderId());
        elem(w, fg.receiverId());
        elem(w, fg.date());
        elem(w, fg.time());
        elem(w, fg.controlNumber());
        elem(w, "X");
        elem(w, "005010X279A1");
        w.writeEndSegment();
    }

    private int writeST(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("ST");
        elem(w, "270");
        elem(w, th.transactionSetControlNumber());
        elem(w, "005010X279A1");
        w.writeEndSegment();
        return 1;
    }

    private int writeBHT(EDIStreamWriter w, TransactionHeader th) throws EDIStreamException {
        w.writeStartSegment("BHT");
        elem(w, "0022");
        elem(w, "13");
        elem(w, th.referenceId());
        elem(w, th.creationDate());
        elem(w, th.creationTime());
        w.writeEndSegment();
        return 1;
    }

    private int writeHL(EDIStreamWriter w, int hlNumber, int parentHL, String levelCode, String childCode) throws EDIStreamException {
        w.writeStartSegment("HL");
        elem(w, String.valueOf(hlNumber));
        elem(w, parentHL > 0 ? String.valueOf(parentHL) : "");
        elem(w, levelCode);
        elem(w, childCode);
        w.writeEndSegment();
        return 1;
    }

    private int writePayerNM1(EDIStreamWriter w, InformationSourceGroup source) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "PR");
        elem(w, "2");
        elem(w, source.payerName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "PI");
        elem(w, source.payerId());
        w.writeEndSegment();
        return 1;
    }

    private int writeProviderNM1(EDIStreamWriter w, InformationReceiverGroup receiver) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "1P");
        elem(w, "2");
        elem(w, receiver.providerName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "XX");
        elem(w, receiver.providerNpi());
        w.writeEndSegment();
        return 1;
    }

    private int writeProviderREF(EDIStreamWriter w, InformationReceiverGroup receiver) throws EDIStreamException {
        w.writeStartSegment("REF");
        elem(w, "EI");
        elem(w, receiver.providerTaxId());
        w.writeEndSegment();
        return 1;
    }

    private int writeSubscriberNM1(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("NM1");
        elem(w, "IL");
        elem(w, "1");
        elem(w, sub.lastName());
        elem(w, sub.firstName());
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "");
        elem(w, "MI");
        elem(w, sub.memberId());
        w.writeEndSegment();
        return 1;
    }

    private int writeDMG(EDIStreamWriter w, SubscriberLoop sub) throws EDIStreamException {
        w.writeStartSegment("DMG");
        elem(w, "D8");
        elem(w, sub.dateOfBirth());
        elem(w, sub.genderCode());
        w.writeEndSegment();
        return 1;
    }

    private int writeDTP(EDIStreamWriter w, String eligibilityDate) throws EDIStreamException {
        w.writeStartSegment("DTP");
        elem(w, "291");
        elem(w, "D8");
        elem(w, eligibilityDate);
        w.writeEndSegment();
        return 1;
    }

    private int writeEQ(EDIStreamWriter w) throws EDIStreamException {
        w.writeStartSegment("EQ");
        elem(w, "30");
        w.writeEndSegment();
        return 1;
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

    private String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value);
    }
}
