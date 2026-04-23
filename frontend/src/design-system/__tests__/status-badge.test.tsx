import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusBadge } from "@/design-system";

describe("StatusBadge", () => {
  it("uses tinted success colors for variant=paid", () => {
    render(<StatusBadge variant="paid">Paid</StatusBadge>);
    const el = screen.getByText("Paid");
    expect(el.className).toMatch(/bg-ds-success-bg/);
    expect(el.className).toMatch(/text-ds-success-fg/);
  });

  it("uses tinted warning colors for variant=pending", () => {
    render(<StatusBadge variant="pending">Pending</StatusBadge>);
    const el = screen.getByText("Pending");
    expect(el.className).toMatch(/bg-ds-warning-bg/);
    expect(el.className).toMatch(/text-ds-warning-fg/);
  });

  it("uses tinted error colors for variant=overdue", () => {
    render(<StatusBadge variant="overdue">Overdue</StatusBadge>);
    const el = screen.getByText("Overdue");
    expect(el.className).toMatch(/bg-ds-error-bg/);
    expect(el.className).toMatch(/text-ds-error-fg/);
  });

  it("renders as a pill", () => {
    render(<StatusBadge variant="paid">x</StatusBadge>);
    expect(screen.getByText("x").className).toMatch(/rounded-full/);
  });
});
