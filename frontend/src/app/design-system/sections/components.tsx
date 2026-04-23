"use client";

import { useState } from "react";
import {
  StatusBadge,
  SummaryCard,
  FiscalInput,
  ControlBar,
  DataTable,
  Input,
  Button,
  type ColumnDef,
} from "@/design-system";

type Claim = { id: string; patient: string; amount: number; status: "paid" | "pending" | "overdue" };

const claims: Claim[] = [
  { id: "1", patient: "A. Alvarez", amount: 1245.5, status: "paid" },
  { id: "2", patient: "M. Brown",   amount:  780.0, status: "pending" },
  { id: "3", patient: "J. Chen",    amount: 2310.0, status: "overdue" },
];

const columns: ColumnDef<Claim>[] = [
  { header: "Patient", accessor: "patient", sortKey: "patient" },
  { header: "Amount",  accessor: "amount",  sortKey: "amount",
    cell: (r) => `$${r.amount.toFixed(2)}` },
  { header: "Status",  accessor: "status",
    cell: (r) => <StatusBadge variant={r.status}>{r.status}</StatusBadge> },
];

export function ComponentsPreview() {
  const [selection, setSelection] = useState<Set<string>>(new Set());
  const [amount, setAmount] = useState<number | "">(127430);
  const [sortKey, setSortKey] = useState<string>("amount");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Components</h2>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">StatusBadge</h3>
        <div className="flex gap-sm">
          <StatusBadge variant="paid">Paid</StatusBadge>
          <StatusBadge variant="pending">Pending</StatusBadge>
          <StatusBadge variant="overdue">Overdue</StatusBadge>
          <StatusBadge variant="neutral">Neutral</StatusBadge>
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">SummaryCard</h3>
        <div className="grid grid-cols-3 gap-md">
          <SummaryCard label="Total Outstanding" metric="$127,430.00" delta="+2.4%" trend="up" />
          <SummaryCard label="Collected MTD"      metric="$84,120.00"  delta="-1.1%" trend="down" />
          <SummaryCard label="Net Collection Rate" metric="96.2%" />
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">FiscalInput</h3>
        <div className="max-w-xs">
          <FiscalInput value={amount} onValueChange={setAmount} aria-label="Fiscal amount" />
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">ControlBar + DataTable</h3>
        <ControlBar>
          <Input placeholder="Search claims…" className="max-w-xs" />
          <Button variant="outline">Filter</Button>
        </ControlBar>
        <div className="mt-sm">
          <DataTable
            columns={columns}
            data={claims}
            selectedIds={selection}
            onSelectionChange={setSelection}
            getId={(r) => r.id}
            sortKey={sortKey}
            sortDir={sortDir}
            onSortChange={(k, d) => { setSortKey(k); setSortDir(d); }}
            bulkActions={<Button>Void selected</Button>}
          />
        </div>
      </div>
    </div>
  );
}
