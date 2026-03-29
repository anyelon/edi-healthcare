"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
  const [error, setError] = useState<string | null>(null);

  function loadEncounters() {
    setError(null);
    setFetching(true);
    fetchEncounters()
      .then(setEncounters)
      .catch((err) => {
        const message = err instanceof Error ? err.message : "Failed to fetch encounters";
        setError(message);
        toast.error(message);
      })
      .finally(() => setFetching(false));
  }

  useEffect(() => {
    loadEncounters();
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
      {fetching ? (
        <div className="flex h-64 items-center justify-center text-muted-foreground">
          Loading encounters...
        </div>
      ) : error ? (
        <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
          <p>{error}</p>
          <Button variant="outline" onClick={loadEncounters}>
            Retry
          </Button>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={encounters}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          getId={(e) => e.id}
          emptyMessage="No encounters found. Try seeding the database from the sidebar."
        />
      )}
    </GenerationLayout>
  );
}
