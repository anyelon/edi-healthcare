# 007 — Data GET Endpoints

## Summary

Add read-only GET endpoints to `claims-app` for retrieving patients and encounters. These endpoints let the frontend display data for selection rather than requiring users to manually enter IDs.

## Endpoints

### `GET /api/patients`

Returns all patients.

**Response:** `200 OK` — `List<PatientResponse>`

```json
[
  {
    "id": "abc123",
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-15",
    "gender": "M",
    "address": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "zipCode": "62701",
    "phone": "555-0100"
  }
]
```

Returns an empty list if no patients exist.

### `GET /api/encounters`

Returns all encounters, each enriched with resolved names from related entities and nested diagnoses/procedures.

**Response:** `200 OK` — `List<EncounterResponse>`

```json
[
  {
    "id": "enc456",
    "patientId": "abc123",
    "patientName": "John Doe",
    "providerId": "prov789",
    "providerName": "Dr. Jane Smith",
    "facilityId": "fac012",
    "facilityName": "Main Street Clinic",
    "dateOfService": "2026-03-15",
    "authorizationNumber": "AUTH001",
    "diagnoses": [
      { "diagnosisCode": "J06.9", "rank": 1 }
    ],
    "procedures": [
      { "procedureCode": "99213", "modifiers": ["25"], "chargeAmount": 150.00, "units": 1 }
    ]
  }
]
```

Returns an empty list if no encounters exist.

## Architecture

### Where: `claims-app`

Both endpoints are added to `claims-app` (port 8080). This app already has all repository dependencies, the `DevSeedController` for data management, and is the frontend's primary backend proxy target.

### Layer Structure

```
PatientController → PatientService → PatientRepository
EncounterController → EncounterService → EncounterRepository + PatientRepository + ProviderRepository + FacilityRepository + EncounterDiagnosisRepository + EncounterProcedureRepository
```

### New Files (all in `claims-app`)

**Controllers:**
- `PatientController` — `@RestController`, `@RequestMapping("/api/patients")`, single `GET /` method
- `EncounterController` — `@RestController`, `@RequestMapping("/api/encounters")`, single `GET /` method

**Services:**
- `PatientService` — delegates to `PatientRepository`, maps `Patient` → `PatientResponse`
- `EncounterService` — fetches all encounters, batch-loads related patients/providers/facilities/diagnoses/procedures, assembles `EncounterResponse` objects

**DTOs (Java records):**
- `PatientResponse` — mirrors Patient document fields
- `EncounterResponse` — encounter fields + resolved names + nested lists
- `DiagnosisResponse` — diagnosisCode, rank
- `ProcedureResponse` — procedureCode, modifiers, chargeAmount, units

All DTOs live in claims-app (not common) since they are API-specific.

### Enrichment Strategy

`EncounterService` batch-loads related entities to avoid N+1 queries:
1. Fetch all encounters
2. Collect distinct patientIds, providerIds, facilityIds, and encounter IDs
3. Batch-fetch patients, providers, facilities, diagnoses, and procedures
4. Build lookup maps (id → entity)
5. Assemble enriched responses

## Scope

**In scope:** Backend API changes only — two GET endpoints in claims-app with services, DTOs, and tests.

**Out of scope:** Frontend changes, pagination, filtering, sorting, new frontend BFF routes.

## Testing

- `@WebMvcTest` controller tests for `PatientController` and `EncounterController` (mock services, verify JSON shape and status codes)
- Unit tests for `EncounterService` (mock repositories, verify enrichment logic and batch-loading)
- Unit tests for `PatientService` (mock repository, verify mapping)

## Conventions

- Constructor injection only (no `@Autowired`)
- Record types for DTOs
- Test naming: `*Test.java`
- `@EnableMongoRepositories` already configured in `MongoConfig.java`
