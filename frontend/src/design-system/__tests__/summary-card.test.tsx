// frontend/src/design-system/__tests__/summary-card.test.tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { SummaryCard } from "@/design-system";

describe("SummaryCard", () => {
  it("renders label uppercase and metric", () => {
    render(<SummaryCard label="Total Outstanding" metric="$127,430.00" />);
    expect(screen.getByText("Total Outstanding").className).toMatch(/uppercase/);
    expect(screen.getByText("$127,430.00")).toBeInTheDocument();
  });

  it("renders metric with h1 text utility", () => {
    render(<SummaryCard label="x" metric="42" />);
    expect(screen.getByText("42").className).toMatch(/text-h1/);
  });

  it("delta uses success color when trend=up", () => {
    render(<SummaryCard label="x" metric="42" delta="+2.4%" trend="up" />);
    expect(screen.getByText("+2.4%").className).toMatch(/text-ds-success/);
  });

  it("delta uses error color when trend=down", () => {
    render(<SummaryCard label="x" metric="42" delta="-1.1%" trend="down" />);
    expect(screen.getByText("-1.1%").className).toMatch(/text-ds-error/);
  });
});
