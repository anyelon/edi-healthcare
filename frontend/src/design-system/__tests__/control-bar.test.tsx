import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { ControlBar } from "@/design-system";

describe("ControlBar", () => {
  it("uses flex row with 8px gap (gap-ds-sm)", () => {
    render(<ControlBar data-testid="cb"><div>a</div><div>b</div></ControlBar>);
    const el = screen.getByTestId("cb");
    expect(el.className).toMatch(/\bflex\b/);
    expect(el.className).toMatch(/gap-ds-sm/);
  });
});
