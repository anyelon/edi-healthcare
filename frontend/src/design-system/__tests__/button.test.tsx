import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "@/design-system";

describe("Button", () => {
  it("renders a primary variant with navy background and white text", () => {
    render(<Button>Save</Button>);
    const btn = screen.getByRole("button", { name: "Save" });
    expect(btn.className).toMatch(/bg-primary/);
    expect(btn.className).toMatch(/text-primary-foreground/);
  });

  it("outline variant uses primary border and primary text", () => {
    render(<Button variant="outline">Cancel</Button>);
    const btn = screen.getByRole("button", { name: "Cancel" });
    expect(btn.className).toMatch(/border-primary/);
    expect(btn.className).toMatch(/text-primary/);
    expect(btn.className).toMatch(/bg-transparent/);
  });

  it("ghost variant uses secondary (blue) text", () => {
    render(<Button variant="ghost">Details</Button>);
    const btn = screen.getByRole("button", { name: "Details" });
    expect(btn.className).toMatch(/text-secondary/);
  });

  it("has 4px rounded corners (rounded class)", () => {
    render(<Button>x</Button>);
    const btn = screen.getByRole("button", { name: "x" });
    expect(btn.className).toMatch(/\brounded\b/);
  });
});
