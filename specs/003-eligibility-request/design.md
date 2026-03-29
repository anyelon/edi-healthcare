# EDI 270 Eligibility Request Generation

## Overview

Rewrite the `insurance-request-app` to generate EDI 270 5010 eligibility inquiry files for multiple patients in a single batch file. The app accepts a list of patient IDs, groups subscribers by payer, and produces a valid 005010X279A1 EDI 270 file using StAEDI.

## Decisions

### ADR-001: Use StAEDI for EDI 270 Generation

Replace the existing manual string-concatenation approach with StAEDI (`io.xlate:staedi`), consistent with the claims-app's EDI 837 generator. StAEDI handles ISA padding, segment separators, and segment counting automatically, reducing error surface.

### ADR-002: Group Subscribers by Payer

Multiple patients in a single request are grouped by payer in the output file. Each unique payer gets one Information Source (HL) loop containing an Information Receiver (provider) loop, with subscriber loops nested underneath. This is the standard batch 270 structure.

### ADR-003: Three-Layer Architecture

Follow the claims-app pattern: Mapper (entities → loop records) → Generator (loop records → EDI via StAEDI) → Service (orchestrator with repo access). Each layer is independently testable.

### ADR-004: Reuse Claims-App Seed Data

Both apps share the same MongoDB database (`edi_healthcare`). No seed endpoint needed in the insurance-request-app — use the claims-app's `/api/dev/seed` endpoint.

## EDI 270 5010 File Structure

```
ISA (Interchange Control Header)
  GS (Functional Group Header — "HS", version "005010X279A1")
    ST (Transaction Set Header — "270")
    BHT (Beginning — "0022", purpose "13" = Request)
      HL*1 (Information Source — Payer A)
        NM1*PR (Payer name + payer ID)
        HL*2 (Information Receiver — Practice/Provider)
          NM1*1P (Provider name + NPI)
          REF*EI (Tax ID)
          HL*3 (Subscriber — Patient 1)
            NM1*IL (Insured name + member ID)
            DMG (DOB, gender)
            DTP*291*D8 (Eligibility date — today)
            EQ*30 (Health Benefit Plan Coverage)
          HL*4 (Subscriber — Patient 2)
            NM1*IL, DMG, DTP, EQ
      HL*5 (Information Source — Payer B)
        NM1*PR
        HL*6 (Information Receiver — same Provider)
          NM1*1P, REF*EI
          HL*7 (Subscriber — Patient 3)
            NM1*IL, DMG, DTP, EQ
    SE (Transaction Set Trailer)
  GE (Functional Group Trailer)
IEA (Interchange Control Trailer)
```

- HL counter is global and sequential across the entire file
- Each HL segment includes a parent ID: Information Source has no parent, Information Receiver references its Information Source, Subscriber references its Information Receiver (e.g., `HL*3*2*22*0` means HL #3, parent is HL #2, level code 22, no children)
- HL level codes: 20 = Information Source, 21 = Information Receiver, 22 = Subscriber
- Each payer group has its own Information Source → Information Receiver → Subscriber chain
- The provider (practice) is repeated under each payer group (HL is strictly hierarchical)
- One transaction set per file (single ST/SE)
- Eligibility inquiry is always code 30 (Health Benefit Plan Coverage)
- Eligibility date is always the current date

## Loop Records

### Reused from common module

- `InterchangeEnvelope` — ISA segment fields
- `FunctionalGroup` — GS segment fields
- `TransactionHeader` — ST/BHT segment fields
- `SubscriberLoop` — NM1*IL, DMG subscriber fields (name, member ID, DOB, gender, address)

### New 270-specific records

Located in `insurance-request-app/src/main/java/com/example/edi/insurancerequest/domain/loop/`:

```java
// Root container for the entire 270 file
record EDI270Inquiry(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    List<InformationSourceGroup> informationSourceGroups
)

// One per payer — groups payer with provider and subscribers
record InformationSourceGroup(
    String payerName,
    String payerId,
    InformationReceiverGroup informationReceiver
)

// Practice/provider — one per payer group
record InformationReceiverGroup(
    String providerName,
    String providerNpi,
    String providerTaxId,
    List<EligibilitySubscriber> subscribers
)

// One per patient under a given payer
record EligibilitySubscriber(
    SubscriberLoop subscriber,
    String eligibilityDate
)
```

## Service Layer

### EDI270Mapper

Pure mapping function. No repository access.

- Input: `Practice`, `List<EligibilityBundle>`
- Groups bundles by payer ID to build `List<InformationSourceGroup>`
- Builds shared loop records (envelope, functional group, transaction header) from `InterchangeProperties` config
- Generates control numbers from `System.currentTimeMillis()`
- Output: `EDI270Inquiry`

### EDI270Generator

Pure EDI writing. Takes loop records only.

- Input: `EDI270Inquiry`
- Uses StAEDI `EDIStreamWriter` with `elem()`/`comp()` helpers (same pattern as `EDI837Generator`)
- Writes ISA/GS/ST/BHT, then iterates through information source groups → information receiver → subscribers
- Manages HL counter globally across all groups
- Output: EDI string

### InsuranceRequestService

Orchestrator with repository access.

- Input: `List<String> patientIds`
- For each patient: fetch `Patient`, find active `PatientInsurance` (terminationDate is null), fetch `Payer`
- Fetch `Practice` (first available)
- Bundle into `List<EligibilityBundle>` and delegate to mapper → generator
- Output: EDI string

### Supporting types

```java
// Bundles entities needed per patient
record EligibilityBundle(Patient patient, PatientInsurance insurance, Payer payer)
```

## Controller & DTO

Existing endpoint stays the same:

- `POST /api/insurance/eligibility-request`
- Response: `270_inquiry.edi` file download

DTO changes from single patient to list:

```java
record InsuranceRequestDTO(@NotEmpty List<@NotBlank String> patientIds)
```

## Configuration

Add `InterchangeProperties` with `@ConfigurationProperties(prefix = "edi.interchange")`:

```yaml
edi:
  interchange:
    sender-id-qualifier: ZZ
    sender-id: SENDER_ID
    receiver-id-qualifier: ZZ
    receiver-id: RECEIVER_ID
    ack-requested: "0"
    usage-indicator: T
```

Same pattern as claims-app.

## Testing Strategy

### Unit tests

- **EDI270MapperTest** — Payer grouping, date formatting, gender mapping, control number generation. Hand-build entities, assert loop record fields.
- **EDI270GeneratorTest** — Correct segments, ISA padding, HL hierarchy numbering, segment counts. Hand-build loop records, assert EDI string content.
- **InsuranceRequestServiceTest** — Mock repos + mapper + generator. Verify orchestration and error handling.
- **InsuranceRequestControllerTest** — `@WebMvcTest`. Verify HTTP behavior: 200 file download, 400 for empty/missing list.

### Integration tests

- **EligibilityInquiryIT** — Testcontainers with MongoDB. Seed data, call endpoint with multiple patient IDs, verify EDI file has correct payer grouping and subscriber entries.

### Key test scenarios

- Single patient, single payer
- Multiple patients, same payer (grouped under one HL payer loop)
- Multiple patients, different payers (separate HL payer loops)
- Patient with no active insurance → error
- Patient not found → error

## Dependencies

- `io.xlate:staedi` — already in common module's `build.gradle`
- Testcontainers — needs to be added to `insurance-request-app/build.gradle`
- No new external dependencies required
