import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { FiscalInput } from "@/design-system";

function Harness({ initial = 0 }: { initial?: number }) {
  const [v, setV] = useState<number | "">(initial);
  return <FiscalInput value={v} onValueChange={setV} aria-label="amount" />;
}

describe("FiscalInput", () => {
  it("renders a $ prefix icon", () => {
    render(<Harness />);
    expect(screen.getByText("$")).toBeInTheDocument();
  });

  it("right-aligns input text and uses tabular-nums", () => {
    render(<Harness />);
    const input = screen.getByLabelText("amount");
    expect(input.className).toMatch(/text-right/);
    expect(input.className).toMatch(/tabular-nums/);
  });

  it("invokes onValueChange with numeric value", async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const input = screen.getByLabelText("amount") as HTMLInputElement;
    await user.clear(input);
    await user.type(input, "1234");
    expect(input).toHaveValue(1234);
  });
});
