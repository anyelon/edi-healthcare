import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DataTable, type ColumnDef } from "@/design-system";

type Row = { id: string; name: string; amount: number };

const columns: ColumnDef<Row>[] = [
  { header: "Name", accessor: "name", sortKey: "name" },
  { header: "Amount", accessor: "amount", sortKey: "amount" },
];

const rows: Row[] = [
  { id: "1", name: "Alpha", amount: 10 },
  { id: "2", name: "Bravo", amount: 20 },
];

describe("DataTable", () => {
  it("renders a sort indicator on the active sort column", () => {
    render(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set()}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        sortKey="amount"
        sortDir="desc"
        onSortChange={() => {}}
      />,
    );
    const amountHeader = screen.getByRole("columnheader", { name: /amount/i });
    expect(amountHeader.querySelector("[data-sort-indicator]")).not.toBeNull();
  });

  it("fires onSortChange when a sortable header is clicked", async () => {
    const user = userEvent.setup();
    const onSort = vi.fn();
    render(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set()}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        sortKey="amount"
        sortDir="desc"
        onSortChange={onSort}
      />,
    );
    await user.click(screen.getByRole("columnheader", { name: /amount/i }));
    expect(onSort).toHaveBeenCalledWith("amount", "asc");
  });

  it("renders the bulk-action slot only when selection is non-empty", () => {
    const { rerender } = render(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set()}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        bulkActions={<button>Void</button>}
      />,
    );
    expect(screen.queryByRole("button", { name: "Void" })).toBeNull();

    rerender(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set(["1"])}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        bulkActions={<button>Void</button>}
      />,
    );
    expect(screen.getByRole("button", { name: "Void" })).toBeInTheDocument();
  });
});
