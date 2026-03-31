# 010 — Prior Authorization (EDI 278)

## Overview

New `prior-auth-app` microservice that generates EDI 278 Health Care Services Review requests from encounter data and parses inbound EDI 278 responses. On parsing a response, the service writes the authorization number back to the encounter document. Runs on port 8083.

## Data Model Changes

### EncounterProcedure (updated)

Add two fields to the existing `EncounterProcedure` document in `com.example.edi.common.document`:

| Field | Type | Description |
|-------|------|-------------|
| needsAuth | boolean | Whether this procedure requires prior authorization |
| clinicalReason | String | Free-text clinical justification (null when needsAuth is false) |

This avoids a separate embedded list on `Encounter` — procedures that need prior auth are already in the `encounter_procedures` collection, so we flag them there. The prior-auth service filters for `needsAuth == true` when building the 278 request.

The `ProcedureResponse` DTO, `EncounterResponse`, and GET endpoint must also expose `needsAuth` and `clinicalReason`.

### Removed

The `RequestedProcedure` embedded class and `Encounter.requestedProcedures` field are removed. The `RequestedProcedureResponse` frontend type is removed.

## Backend Module

**Module:** `prior-auth-app`
**Package:** `com.example.edi.priorauth`
**Port:** 8083

### Package Layout

```
backend/prior-auth-app/
├── PriorAuthApplication.java
├── config/
│   ├── MongoConfig.java
│   ├── InterchangeProperties.java
│   └── ArchiveProperties.java
├── controller/
│   └── PriorAuthController.java
├── dto/
│   ├── PriorAuthRequestDTO.java      # record(encounterIds: List<String>)
│   └── PriorAuthBundle.java          # record(encounter, patient, insurance, payer)
├── domain/
│   ├── EDI278Request.java            # Record tree for outbound 278
│   └── EDI278Response.java           # Record tree for parsed inbound 278
└── service/
    ├── PriorAuthService.java         # Orchestrator
    ├── EDI278Mapper.java             # Entities → EDI278Request
    ├── EDI278Generator.java          # EDI278Request → EDI string (StAEDI)
    ├── EDI278Parser.java             # EDI file → EDI278Response (StAEDI)
    └── EDI278ResponseMapper.java     # EDI278Response → encounter update
```

### API Endpoints

| Method | Path | Input | Output |
|--------|------|-------|--------|
| POST | `/api/prior-auth/generate` | `{ "encounterIds": [...] }` | EDI 278 file download (`278_prior_auth.edi`) |
| POST | `/api/prior-auth/response` | Multipart file | JSON with parsed authorization decision |

### Gradle Configuration

Same dependency pattern as the other apps: `spring-boot-starter-web`, `staedi`, `springdoc-openapi`, `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, testcontainers-mongodb. Depends on `:common`.

Registered in `settings.gradle` as:

```gradle
include 'prior-auth-app'
project(':prior-auth-app').projectDir = file('backend/prior-auth-app')
```

## EDI 278 Transaction Structure

### Outbound Request

Envelope reuses the standard X12 pattern (ISA/GS/ST with functional identifier `HI`, transaction set `278`).

Business content:

- **BHT** — Beginning of transaction (reference type `0007` = request)
- **TRN** — Trace number (set to the encounter ID so the response can be correlated back)
- **HL 1** — Utilization Management Organization (payer): NM1
- **HL 2** — Requester (provider/practice): NM1 + REF (NPI, Tax ID)
- **HL 3** — Subscriber (patient): NM1 + DMG
- **HL 4** — Patient event:
  - UM — Service type, certification type, facility type
  - DTP — Requested service date(s)
  - SV1/SV2 — Service line with procedure code
  - HI — Clinical reason (diagnosis/justification)

Common segments (InterchangeEnvelope, FunctionalGroup, TransactionHeader) follow the same record structure used in the 270 and 837 generators.

### Inbound Response

Same envelope. Key differences:

- **BHT** reference type `0008` (response)
- **HCR** — Authorization decision (certified / denied / pended)
- **REF** — Authorization/certification number (written back to `encounter.authorizationNumber`)
- **AAA** — Validation errors (if any)

## Service Flow

### Request Generation

1. Controller receives `PriorAuthRequestDTO` with `encounterIds`
2. `PriorAuthService` fetches per encounter: Encounter, Patient, PatientInsurance, Payer, Practice, and EncounterProcedures (filtered to `needsAuth == true`) — bundles into `PriorAuthBundle`
3. `EDI278Mapper` maps bundles to `EDI278Request` domain model
4. `EDI278Generator` writes EDI string via StAEDI
5. Controller returns `.edi` file as byte array download

### Response Parsing

1. Controller receives multipart file upload
2. `PriorAuthService` archives the file (via `ArchiveProperties`)
3. `EDI278Parser` parses file via StAEDI into `EDI278Response`
4. `EDI278ResponseMapper` extracts authorization number from HCR/REF segments and the encounter trace number from the TRN segment (the trace number is the encounter ID, set during request generation)
5. Service looks up the encounter by the trace number and updates `encounter.authorizationNumber` via `EncounterRepository.save()`
6. Controller returns parsed response as JSON

## Frontend

### Page

`frontend/src/app/prior-auth/page.tsx` — Single page with two tabs:

**Generate Request tab:**
- Uses `GenerationLayout` + `DataTable` (same pattern as claims page)
- Displays encounters with columns: Patient Name, Date of Service, Provider, Procedures Needing Auth (filtered from procedures where needsAuth is true), Authorization Status
- Select encounters, generate EDI 278, preview panel shows output

**Parse Response tab:**
- Uses `Dropzone` for file upload (same pattern as eligibility-response page)
- Displays parsed authorization decision, certification number, patient info, service details
- Success/denied badge based on decision

### BFF API Routes

| Route File | Proxies To |
|------------|------------|
| `frontend/src/app/api/prior-auth/generate/route.ts` | `http://localhost:8083/api/prior-auth/generate` |
| `frontend/src/app/api/prior-auth/response/route.ts` | `http://localhost:8083/api/prior-auth/response` |

Environment variable: `PRIOR_AUTH_API_URL` (default `http://localhost:8083`).

### API Client Additions

```typescript
generatePriorAuth(encounterIds: string[]): Promise<Blob>
parsePriorAuthResponse(file: File): Promise<PriorAuthResponse>
```

### Type Additions

```typescript
interface ProcedureResponse {
  procedureCode: string;
  modifiers: string[];
  chargeAmount: number;
  units: number;
  needsAuth: boolean;
  clinicalReason: string | null;
}

interface PriorAuthResponse {
  payerName: string;
  payerId: string;
  subscriberFirstName: string;
  subscriberLastName: string;
  memberId: string;
  decisions: AuthorizationDecision[];
}
```

`ProcedureResponse` updated with `needsAuth` and `clinicalReason`. The separate `RequestedProcedureResponse` type is removed.

### Dashboard

Add a fourth card on the dashboard linking to `/prior-auth` with label "Prior Authorization 278".

## Testing

### Unit Tests

| Test | Scope |
|------|-------|
| `EDI278GeneratorTest` | Verify generated EDI contains correct segments (ISA, GS, ST, BHT, HL loops, UM, SV1, HI, SE, GE, IEA) |
| `EDI278ParserTest` | Parse sample 278 response files, verify domain model correctness |
| `EDI278MapperTest` | Verify entity-to-domain mapping |
| `EDI278ResponseMapperTest` | Verify parsed response extraction and auth number mapping |
| `PriorAuthControllerTest` | `@WebMvcTest` with `@MockitoBean`. Test generate returns file, response returns JSON |

### Integration Tests

| Test | Scope |
|------|-------|
| `PriorAuthServiceIT` | Testcontainers + MongoDB. Seed data, generate 278, verify output. Upload response, verify `encounter.authorizationNumber` updated |

### Test Resources

Sample EDI 278 response file under `src/test/resources/` for parser tests.
