# EDI 999 Acknowledgment Handler

**Issue:** [anyelon/edi-healthcare#32](https://github.com/anyelon/edi-healthcare/issues/32)
**Date:** 2026-04-09

## Overview

Parse EDI 999 Implementation Acknowledgment files to determine whether outbound EDI transmissions (837s, 276s) were accepted by the clearinghouse/gateway. The handler is stateless — it parses a 999 file and returns structured results with no database persistence.

Lives in the existing `insurance-response-app` (port 8082) alongside the EDI 271 parser.

## Domain Model

New records in `com.example.edi.common.edi.ack` package:

### EDI999Acknowledgment

Represents a single transaction set acknowledgment extracted from a 999 file. One 999 file may contain multiple acknowledgments.

| Field | Type | Source |
|-------|------|--------|
| envelope | InterchangeEnvelope (reused) | ISA segment |
| functionalGroup | FunctionalGroup (reused) | GS segment |
| acknowledgedGroupControlNumber | String | AK1-02 |
| acknowledgedTransactionSetId | String | AK2-01 (e.g. "837") |
| acknowledgedTransactionControlNumber | String | AK2-02 |
| transactionStatus | TransactionSetStatus | IK5-01 |
| groupStatus | FunctionalGroupStatus | AK9-01 |
| transactionSetsIncluded | int | AK9-02 |
| transactionSetsReceived | int | AK9-03 |
| transactionSetsAccepted | int | AK9-04 |
| errors | List\<SegmentError\> | IK3/IK4 segments |

### TransactionSetStatus (enum)

| Value | EDI Code | Meaning |
|-------|----------|---------|
| ACCEPTED | A | Transaction accepted |
| ACCEPTED_WITH_ERRORS | E | Accepted with errors |
| REJECTED | R | Transaction rejected |

### FunctionalGroupStatus (enum)

| Value | EDI Code | Meaning |
|-------|----------|---------|
| ACCEPTED | A | Group accepted |
| ACCEPTED_WITH_ERRORS | E | Accepted with errors |
| REJECTED | R | Group rejected |
| PARTIALLY_ACCEPTED | P | Partially accepted |

### SegmentError

| Field | Type | Source |
|-------|------|--------|
| segmentId | String | IK3-01 |
| segmentPosition | int | IK3-02 |
| segmentErrorCode | String | IK3-04 |
| elementPosition | int | IK4-01 (0 if N/A) |
| elementErrorCode | String (nullable) | IK4-03 |
| errorDescription | String | Human-readable mapped from code |

## Parser (EDI999Parser)

New `@Service` in `insurance-response-app` following the xlate event-driven streaming pattern established by `EDI271Parser`.

**Parsing flow:**
1. Read ISA/GS envelope (same position-based extraction as existing parsers)
2. AK1 segment — capture acknowledged functional group control number
3. AK2 segment — start new transaction acknowledgment context
4. IK3 segments — accumulate segment-level errors
5. IK4 segments — attach element-level error detail to current IK3 error
6. IK5 segment — finalize transaction set status, close current acknowledgment
7. AK9 segment — capture functional group status and counts
8. On each AK2→IK5 cycle, emit one `EDI999Acknowledgment` into the result list

**Error code mapping:** IK3-04 and IK4-03 codes mapped to human-readable descriptions (e.g., code "8" → "Segment Has Data Element Errors").

**Returns:** `List<EDI999Acknowledgment>` — one entry per acknowledged transaction set in the file. Each entry carries the group-level data (AK9 status and counts) alongside its transaction-level data (IK5), since there's no separate grouping structure. All acknowledgments from the same functional group share the same AK9 values.

## Service (EDI999Service)

Thin orchestrator — accepts `MultipartFile`, passes input stream to `EDI999Parser`, returns the parsed list directly. No file archival, no database persistence.

## Controller

New endpoint added to `InsuranceResponseController`:

```
POST /api/insurance/acknowledgment
Content-Type: multipart/form-data
Parameter: file (MultipartFile)

Response: 200 OK
Body: List<EDI999Acknowledgment> (JSON)

Error: 400 Bad Request (via existing GlobalExceptionHandler for EdiParseException)
```

## Frontend

### BFF API Route

`frontend/src/app/api/insurance/acknowledgment/route.ts`

Proxies POST with FormData to `RESPONSE_API_URL/api/insurance/acknowledgment`. Same pattern as the eligibility-response route, same `RESPONSE_API` base URL.

### Page

`frontend/src/app/acknowledgment/page.tsx`

**Layout:**
- Header: "Acknowledgment" title with "EDI 999" badge
- Left column: Dropzone for file upload (reuses existing `<Dropzone>` component)
- Right column / below: one card per acknowledgment result

**Per-acknowledgment card:**
- Header row: transaction set ID (e.g. "837") + control number, status badge
  - Green badge for Accepted
  - Yellow/warning badge for Accepted with Errors
  - Red/destructive badge for Rejected
- Summary grid: group status, transactions included/received/accepted
- Error table (shown only when errors exist):
  - Columns: Segment ID, Position, Segment Error, Element Position, Element Error, Description

### Types

Add to `frontend/src/types`:
- `EDI999Acknowledgment` interface
- `SegmentError` interface

### API Client

Add `parseAcknowledgment(file: File): Promise<EDI999Acknowledgment[]>` to the API client module.

### Navigation

Add "Acknowledgment" link to sidebar navigation.

## Testing

### EDI999ParserTest

Unit tests with sample 999 EDI files as test resources:
- Accepted transaction (AK9-01=A, IK5-01=A)
- Rejected transaction (IK5-01=R) with IK3/IK4 error segments
- Multiple transaction sets in one 999 file
- Accepted with errors (IK5-01=E)

### EDI999ControllerTest

`@WebMvcTest` slice test with `@MockitoBean` for the service:
- 200 OK with valid file upload returning JSON list
- 400 Bad Request for unparseable input

### Frontend

Manual testing (consistent with existing pages — no frontend test infrastructure).
