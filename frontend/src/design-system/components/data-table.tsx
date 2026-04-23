"use client";

import React from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../primitives/table";
import { cn } from "../utils/cn";

export type SortDir = "asc" | "desc";

export interface ColumnDef<T> {
  header: string;
  accessor: keyof T | ((row: T) => unknown);
  cell?: (row: T) => React.ReactNode;
  sortKey?: string;
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  selectedIds: Set<string>;
  onSelectionChange: (ids: Set<string>) => void;
  getId: (row: T) => string;
  emptyMessage?: React.ReactNode;
  sortKey?: string;
  sortDir?: SortDir;
  onSortChange?: (key: string, dir: SortDir) => void;
  bulkActions?: React.ReactNode;
}

export function DataTable<T>({
  columns,
  data,
  selectedIds,
  onSelectionChange,
  getId,
  emptyMessage = "No data available.",
  sortKey,
  sortDir,
  onSortChange,
  bulkActions,
}: DataTableProps<T>) {
  const allSelected = data.length > 0 && data.every((row) => selectedIds.has(getId(row)));
  const someSelected = data.some((row) => selectedIds.has(getId(row)));

  function toggleAll() {
    onSelectionChange(allSelected ? new Set() : new Set(data.map(getId)));
  }

  function toggleRow(id: string) {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    onSelectionChange(next);
  }

  function getCellValue(row: T, col: ColumnDef<T>): React.ReactNode {
    if (col.cell) return col.cell(row);
    if (typeof col.accessor === "function") return String(col.accessor(row));
    return String(row[col.accessor] ?? "");
  }

  function handleHeaderClick(col: ColumnDef<T>) {
    if (!col.sortKey || !onSortChange) return;
    const nextDir: SortDir = col.sortKey === sortKey && sortDir === "asc" ? "desc" : col.sortKey === sortKey && sortDir === "desc" ? "asc" : "asc";
    onSortChange(col.sortKey, nextDir);
  }

  return (
    <>
      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <input
                  type="checkbox"
                  checked={allSelected}
                  ref={(el) => { if (el) el.indeterminate = someSelected && !allSelected; }}
                  onChange={toggleAll}
                  className="h-4 w-4 rounded border-border accent-primary"
                  aria-label="Select all"
                />
              </TableHead>
              {columns.map((col) => {
                const isActive = col.sortKey && col.sortKey === sortKey;
                const clickable = Boolean(col.sortKey && onSortChange);
                return (
                  <TableHead
                    key={col.header}
                    onClick={clickable ? () => handleHeaderClick(col) : undefined}
                    className={cn(clickable && "cursor-pointer select-none")}
                  >
                    <span className="inline-flex items-center gap-1">
                      {col.header}
                      {isActive ? (
                        <span data-sort-indicator className="text-ds-secondary">
                          {sortDir === "asc" ? "▲" : "▼"}
                        </span>
                      ) : null}
                    </span>
                  </TableHead>
                );
              })}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} className="h-24 text-center text-muted-foreground">
                  {emptyMessage}
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
                        aria-label="Select row"
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
      {bulkActions && selectedIds.size > 0 ? (
        <div
          className="sticky bottom-0 z-10 mt-ds-sm flex items-center justify-between rounded-lg border bg-card p-ds-sm"
          style={{ boxShadow: "var(--ds-shadow-popover)" }}
        >
          <span className="text-body-md text-muted-foreground">
            {selectedIds.size} selected
          </span>
          <div className="flex items-center gap-ds-sm">{bulkActions}</div>
        </div>
      ) : null}
    </>
  );
}
