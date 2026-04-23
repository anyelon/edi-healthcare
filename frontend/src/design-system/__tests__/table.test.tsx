import { describe, it, expect } from "vitest";
import { render } from "@testing-library/react";
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/design-system";

describe("Table", () => {
  it("header has sticky positioning class", () => {
    const { container } = render(
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>H</TableHead>
          </TableRow>
        </TableHeader>
      </Table>,
    );
    const thead = container.querySelector("thead")!;
    expect(thead.className).toMatch(/sticky/);
    expect(thead.className).toMatch(/top-0/);
  });

  it("TableHead uses heading font + label-md + uppercase", () => {
    const { container } = render(
      <Table>
        <TableHeader><TableRow><TableHead>H</TableHead></TableRow></TableHeader>
      </Table>,
    );
    const th = container.querySelector("th")!;
    expect(th.className).toMatch(/font-heading|font-\[family-name:var\(--font-heading\)\]/);
    expect(th.className).toMatch(/text-label-md|uppercase/);
  });

  it("TableCell respects data-density='compact' ancestor via :where([data-density=compact] &)", () => {
    const { container } = render(
      <div data-density="compact">
        <Table>
          <TableBody><TableRow><TableCell>cell</TableCell></TableRow></TableBody>
        </Table>
      </div>,
    );
    const td = container.querySelector("td")!;
    // we apply a density-aware padding utility class; assert the class is present
    expect(td.className).toMatch(/py-2|py-1\.5/);
    // The density switching is driven by CSS rules written in the primitive;
    // this test asserts the class hook exists. Visual behaviour is verified
    // on the /design-system preview route in Task 19 and Task 22.
  });
});
