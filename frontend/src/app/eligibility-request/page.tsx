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
