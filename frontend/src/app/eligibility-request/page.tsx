"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button, DataTable, type ColumnDef } from "@/design-system";
import { GenerationLayout } from "@/components/generation-layout";
import { fetchPatients, generateEligibilityRequest } from "@/lib/api-client";
import type { PatientResponse } from "@/types";

const columns: ColumnDef<PatientResponse>[] = [
  {
    header: "Name",
    accessor: "firstName",
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
  const [error, setError] = useState<string | null>(null);

  function loadPatients() {
    setError(null);
    setFetching(true);
    fetchPatients()
      .then(setPatients)
      .catch((err) => {
        const message = err instanceof Error ? err.message : "Failed to fetch patients";
        setError(message);
        toast.error(message);
      })
      .finally(() => setFetching(false));
  }

  useEffect(() => {
    loadPatients();
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
      {fetching ? (
        <div className="flex h-64 items-center justify-center text-muted-foreground">
          Loading patients...
        </div>
      ) : error ? (
        <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
          <p>{error}</p>
          <Button variant="outline" onClick={loadPatients}>
            Retry
          </Button>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={patients}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          getId={(p) => p.id}
          emptyMessage="No patients found. Try seeding the database from the sidebar."
        />
      )}
    </GenerationLayout>
  );
}
