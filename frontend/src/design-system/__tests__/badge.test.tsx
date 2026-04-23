import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Badge } from "@/design-system";

describe("Badge", () => {
  it("renders rectangular by default (rounded)", () => {
    render(<Badge>Label</Badge>);
    expect(screen.getByText("Label").className).toMatch(/\brounded\b/);
  });

  it("supports pill shape via shape=\"pill\"", () => {
    render(<Badge shape="pill">Status</Badge>);
    expect(screen.getByText("Status").className).toMatch(/rounded-full/);
  });
});
