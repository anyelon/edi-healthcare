# 008: Table-Driven EDI Generation UI

## Overview

Replace the free-text ID input on the Claims (837) and Eligibility Request (270) pages with data tables populated from backend GET endpoints. Users select records via checkboxes and generate EDI files for all or selected records. Generated EDI content displays in a side panel alongside the table.

## Motivation

The current UI requires users to manually type encounter or patient IDs, which is error-prone and requires knowledge of the database. The backend now exposes `/api/encounters` and `/api/patients` endpoints that return all available data — the UI should use these to present a browsable, selectable list.

## New BFF API Routes

### GET `/api/encounters`

Proxies to `http://127.0.0.1:8080/api/encounters`. Returns `EncounterResponse[]`.

### GET `/api/patients`

Proxies to `http://127.0.0.1:8080/api/patients`. Returns `PatientResponse[]`.

Both follow the existing BFF proxy pattern in `frontend/src/app/api/`.

## New TypeScript Types

```typescript
interface DiagnosisResponse {
  diagnosisCode: string;
  rank: number;
}

interface ProcedureResponse {
  procedureCode: string;
  modifiers: string[];
  chargeAmount: number;
  units: number;
}

interface EncounterResponse {
  id: string;
  patientId: string;
  patientName: string;
  providerId: string;
  providerName: string;
  facilityId: string;
  facilityName: string;
  dateOfService: string;
  authorizationNumber: string;
  diagnoses: DiagnosisResponse[];
  procedures: ProcedureResponse[];
}

interface PatientResponse {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  phone: string;
}
```

## New API Client Functions

Add to `frontend/src/lib/api-client.ts`:

- `fetchEncounters(): Promise<EncounterResponse[]>` — GET `/api/encounters`
- `fetchPatients(): Promise<PatientResponse[]>` — GET `/api/patients`

## Shared Components

### `DataTable<T>`

Generic table with checkbox selection. Location: `frontend/src/components/ui/data-table.tsx`.

**Props:**
- `columns: ColumnDef<T>[]` — column definitions (header label, accessor, optional cell renderer)
- `data: T[]` — row data
- `selectedIds: Set<string>` — currently selected row IDs
- `onSelectionChange: (ids: Set<string>) => void` — selection callback
- `getId: (row: T) => string` — extracts the unique ID from a row

**Behavior:**
- Header checkbox toggles select-all / deselect-all
- Row checkboxes toggle individual selection
- Selected rows get a highlighted background

**ColumnDef shape:**
```typescript
interface ColumnDef<T> {
  header: string;
  accessor: keyof T | ((row: T) => unknown);
  cell?: (row: T) => React.ReactNode;
}
```

### `GenerationLayout`

Page wrapper providing the header, action buttons, and two-panel layout. Location: `frontend/src/components/generation-layout.tsx`.

**Props:**
- `title: string`
- `description: string`
- `selectedCount: number`
- `totalCount: number`
- `isLoading: boolean`
- `onGenerateAll: () => void`
- `onGenerateSelected: () => void`
- `preview: string | null` — EDI content to show in the side panel
- `onClosePreview: () => void`
- `children: React.ReactNode` — the DataTable slot

**Layout:**
- Default: full-width table
- With preview: table on the left, EDI preview panel on the right. The panel reuses the existing `EdiPreview` component (from `frontend/src/components/edi-preview.tsx`) and adds a close button (X) in the panel header to dismiss it.

## Page Changes

### `/claims/page.tsx`

Replace the current textarea + form with:

1. `useEffect` on mount → call `fetchEncounters()` → populate state
2. `useState` for `selectedIds: Set<string>`, `preview: string | null`, `isLoading: boolean`
3. Render `GenerationLayout` wrapping a `DataTable<EncounterResponse>`

**Encounters table columns:**
| Column | Source | Rendering |
|--------|--------|-----------|
| Patient | `patientName` | Plain text |
| Provider | `providerName` | Plain text |
| Facility | `facilityName` | Plain text |
| Date of Service | `dateOfService` | Plain text |
| Diagnoses | `diagnoses` | Blue badges showing `diagnosisCode` for each entry |
| Procedures | `procedures` | Green badges showing `procedureCode` for each entry |

**Generation handler:**
- "Generate All" → collect all encounter IDs from data → call `generateClaim(ids)`
- "Generate Selected" → use `selectedIds` → call `generateClaim(ids)`
- Response blob → text → set `preview`

### `/eligibility-request/page.tsx`

Same pattern as claims but with patient data:

1. `useEffect` on mount → call `fetchPatients()` → populate state
2. Same selection and generation state

**Patients table columns:**
| Column | Source | Rendering |
|--------|--------|-----------|
| Name | `firstName` + `lastName` | Combined as "First Last" |
| Date of Birth | `dateOfBirth` | Plain text |
| Gender | `gender` | Plain text |
| Address | `address` | Plain text |
| City | `city` | Plain text |
| State | `state` | Plain text |
| Zip | `zipCode` | Plain text |
| Phone | `phone` | Plain text |

**Generation handler:**
- "Generate All" → collect all patient IDs → call `generateEligibilityRequest(ids)`
- "Generate Selected" → use `selectedIds` → call `generateEligibilityRequest(ids)`
- Response blob → text → set `preview`

## Data Flow

1. **Page load** → fetch data from BFF → populate table
2. **User selects rows** → `selectedIds` state updates → "Generate Selected (N)" button updates count and becomes enabled
3. **User clicks generate** → collect IDs → call existing generation API → blob to text → preview panel slides in
4. **User closes preview** → preview state cleared → table returns to full width

## Error Handling

- **Empty data:** Centered empty state in table: "No encounters found" / "No patients found" with a hint to seed the database.
- **Fetch failure:** Toast error notification. Table shows error state with a retry button.
- **Generation failure:** Toast error. Buttons re-enable. Preview panel stays closed (or retains previous result if one exists).
- **Loading states:** Table shows skeleton/spinner while fetching. Generate buttons show spinner and are disabled during generation.

## Out of Scope

- Pagination, sorting, or filtering (dataset is small, seeded dev data)
- Editing or deleting records from the table
- Changes to the Eligibility Response (271) page
- Changes to backend endpoints
