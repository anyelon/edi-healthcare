import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Card } from "@/design-system";

describe("Card", () => {
  it("has border and no shadow", () => {
    render(<Card data-testid="c">x</Card>);
    const el = screen.getByTestId("c");
    expect(el.className).toMatch(/\bborder\b/);
    expect(el.className).not.toMatch(/shadow/);
  });

  it("uses rounded-lg (8px) for large containers", () => {
    render(<Card data-testid="c">x</Card>);
    expect(screen.getByTestId("c").className).toMatch(/rounded-lg/);
  });
});
