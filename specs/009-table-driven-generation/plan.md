# Table-Driven EDI Generation UI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the free-text ID input on Claims and Eligibility Request pages with data tables that let users select records and generate EDI files, with a side-panel preview.

**Architecture:** Two new BFF GET routes proxy encounter/patient data. A shared `DataTable` component handles checkbox selection. A `GenerationLayout` component provides the two-panel layout (table + EDI preview). Both pages reuse these components with different column configs and API calls.

**Tech Stack:** Next.js 16, React 19, TypeScript, Tailwind CSS, existing shadcn-style UI components (Table, Badge, Button, Card, ScrollArea), Sonner toasts.

---

## File Map

| Action | File | Responsibility |
|--------|------|---------------|
| Create | `frontend/src/app/api/encounters/route.ts` | BFF proxy: GET → backend /api/encounters |
| Create | `frontend/src/app/api/patients/route.ts` | BFF proxy: GET → backend /api/patients |
| Modify | `frontend/src/types/index.ts` | Add EncounterResponse, PatientResponse, DiagnosisResponse, ProcedureResponse |
| Modify | `frontend/src/lib/api-client.ts` | Add fetchEncounters(), fetchPatients() |
| Create | `frontend/src/components/ui/data-table.tsx` | Generic table with checkbox selection |
| Create | `frontend/src/components/generation-layout.tsx` | Page wrapper: header, action buttons, table + preview panel |
| Modify | `frontend/src/app/claims/page.tsx` | Replace textarea with DataTable + GenerationLayout |
| Modify | `frontend/src/app/eligibility-request/page.tsx` | Replace textarea with DataTable + GenerationLayout |

---

### Task 1: Create branch

**Files:** None

- [ ] **Step 1: Create and switch to feature branch**

```bash
git checkout -b feat/table-driven-generation
```

- [ ] **Step 2: Verify branch**

```bash
git branch --show-current
```

Expected: `feat/table-driven-generation`

---

### Task 2: Add TypeScript types

**Files:**
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1: Add the new interfaces to the types file**

Add these interfaces to `frontend/src/types/index.ts` after the existing `SeedResult` interface:

```typescript
export interface DiagnosisResponse {
  diagnosisCode: string;
  rank: number;
}

export interface ProcedureResponse {
  procedureCode: string;
  modifiers: string[];
  chargeAmount: number;
  units: number;
}

export interface EncounterResponse {
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

export interface PatientResponse {
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

- [ ] **Step 2: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/index.ts
git commit -m "feat: add EncounterResponse and PatientResponse types"
```

---

### Task 3: Add BFF proxy routes

**Files:**
- Create: `frontend/src/app/api/encounters/route.ts`
- Create: `frontend/src/app/api/patients/route.ts`

- [ ] **Step 1: Create the encounters BFF route**

Create `frontend/src/app/api/encounters/route.ts`:

```typescript
const CLAIMS_API = process.env.CLAIMS_API_URL || "http://localhost:8080";

export async function GET() {
  const response = await fetch(`${CLAIMS_API}/api/encounters`);

  const data = await response.json();
  return Response.json(data, { status: response.status });
}
```

- [ ] **Step 2: Create the patients BFF route**

Create `frontend/src/app/api/patients/route.ts`:

```typescript
const CLAIMS_API = process.env.CLAIMS_API_URL || "http://localhost:8080";

export async function GET() {
  const response = await fetch(`${CLAIMS_API}/api/patients`);

  const data = await response.json();
  return Response.json(data, { status: response.status });
}
```

- [ ] **Step 3: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/api/encounters/route.ts frontend/src/app/api/patients/route.ts
git commit -m "feat: add BFF proxy routes for encounters and patients"
```

---

### Task 4: Add API client functions

**Files:**
- Modify: `frontend/src/lib/api-client.ts`

- [ ] **Step 1: Add fetchEncounters and fetchPatients to api-client.ts**

Add these two functions at the end of `frontend/src/lib/api-client.ts`, and add the import for the new types at the top:

Update the import line at the top of the file:

```typescript
import type { SeedResult, EligibilityResponse, EncounterResponse, PatientResponse } from "@/types";
```

Add these functions after the existing `downloadBlob` function:

```typescript
export async function fetchEncounters(): Promise<EncounterResponse[]> {
  const res = await fetch("/api/encounters");
  if (!res.ok) throw new Error(`Failed to fetch encounters: ${res.statusText}`);
  return res.json();
}

export async function fetchPatients(): Promise<PatientResponse[]> {
  const res = await fetch("/api/patients");
  if (!res.ok) throw new Error(`Failed to fetch patients: ${res.statusText}`);
  return res.json();
}
```

- [ ] **Step 2: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/api-client.ts
git commit -m "feat: add fetchEncounters and fetchPatients API client functions"
```

---

### Task 5: Build the DataTable component

**Files:**
- Create: `frontend/src/components/ui/data-table.tsx`

- [ ] **Step 1: Create the DataTable component**

Create `frontend/src/components/ui/data-table.tsx`:

```tsx
"use client";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";

export interface ColumnDef<T> {
  header: string;
  accessor: keyof T | ((row: T) => unknown);
  cell?: (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  selectedIds: Set<string>;
  onSelectionChange: (ids: Set<string>) => void;
  getId: (row: T) => string;
}

export function DataTable<T>({
  columns,
  data,
  selectedIds,
  onSelectionChange,
  getId,
}: DataTableProps<T>) {
  const allSelected = data.length > 0 && data.every((row) => selectedIds.has(getId(row)));
  const someSelected = data.some((row) => selectedIds.has(getId(row)));

  function toggleAll() {
    if (allSelected) {
      onSelectionChange(new Set());
    } else {
      onSelectionChange(new Set(data.map(getId)));
    }
  }

  function toggleRow(id: string) {
    const next = new Set(selectedIds);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    onSelectionChange(next);
  }

  function getCellValue(row: T, col: ColumnDef<T>): React.ReactNode {
    if (col.cell) return col.cell(row);
    if (typeof col.accessor === "function") return String(col.accessor(row));
    return String(row[col.accessor] ?? "");
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-10">
              <input
                type="checkbox"
                checked={allSelected}
                ref={(el) => {
                  if (el) el.indeterminate = someSelected && !allSelected;
                }}
                onChange={toggleAll}
                className="h-4 w-4 rounded border-border accent-primary"
              />
            </TableHead>
            {columns.map((col) => (
              <TableHead key={col.header}>{col.header}</TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.length === 0 ? (
            <TableRow>
              <TableCell colSpan={columns.length + 1} className="h-24 text-center text-muted-foreground">
                No data available.
              </TableCell>
            </TableRow>
          ) : (
            data.map((row) => {
              const id = getId(row);
              const isSelected = selectedIds.has(id);
              return (
                <TableRow
                  key={id}
                  data-state={isSelected ? "selected" : undefined}
                  className={cn(isSelected && "bg-muted/50")}
                >
                  <TableCell>
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => toggleRow(id)}
                      className="h-4 w-4 rounded border-border accent-primary"
                    />
                  </TableCell>
                  {columns.map((col) => (
                    <TableCell key={col.header}>{getCellValue(row, col)}</TableCell>
                  ))}
                </TableRow>
              );
            })
          )}
        </TableBody>
      </Table>
    </div>
  );
}
```

- [ ] **Step 2: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/ui/data-table.tsx
git commit -m "feat: add generic DataTable component with checkbox selection"
```

---

### Task 6: Build the GenerationLayout component

**Files:**
- Create: `frontend/src/components/generation-layout.tsx`

- [ ] **Step 1: Create the GenerationLayout component**

Create `frontend/src/components/generation-layout.tsx`:

```tsx
"use client";

import { X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EdiPreview } from "@/components/edi-preview";

interface GenerationLayoutProps {
  title: string;
  description: string;
  badgeLabel: string;
  selectedCount: number;
  totalCount: number;
  isLoading: boolean;
  onGenerateAll: () => void;
  onGenerateSelected: () => void;
  preview: string | null;
  previewFilename: string;
  onClosePreview: () => void;
  children: React.ReactNode;
}

export function GenerationLayout({
  title,
  description,
  badgeLabel,
  selectedCount,
  totalCount,
  isLoading,
  onGenerateAll,
  onGenerateSelected,
  preview,
  previewFilename,
  onClosePreview,
  children,
}: GenerationLayoutProps) {
  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
            <Badge variant="outline">{badgeLabel}</Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={onGenerateAll}
            disabled={isLoading || totalCount === 0}
          >
            {isLoading ? "Generating..." : "Generate All"}
          </Button>
          <Button
            onClick={onGenerateSelected}
            disabled={isLoading || selectedCount === 0}
          >
            {isLoading
              ? "Generating..."
              : `Generate Selected (${selectedCount})`}
          </Button>
        </div>
      </div>

      <div className="flex gap-6">
        <div className={preview ? "flex-1 min-w-0" : "w-full"}>
          {children}
        </div>

        {preview && (
          <div className="w-[400px] shrink-0 space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold">EDI Preview</h2>
              <Button variant="ghost" size="icon-xs" onClick={onClosePreview}>
                <X className="h-4 w-4" />
              </Button>
            </div>
            <EdiPreview content={preview} filename={previewFilename} />
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/generation-layout.tsx
git commit -m "feat: add GenerationLayout component with table + preview panel"
```

---

### Task 7: Rewrite the Claims page

**Files:**
- Modify: `frontend/src/app/claims/page.tsx`

- [ ] **Step 1: Replace the claims page with the new table-driven UI**

Replace the entire contents of `frontend/src/app/claims/page.tsx` with:

```tsx
"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { GenerationLayout } from "@/components/generation-layout";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { fetchEncounters, generateClaim } from "@/lib/api-client";
import type { EncounterResponse } from "@/types";

const columns: ColumnDef<EncounterResponse>[] = [
  { header: "Patient", accessor: "patientName" },
  { header: "Provider", accessor: "providerName" },
  { header: "Facility", accessor: "facilityName" },
  { header: "Date of Service", accessor: "dateOfService" },
  {
    header: "Diagnoses",
    accessor: "diagnoses",
    cell: (row) => (
      <div className="flex flex-wrap gap-1">
        {row.diagnoses.map((d) => (
          <Badge key={d.diagnosisCode} variant="secondary" className="bg-blue-500/10 text-blue-600 dark:text-blue-400">
            {d.diagnosisCode}
          </Badge>
        ))}
      </div>
    ),
  },
  {
    header: "Procedures",
    accessor: "procedures",
    cell: (row) => (
      <div className="flex flex-wrap gap-1">
        {row.procedures.map((p) => (
          <Badge key={p.procedureCode} variant="secondary" className="bg-green-500/10 text-green-600 dark:text-green-400">
            {p.procedureCode}
          </Badge>
        ))}
      </div>
    ),
  },
];

export default function ClaimsPage() {
  const [encounters, setEncounters] = useState<EncounterResponse[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => {
    fetchEncounters()
      .then(setEncounters)
      .catch((err) =>
        toast.error(err instanceof Error ? err.message : "Failed to fetch encounters")
      )
      .finally(() => setFetching(false));
  }, []);

  async function handleGenerate(ids: string[]) {
    setLoading(true);
    try {
      const blob = await generateClaim(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 837 claim generated successfully.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to generate claim");
    } finally {
      setLoading(false);
    }
  }

  if (fetching) {
    return (
      <div className="flex h-64 items-center justify-center text-muted-foreground">
        Loading encounters...
      </div>
    );
  }

  return (
    <GenerationLayout
      title="Claims Generation"
      description="Select encounters to generate EDI 837 professional claims"
      badgeLabel="EDI 837P"
      selectedCount={selectedIds.size}
      totalCount={encounters.length}
      isLoading={loading}
      onGenerateAll={() => handleGenerate(encounters.map((e) => e.id))}
      onGenerateSelected={() => handleGenerate([...selectedIds])}
      preview={preview}
      previewFilename="837_claim.edi"
      onClosePreview={() => setPreview(null)}
    >
      <DataTable
        columns={columns}
        data={encounters}
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        getId={(e) => e.id}
      />
    </GenerationLayout>
  );
}
```

- [ ] **Step 2: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/claims/page.tsx
git commit -m "feat: replace claims page textarea with encounter data table"
```

---

### Task 8: Rewrite the Eligibility Request page

**Files:**
- Modify: `frontend/src/app/eligibility-request/page.tsx`

- [ ] **Step 1: Replace the eligibility request page with the new table-driven UI**

Replace the entire contents of `frontend/src/app/eligibility-request/page.tsx` with:

```tsx
"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { GenerationLayout } from "@/components/generation-layout";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { fetchPatients, generateEligibilityRequest } from "@/lib/api-client";
import type { PatientResponse } from "@/types";

const columns: ColumnDef<PatientResponse>[] = [
  {
    header: "Name",
    accessor: (row) => `${row.firstName} ${row.lastName}`,
    cell: (row) => `${row.firstName} ${row.lastName}`,
  },
  { header: "Date of Birth", accessor: "dateOfBirth" },
  { header: "Gender", accessor: "gender" },
  { header: "Address", accessor: "address" },
  { header: "City", accessor: "city" },
  { header: "State", accessor: "state" },
  { header: "Zip", accessor: "zipCode" },
  { header: "Phone", accessor: "phone" },
];

export default function EligibilityRequestPage() {
  const [patients, setPatients] = useState<PatientResponse[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => {
    fetchPatients()
      .then(setPatients)
      .catch((err) =>
        toast.error(err instanceof Error ? err.message : "Failed to fetch patients")
      )
      .finally(() => setFetching(false));
  }, []);

  async function handleGenerate(ids: string[]) {
    setLoading(true);
    try {
      const blob = await generateEligibilityRequest(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 270 eligibility request generated successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to generate request"
      );
    } finally {
      setLoading(false);
    }
  }

  if (fetching) {
    return (
      <div className="flex h-64 items-center justify-center text-muted-foreground">
        Loading patients...
      </div>
    );
  }

  return (
    <GenerationLayout
      title="Eligibility Request"
      description="Select patients to generate EDI 270 eligibility inquiries"
      badgeLabel="EDI 270"
      selectedCount={selectedIds.size}
      totalCount={patients.length}
      isLoading={loading}
      onGenerateAll={() => handleGenerate(patients.map((p) => p.id))}
      onGenerateSelected={() => handleGenerate([...selectedIds])}
      preview={preview}
      previewFilename="270_inquiry.edi"
      onClosePreview={() => setPreview(null)}
    >
      <DataTable
        columns={columns}
        data={patients}
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        getId={(p) => p.id}
      />
    </GenerationLayout>
  );
}
```

- [ ] **Step 2: Verify the frontend still compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/eligibility-request/page.tsx
git commit -m "feat: replace eligibility request page textarea with patient data table"
```

---

### Task 9: Manual smoke test

**Files:** None

- [ ] **Step 1: Start MongoDB and backend**

```bash
docker-compose up -d mongodb
```

Then in separate terminals:
```bash
./gradlew :claims-app:bootRun
```

- [ ] **Step 2: Start frontend**

```bash
cd frontend && npm run dev
```

- [ ] **Step 3: Seed the database**

Open the app at `http://localhost:3000`. Click "Seed Database" in the sidebar.

- [ ] **Step 4: Test claims page**

Navigate to Claims Generation (837). Verify:
- Table loads with encounter data (patient names, providers, diagnosis/procedure badges)
- Clicking checkboxes updates the "Generate Selected (N)" count
- "Select all" checkbox works
- "Generate All" produces EDI preview in side panel
- "Generate Selected" with a subset works
- Download button in preview works
- Close (X) button dismisses preview

- [ ] **Step 5: Test eligibility request page**

Navigate to Eligibility Request (270). Verify:
- Table loads with patient data (name, DOB, gender, address, etc.)
- Same checkbox selection behavior as claims
- "Generate All" and "Generate Selected" both work
- Preview panel with download works

- [ ] **Step 6: Final commit if any fixes were needed**

If any fixes were applied during testing:
```bash
git add -A
git commit -m "fix: address issues found during smoke testing"
```
