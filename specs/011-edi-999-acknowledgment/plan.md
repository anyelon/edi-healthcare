# EDI 999 Acknowledgment Handler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parse EDI 999 Implementation Acknowledgment files and return structured JSON results via a REST API with a frontend UI.

**Architecture:** Stateless parse-and-return flow added to `insurance-response-app` (port 8082). EDI999Parser follows the xlate event-driven streaming pattern from EDI271Parser. Domain records live in `common` module. Frontend adds an upload page following the eligibility-response page pattern.

**Tech Stack:** Java 21, Spring Boot 4.0.4, staedi (xlate) 1.26.2, Next.js 16, TypeScript, Tailwind CSS, shadcn/ui

---

### Task 1: Sample 999 EDI Test Fixtures

Create sample 999 files for parser tests and manual testing.

**Files:**
- Create: `backend/insurance-response-app/src/test/resources/edi/999_accepted.edi`
- Create: `backend/insurance-response-app/src/test/resources/edi/999_rejected.edi`
- Create: `backend/insurance-response-app/src/test/resources/edi/999_accepted_with_errors.edi`
- Create: `backend/insurance-response-app/src/test/resources/edi/999_multiple_transactions.edi`
- Create: `specs/011-edi-999-acknowledgment/samples/999_accepted.edi`
- Create: `specs/011-edi-999-acknowledgment/samples/999_rejected.edi`
- Create: `specs/011-edi-999-acknowledgment/samples/999_accepted_with_errors.edi`

- [ ] **Step 1: Create accepted 999 sample file**

Create `backend/insurance-response-app/src/test/resources/edi/999_accepted.edi`:

```
ISA*00*          *00*          *ZZ*CLEARINGHOUSE01*ZZ*SENDER12345    *260401*1200*^*00501*000000001*0*T*:~
GS*FA*CLEARINGHOUSE01*SENDER12345*20260401*1200*1*X*005010X231A1~
ST*999*0001*005010X231A1~
AK1*HC*1~
AK2*837*0001~
IK5*A~
AK9*A*1*1*1~
SE*7*0001~
GE*1*1~
IEA*1*000000001~
```

- [ ] **Step 2: Create rejected 999 sample file with errors**

Create `backend/insurance-response-app/src/test/resources/edi/999_rejected.edi`:

```
ISA*00*          *00*          *ZZ*CLEARINGHOUSE01*ZZ*SENDER12345    *260401*1200*^*00501*000000002*0*T*:~
GS*FA*CLEARINGHOUSE01*SENDER12345*20260401*1200*2*X*005010X231A1~
ST*999*0002*005010X231A1~
AK1*HC*1~
AK2*837*0001~
IK3*CLM*22**8~
IK4*1*782*1~
IK3*NM1*35**5~
IK5*R~
AK9*R*1*1*0~
SE*10*0002~
GE*1*2~
IEA*1*000000002~
```

- [ ] **Step 3: Create accepted-with-errors 999 sample file**

Create `backend/insurance-response-app/src/test/resources/edi/999_accepted_with_errors.edi`:

```
ISA*00*          *00*          *ZZ*CLEARINGHOUSE01*ZZ*SENDER12345    *260401*1200*^*00501*000000003*0*T*:~
GS*FA*CLEARINGHOUSE01*SENDER12345*20260401*1200*3*X*005010X231A1~
ST*999*0003*005010X231A1~
AK1*HC*1~
AK2*837*0001~
IK3*REF*10**3~
IK5*E~
AK9*E*1*1*1~
SE*8*0003~
GE*1*3~
IEA*1*000000003~
```

- [ ] **Step 4: Create multiple-transactions 999 sample file**

Create `backend/insurance-response-app/src/test/resources/edi/999_multiple_transactions.edi`:

```
ISA*00*          *00*          *ZZ*CLEARINGHOUSE01*ZZ*SENDER12345    *260401*1200*^*00501*000000004*0*T*:~
GS*FA*CLEARINGHOUSE01*SENDER12345*20260401*1200*4*X*005010X231A1~
ST*999*0004*005010X231A1~
AK1*HC*1~
AK2*837*0001~
IK5*A~
AK2*837*0002~
IK3*CLM*22**8~
IK5*R~
AK9*P*2*2*1~
SE*10*0004~
GE*1*4~
IEA*1*000000004~
```

- [ ] **Step 5: Copy sample files to specs folder for manual testing**

Copy the accepted, rejected, and accepted-with-errors samples to `specs/011-edi-999-acknowledgment/samples/`.

- [ ] **Step 6: Commit**

```bash
git add backend/insurance-response-app/src/test/resources/edi/ specs/011-edi-999-acknowledgment/samples/
git commit -m "test: add sample EDI 999 files for parser tests and manual testing"
```

---

### Task 2: Domain Model Records

Create the enums and records in the `common` module.

**Files:**
- Create: `backend/common/src/main/java/com/example/edi/common/edi/ack/TransactionSetStatus.java`
- Create: `backend/common/src/main/java/com/example/edi/common/edi/ack/FunctionalGroupStatus.java`
- Create: `backend/common/src/main/java/com/example/edi/common/edi/ack/SegmentError.java`
- Create: `backend/common/src/main/java/com/example/edi/common/edi/ack/EDI999Acknowledgment.java`

- [ ] **Step 1: Create TransactionSetStatus enum**

Create `backend/common/src/main/java/com/example/edi/common/edi/ack/TransactionSetStatus.java`:

```java
package com.example.edi.common.edi.ack;

public enum TransactionSetStatus {
    ACCEPTED("A"),
    ACCEPTED_WITH_ERRORS("E"),
    REJECTED("R");

    private final String code;

    TransactionSetStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionSetStatus fromCode(String code) {
        return switch (code) {
            case "A" -> ACCEPTED;
            case "E" -> ACCEPTED_WITH_ERRORS;
            case "R" -> REJECTED;
            default -> throw new IllegalArgumentException("Unknown transaction set status code: " + code);
        };
    }
}
```

- [ ] **Step 2: Create FunctionalGroupStatus enum**

Create `backend/common/src/main/java/com/example/edi/common/edi/ack/FunctionalGroupStatus.java`:

```java
package com.example.edi.common.edi.ack;

public enum FunctionalGroupStatus {
    ACCEPTED("A"),
    ACCEPTED_WITH_ERRORS("E"),
    REJECTED("R"),
    PARTIALLY_ACCEPTED("P");

    private final String code;

    FunctionalGroupStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FunctionalGroupStatus fromCode(String code) {
        return switch (code) {
            case "A" -> ACCEPTED;
            case "E" -> ACCEPTED_WITH_ERRORS;
            case "R" -> REJECTED;
            case "P" -> PARTIALLY_ACCEPTED;
            default -> throw new IllegalArgumentException("Unknown functional group status code: " + code);
        };
    }
}
```

- [ ] **Step 3: Create SegmentError record**

Create `backend/common/src/main/java/com/example/edi/common/edi/ack/SegmentError.java`:

```java
package com.example.edi.common.edi.ack;

public record SegmentError(
    String segmentId,
    int segmentPosition,
    String segmentErrorCode,
    int elementPosition,
    String elementErrorCode,
    String errorDescription
) {}
```

- [ ] **Step 4: Create EDI999Acknowledgment record**

Create `backend/common/src/main/java/com/example/edi/common/edi/ack/EDI999Acknowledgment.java`:

```java
package com.example.edi.common.edi.ack;

import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import java.util.List;

public record EDI999Acknowledgment(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    String acknowledgedGroupControlNumber,
    String acknowledgedTransactionSetId,
    String acknowledgedTransactionControlNumber,
    TransactionSetStatus transactionStatus,
    FunctionalGroupStatus groupStatus,
    int transactionSetsIncluded,
    int transactionSetsReceived,
    int transactionSetsAccepted,
    List<SegmentError> errors
) {}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add backend/common/src/main/java/com/example/edi/common/edi/ack/
git commit -m "feat: add EDI 999 domain model records and enums"
```

---

### Task 3: EDI999Parser — Failing Tests

Write parser tests first (TDD red phase).

**Files:**
- Create: `backend/insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI999ParserTest.java`

- [ ] **Step 1: Write parser test class with all test cases**

Create `backend/insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI999ParserTest.java`:

```java
package com.example.edi.insuranceresponse.service;

import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.common.edi.ack.FunctionalGroupStatus;
import com.example.edi.common.edi.ack.TransactionSetStatus;
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

    private InputStream loadResource(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Test resource not found: " + path);
        }
        return stream;
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :insurance-response-app:test --tests "com.example.edi.insuranceresponse.service.EDI999ParserTest" --info`
Expected: FAIL (EDI999Parser class does not exist yet)

- [ ] **Step 3: Commit failing tests**

```bash
git add backend/insurance-response-app/src/test/java/com/example/edi/insuranceresponse/service/EDI999ParserTest.java
git commit -m "test: add failing EDI 999 parser tests (TDD red phase)"
```

---

### Task 4: EDI999Parser — Implementation

Implement the parser to make all tests pass (TDD green phase).

**Files:**
- Create: `backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI999Parser.java`

- [ ] **Step 1: Implement EDI999Parser**

Create `backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI999Parser.java`:

```java
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
import io.xlate.edi.stream.EDIStreamReader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EDI999Parser {

    private static final Map<String, String> SEGMENT_ERROR_DESCRIPTIONS = Map.ofEntries(
        Map.entry("1", "Unrecognized Segment ID"),
        Map.entry("2", "Unexpected Segment"),
        Map.entry("3", "Required Segment Missing"),
        Map.entry("4", "Loop Occurs Over Maximum Times"),
        Map.entry("5", "Segment Exceeds Maximum Use"),
        Map.entry("6", "Segment Not in Defined Transaction Set"),
        Map.entry("7", "Segment Not in Proper Sequence"),
        Map.entry("8", "Segment Has Data Element Errors")
    );

    private static final Map<String, String> ELEMENT_ERROR_DESCRIPTIONS = Map.ofEntries(
        Map.entry("1", "Required Data Element Missing"),
        Map.entry("2", "Conditional Required Data Element Missing"),
        Map.entry("3", "Too Many Data Elements"),
        Map.entry("4", "Data Element Too Short"),
        Map.entry("5", "Data Element Too Long"),
        Map.entry("6", "Invalid Character in Data Element"),
        Map.entry("7", "Invalid Code Value"),
        Map.entry("8", "Invalid Date"),
        Map.entry("9", "Invalid Time"),
        Map.entry("10", "Exclusion Condition Violated")
    );

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

            // AK1: acknowledged group
            String acknowledgedGroupControlNumber = "";

            // Current transaction (AK2 scope)
            String currentTransactionSetId = "";
            String currentTransactionControlNumber = "";
            List<SegmentError> currentErrors = new ArrayList<>();

            // Current IK3 scope
            String ik3SegmentId = "";
            int ik3SegmentPosition = 0;
            String ik3ErrorCode = "";
            int ik4ElementPosition = 0;
            String ik4ErrorCode = "";
            boolean hasIk4 = false;
            boolean hasIk3 = false;

            // AK9: group-level results
            FunctionalGroupStatus groupStatus = null;
            int transactionSetsIncluded = 0;
            int transactionSetsReceived = 0;
            int transactionSetsAccepted = 0;

            // Collected per-transaction results
            List<String> txSetIds = new ArrayList<>();
            List<String> txControlNumbers = new ArrayList<>();
            List<TransactionSetStatus> txStatuses = new ArrayList<>();
            List<List<SegmentError>> txErrors = new ArrayList<>();

            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();
                switch (event) {
                    case START_SEGMENT -> {
                        currentSegment = reader.getText();
                        elementPosition = 0;

                        if ("AK2".equals(currentSegment)) {
                            currentErrors = new ArrayList<>();
                            currentTransactionSetId = "";
                            currentTransactionControlNumber = "";
                            hasIk3 = false;
                            hasIk4 = false;
                        }
                        if ("IK3".equals(currentSegment)) {
                            if (hasIk3 && !hasIk4) {
                                finalizeIk3(currentErrors, ik3SegmentId, ik3SegmentPosition,
                                        ik3ErrorCode, false, 0, "");
                            }
                            ik3SegmentId = "";
                            ik3SegmentPosition = 0;
                            ik3ErrorCode = "";
                            ik4ElementPosition = 0;
                            ik4ErrorCode = "";
                            hasIk4 = false;
                            hasIk3 = true;
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
                                if (elementPosition == 2) {
                                    acknowledgedGroupControlNumber = value;
                                }
                            }
                            case "AK2" -> {
                                switch (elementPosition) {
                                    case 1 -> currentTransactionSetId = value;
                                    case 2 -> currentTransactionControlNumber = value;
                                }
                            }
                            case "IK3" -> {
                                switch (elementPosition) {
                                    case 1 -> ik3SegmentId = value;
                                    case 2 -> ik3SegmentPosition = parseIntSafe(value);
                                    case 4 -> ik3ErrorCode = value;
                                }
                            }
                            case "IK4" -> {
                                switch (elementPosition) {
                                    case 1 -> ik4ElementPosition = parseIntSafe(value);
                                    case 3 -> ik4ErrorCode = value;
                                }
                                hasIk4 = true;
                            }
                            case "IK5" -> {
                                if (elementPosition == 1) {
                                    if (hasIk3) {
                                        finalizeIk3(currentErrors, ik3SegmentId, ik3SegmentPosition,
                                                ik3ErrorCode, hasIk4, ik4ElementPosition, ik4ErrorCode);
                                    }
                                    txSetIds.add(currentTransactionSetId);
                                    txControlNumbers.add(currentTransactionControlNumber);
                                    txStatuses.add(TransactionSetStatus.fromCode(value));
                                    txErrors.add(new ArrayList<>(currentErrors));
                                }
                            }
                            case "AK9" -> {
                                switch (elementPosition) {
                                    case 1 -> groupStatus = FunctionalGroupStatus.fromCode(value);
                                    case 2 -> transactionSetsIncluded = parseIntSafe(value);
                                    case 3 -> transactionSetsReceived = parseIntSafe(value);
                                    case 4 -> transactionSetsAccepted = parseIntSafe(value);
                                }
                            }
                        }
                    }
                    case END_SEGMENT -> {
                        if ("IK4".equals(currentSegment)) {
                            finalizeIk3(currentErrors, ik3SegmentId, ik3SegmentPosition,
                                    ik3ErrorCode, true, ik4ElementPosition, ik4ErrorCode);
                            ik3SegmentId = "";
                            ik3SegmentPosition = 0;
                            ik3ErrorCode = "";
                            ik4ElementPosition = 0;
                            ik4ErrorCode = "";
                            hasIk4 = false;
                            hasIk3 = false;
                        }
                    }
                    default -> {}
                }
            }
            reader.close();

            var envelope = new InterchangeEnvelope(isaSenderQualifier, isaSenderId,
                    isaReceiverQualifier, isaReceiverId, isaDate, isaTime,
                    isaControlNumber, isaAckRequested, isaUsageIndicator);
            var functionalGroup = new FunctionalGroup(gsSenderId, gsReceiverId,
                    gsDate, gsTime, gsControlNumber);

            List<EDI999Acknowledgment> results = new ArrayList<>();
            for (int i = 0; i < txStatuses.size(); i++) {
                results.add(new EDI999Acknowledgment(
                        envelope, functionalGroup, acknowledgedGroupControlNumber,
                        txSetIds.get(i), txControlNumbers.get(i),
                        txStatuses.get(i), groupStatus,
                        transactionSetsIncluded, transactionSetsReceived,
                        transactionSetsAccepted, txErrors.get(i)));
            }
            return results;

        } catch (EdiParseException e) {
            throw e;
        } catch (Exception e) {
            throw new EdiParseException("Failed to parse EDI 999 file", e);
        }
    }

    private void finalizeIk3(List<SegmentError> errors, String segmentId,
                              int segmentPosition, String segmentErrorCode,
                              boolean hasIk4, int elementPosition,
                              String elementErrorCode) {
        if (segmentId.isEmpty()) return;

        String segDesc = SEGMENT_ERROR_DESCRIPTIONS.getOrDefault(segmentErrorCode, "Unknown Error");
        String elemCode = hasIk4 ? elementErrorCode : null;
        String elemDesc = hasIk4
                ? ELEMENT_ERROR_DESCRIPTIONS.getOrDefault(elementErrorCode, "Unknown Element Error")
                : null;
        String description = hasIk4 ? segDesc + " - " + elemDesc : segDesc;

        errors.add(new SegmentError(segmentId, segmentPosition, segmentErrorCode,
                hasIk4 ? elementPosition : 0, elemCode, description));
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :insurance-response-app:test --tests "com.example.edi.insuranceresponse.service.EDI999ParserTest" --info`
Expected: ALL PASS

- [ ] **Step 3: Commit**

```bash
git add backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI999Parser.java
git commit -m "feat: implement EDI 999 parser with xlate streaming"
```

---

### Task 5: EDI999Service

Thin service layer to accept MultipartFile and delegate to parser.

**Files:**
- Create: `backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI999Service.java`

- [ ] **Step 1: Create EDI999Service**

Create `backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI999Service.java`:

```java
package com.example.edi.insuranceresponse.service;

import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class EDI999Service {

    private final EDI999Parser edi999Parser;

    public EDI999Service(EDI999Parser edi999Parser) {
        this.edi999Parser = edi999Parser;
    }

    public List<EDI999Acknowledgment> processFile(MultipartFile file) throws IOException {
        return edi999Parser.parse(file.getInputStream());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :insurance-response-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/service/EDI999Service.java
git commit -m "feat: add EDI999Service to orchestrate 999 file parsing"
```

---

### Task 6: Controller — Failing Test

Write the controller test first (TDD red phase).

**Files:**
- Modify: `backend/insurance-response-app/src/test/java/com/example/edi/insuranceresponse/controller/InsuranceResponseControllerTest.java`

- [ ] **Step 1: Add 999 acknowledgment controller tests**

Add these imports to the existing file:

```java
import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.common.edi.ack.FunctionalGroupStatus;
import com.example.edi.common.edi.ack.TransactionSetStatus;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.insuranceresponse.service.EDI999Service;
import java.util.List;
```

Add field after the existing `@MockitoBean`:

```java
@MockitoBean
private EDI999Service edi999Service;
```

Add test methods:

```java
@Test
void processAcknowledgment_validFile_returns200() throws Exception {
    var envelope = new InterchangeEnvelope("ZZ", "CLEARINGHOUSE01", "ZZ", "SENDER12345",
            "260401", "1200", "000000001", "0", "T");
    var group = new FunctionalGroup("CLEARINGHOUSE01", "SENDER12345", "20260401", "1200", "1");
    var ack = new EDI999Acknowledgment(envelope, group, "1", "837", "0001",
            TransactionSetStatus.ACCEPTED, FunctionalGroupStatus.ACCEPTED,
            1, 1, 1, List.of());

    when(edi999Service.processFile(any())).thenReturn(List.of(ack));

    MockMultipartFile file = new MockMultipartFile("file", "999_ack.edi",
            "text/plain", "AK1*HC*1~".getBytes());

    mockMvc.perform(multipart("/api/insurance/acknowledgment").file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$[0].transactionStatus").value("ACCEPTED"))
            .andExpect(jsonPath("$[0].acknowledgedTransactionSetId").value("837"));
}

@Test
void processAcknowledgment_parseError_returns400() throws Exception {
    when(edi999Service.processFile(any()))
            .thenThrow(new EdiParseException("Failed to parse EDI 999 file", null));

    MockMultipartFile file = new MockMultipartFile("file", "bad_999.edi",
            "text/plain", "INVALID EDI".getBytes());

    mockMvc.perform(multipart("/api/insurance/acknowledgment").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :insurance-response-app:test --tests "com.example.edi.insuranceresponse.controller.InsuranceResponseControllerTest" --info`
Expected: FAIL (endpoint does not exist)

- [ ] **Step 3: Commit**

```bash
git add backend/insurance-response-app/src/test/java/com/example/edi/insuranceresponse/controller/InsuranceResponseControllerTest.java
git commit -m "test: add failing controller tests for 999 acknowledgment endpoint"
```

---

### Task 7: Controller — Implementation

Add the new endpoint to the existing controller (TDD green phase).

**Files:**
- Modify: `backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/controller/InsuranceResponseController.java`

- [ ] **Step 1: Update InsuranceResponseController**

Replace the full file content with:

```java
package com.example.edi.insuranceresponse.controller;

import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import com.example.edi.insuranceresponse.service.EDI999Service;
import com.example.edi.insuranceresponse.service.EligibilityResponseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceResponseController {

    private final EligibilityResponseService eligibilityResponseService;
    private final EDI999Service edi999Service;

    public InsuranceResponseController(EligibilityResponseService eligibilityResponseService,
                                       EDI999Service edi999Service) {
        this.eligibilityResponseService = eligibilityResponseService;
        this.edi999Service = edi999Service;
    }

    @PostMapping(value = "/eligibility-response", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EligibilityResponse> processEligibilityResponse(
            @RequestParam("file") MultipartFile file) throws Exception {
        EligibilityResponse result = eligibilityResponseService.processFile(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/acknowledgment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EDI999Acknowledgment>> processAcknowledgment(
            @RequestParam("file") MultipartFile file) throws Exception {
        List<EDI999Acknowledgment> results = edi999Service.processFile(file);
        return ResponseEntity.ok(results);
    }
}
```

- [ ] **Step 2: Run controller tests to verify they pass**

Run: `./gradlew :insurance-response-app:test --tests "com.example.edi.insuranceresponse.controller.InsuranceResponseControllerTest" --info`
Expected: ALL PASS

- [ ] **Step 3: Run all insurance-response-app tests**

Run: `./gradlew :insurance-response-app:test --info`
Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add backend/insurance-response-app/src/main/java/com/example/edi/insuranceresponse/controller/InsuranceResponseController.java
git commit -m "feat: add POST /api/insurance/acknowledgment endpoint for EDI 999"
```

---

### Task 8: Frontend — Types and API Client

Add TypeScript interfaces and API client function.

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/lib/api-client.ts`

- [ ] **Step 1: Add 999 types to frontend**

Add to the end of `frontend/src/types/index.ts`:

```typescript
export interface SegmentError {
  segmentId: string;
  segmentPosition: number;
  segmentErrorCode: string;
  elementPosition: number;
  elementErrorCode: string | null;
  errorDescription: string;
}

export interface EDI999Acknowledgment {
  envelope: {
    senderIdQualifier: string;
    senderId: string;
    receiverIdQualifier: string;
    receiverId: string;
    date: string;
    time: string;
    controlNumber: string;
    ackRequested: string;
    usageIndicator: string;
  };
  functionalGroup: {
    senderId: string;
    receiverId: string;
    date: string;
    time: string;
    controlNumber: string;
  };
  acknowledgedGroupControlNumber: string;
  acknowledgedTransactionSetId: string;
  acknowledgedTransactionControlNumber: string;
  transactionStatus: "ACCEPTED" | "ACCEPTED_WITH_ERRORS" | "REJECTED";
  groupStatus: "ACCEPTED" | "ACCEPTED_WITH_ERRORS" | "REJECTED" | "PARTIALLY_ACCEPTED";
  transactionSetsIncluded: number;
  transactionSetsReceived: number;
  transactionSetsAccepted: number;
  errors: SegmentError[];
}
```

- [ ] **Step 2: Add API client function**

Update the import line in `frontend/src/lib/api-client.ts`:

```typescript
import type { SeedResult, EligibilityResponse, EncounterResponse, PatientResponse, PriorAuthResponse, EDI999Acknowledgment } from "@/types";
```

Add this function before `downloadBlob`:

```typescript
export async function parseAcknowledgment(
  file: File
): Promise<EDI999Acknowledgment[]> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch("/api/insurance/acknowledgment", {
    method: "POST",
    body: formData,
  });
  if (!res.ok)
    throw new Error(`Acknowledgment parsing failed: ${res.statusText}`);
  return res.json();
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/lib/api-client.ts
git commit -m "feat: add EDI 999 TypeScript types and API client function"
```

---

### Task 9: Frontend — BFF API Route

Create the Next.js API route that proxies to the backend.

**Files:**
- Create: `frontend/src/app/api/insurance/acknowledgment/route.ts`

- [ ] **Step 1: Create BFF route**

Create `frontend/src/app/api/insurance/acknowledgment/route.ts`:

```typescript
const RESPONSE_API = process.env.RESPONSE_API_URL || "http://localhost:8082";

export async function POST(request: Request) {
  const formData = await request.formData();
  const response = await fetch(
    `${RESPONSE_API}/api/insurance/acknowledgment`,
    {
      method: "POST",
      body: formData,
    }
  );
  const data = await response.json();
  return Response.json(data, { status: response.status });
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/api/insurance/acknowledgment/route.ts
git commit -m "feat: add BFF API route for EDI 999 acknowledgment"
```

---

### Task 10: Frontend — Acknowledgment Page

Create the UI page with file upload and results display.

**Files:**
- Create: `frontend/src/app/acknowledgment/page.tsx`

- [ ] **Step 1: Create acknowledgment page**

Create `frontend/src/app/acknowledgment/page.tsx`:

```tsx
"use client";

import { useState, useCallback } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Dropzone } from "@/components/dropzone";
import { parseAcknowledgment } from "@/lib/api-client";
import type { EDI999Acknowledgment } from "@/types";

function statusVariant(status: string) {
  switch (status) {
    case "ACCEPTED":
      return "default";
    case "ACCEPTED_WITH_ERRORS":
      return "outline";
    case "REJECTED":
      return "destructive";
    case "PARTIALLY_ACCEPTED":
      return "outline";
    default:
      return "secondary";
  }
}

function statusLabel(status: string) {
  return status.replace(/_/g, " ");
}

export default function AcknowledgmentPage() {
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<EDI999Acknowledgment[]>([]);

  const handleFile = useCallback(async (file: File) => {
    setLoading(true);
    setResults([]);
    try {
      const data = await parseAcknowledgment(file);
      setResults(data);
      toast.success(
        "EDI 999 parsed: " + data.length + " transaction(s) found."
      );
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to parse acknowledgment"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Acknowledgment</h1>
        <Badge variant="outline">EDI 999</Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">
              Upload EDI 999 File
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Dropzone onFileSelect={handleFile} />
            {loading && (
              <p className="mt-3 text-sm text-muted-foreground">
                Parsing acknowledgment...
              </p>
            )}
          </CardContent>
        </Card>

        {results.length > 0 && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-semibold">
                  Group Summary
                </CardTitle>
                <Badge variant={statusVariant(results[0].groupStatus)}>
                  {statusLabel(results[0].groupStatus)}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-4 text-sm">
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Included
                  </p>
                  <p className="mt-1 text-lg font-semibold">
                    {results[0].transactionSetsIncluded}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Received
                  </p>
                  <p className="mt-1 text-lg font-semibold">
                    {results[0].transactionSetsReceived}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Accepted
                  </p>
                  <p className="mt-1 text-lg font-semibold">
                    {results[0].transactionSetsAccepted}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {results.map((ack, i) => (
        <Card key={i} className="mt-6">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-semibold">
                Transaction {ack.acknowledgedTransactionSetId} — Control #
                {ack.acknowledgedTransactionControlNumber}
              </CardTitle>
              <Badge variant={statusVariant(ack.transactionStatus)}>
                {statusLabel(ack.transactionStatus)}
              </Badge>
            </div>
          </CardHeader>
          {ack.errors.length > 0 && (
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Segment</TableHead>
                    <TableHead>Position</TableHead>
                    <TableHead>Segment Error</TableHead>
                    <TableHead>Element Position</TableHead>
                    <TableHead>Element Error</TableHead>
                    <TableHead>Description</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {ack.errors.map((err, j) => (
                    <TableRow key={j}>
                      <TableCell className="font-mono">
                        {err.segmentId}
                      </TableCell>
                      <TableCell>{err.segmentPosition}</TableCell>
                      <TableCell className="font-mono">
                        {err.segmentErrorCode}
                      </TableCell>
                      <TableCell>{err.elementPosition || "-"}</TableCell>
                      <TableCell className="font-mono">
                        {err.elementErrorCode || "-"}
                      </TableCell>
                      <TableCell>{err.errorDescription}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          )}
        </Card>
      ))}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/acknowledgment/page.tsx
git commit -m "feat: add acknowledgment page with file upload and results display"
```

---

### Task 11: Frontend — Navigation

Add the Acknowledgment link to the sidebar.

**Files:**
- Modify: `frontend/src/components/app-sidebar.tsx`

- [ ] **Step 1: Add sidebar entry**

Add `CheckCircle` to the lucide-react import in `frontend/src/components/app-sidebar.tsx`:

```typescript
import {
  LayoutDashboard,
  FileText,
  ArrowRightLeft,
  FileSearch,
  Database,
  ShieldCheck,
  CheckCircle,
} from "lucide-react";
```

Add entry to the `workflowItems` array after the prior-auth item:

```typescript
{ href: "/acknowledgment", label: "Acknowledgment (999)", icon: CheckCircle },
```

The full array becomes:

```typescript
const workflowItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/claims", label: "Claims (837)", icon: FileText },
  { href: "/eligibility-request", label: "Eligibility Request (270)", icon: ArrowRightLeft },
  { href: "/eligibility-response", label: "Eligibility Response (271)", icon: FileSearch },
  { href: "/prior-auth", label: "Prior Auth (278)", icon: ShieldCheck },
  { href: "/acknowledgment", label: "Acknowledgment (999)", icon: CheckCircle },
];
```

- [ ] **Step 2: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/app-sidebar.tsx
git commit -m "feat: add Acknowledgment (999) to sidebar navigation"
```

---

### Task 12: End-to-End Validation

Start the backend and frontend, upload a sample 999 file, and verify the full flow using Chrome DevTools.

**Files:** None (manual validation)

- [ ] **Step 1: Start the backend**

Run: `./gradlew :insurance-response-app:bootRun`
Expected: Application starts on port 8082.

- [ ] **Step 2: Start the frontend**

Run: `cd frontend && npm run dev`
Expected: Next.js dev server starts on port 3000.

- [ ] **Step 3: Validate with Chrome DevTools**

1. Navigate to `http://localhost:3000/acknowledgment`
2. Verify the page renders with "Acknowledgment" title and "EDI 999" badge
3. Verify the Dropzone component appears
4. Upload `specs/011-edi-999-acknowledgment/samples/999_accepted.edi`
5. Verify: one transaction card with "ACCEPTED" green badge, group summary shows 1/1/1
6. Upload `specs/011-edi-999-acknowledgment/samples/999_rejected.edi`
7. Verify: one transaction card with "REJECTED" red badge, error table with CLM and NM1 rows
8. Upload `specs/011-edi-999-acknowledgment/samples/999_accepted_with_errors.edi`
9. Verify: one transaction card with "ACCEPTED WITH ERRORS" badge, error table with REF row
10. Verify sidebar shows "Acknowledgment (999)" link and it is highlighted when active

- [ ] **Step 4: Final commit (if any fixes needed)**

Fix any issues found during validation, run tests again, then commit.
