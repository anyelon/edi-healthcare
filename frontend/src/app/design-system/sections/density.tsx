"use client";

import { useState } from "react";
import { DataTable, Button, type ColumnDef } from "@/design-system";

type R = { id: string; code: string; description: string; amount: number };

const rows: R[] = Array.from({ length: 6 }, (_, i) => ({
  id: String(i + 1),
  code: `99${200 + i}`,
  description: `Sample service ${i + 1}`,
  amount: 100 + i * 25,
}));

const columns: ColumnDef<R>[] = [
  { header: "Code", accessor: "code" },
  { header: "Description", accessor: "description" },
  { header: "Amount", accessor: "amount", cell: (r) => `$${r.amount.toFixed(2)}` },
];

export function DensityPreview() {
  const [density, setDensity] = useState<"comfortable" | "compact">("comfortable");
  const [sel, setSel] = useState<Set<string>>(new Set());

  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Density</h2>
      <div className="flex gap-sm">
        <Button
          variant={density === "comfortable" ? "default" : "outline"}
          onClick={() => setDensity("comfortable")}
        >
          Comfortable
        </Button>
        <Button
          variant={density === "compact" ? "default" : "outline"}
          onClick={() => setDensity("compact")}
        >
          Compact
        </Button>
      </div>
      <div data-density={density}>
        <DataTable
          columns={columns}
          data={rows}
          selectedIds={sel}
          onSelectionChange={setSel}
          getId={(r) => r.id}
        />
      </div>
    </div>
  );
}
