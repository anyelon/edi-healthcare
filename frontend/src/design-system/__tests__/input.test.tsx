import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Input } from "@/design-system";

describe("Input", () => {
  it("renders with 1px border class", () => {
    render(<Input placeholder="p" />);
    const el = screen.getByPlaceholderText("p");
    expect(el.className).toMatch(/\bborder\b/);
  });

  it("applies blue ring on focus via focus-visible:ring-2 and focus-visible:ring-ring", () => {
    render(<Input placeholder="p" />);
    const el = screen.getByPlaceholderText("p");
    expect(el.className).toMatch(/focus-visible:ring-2/);
    expect(el.className).toMatch(/focus-visible:ring-ring/);
  });

  it("renders with aria-invalid and red border classes when invalid", () => {
    render(<Input placeholder="p" aria-invalid />);
    const el = screen.getByPlaceholderText("p");
    expect(el.className).toMatch(/aria-invalid:border-destructive/);
  });
});
