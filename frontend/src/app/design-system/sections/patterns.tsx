import { SummaryCard } from "@/design-system";
import { ComponentsPreview } from "./components";

export function PatternsPreview() {
  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Patterns</h2>

      <div className="grid grid-cols-3 gap-md">
        <SummaryCard label="Claims Filed Today" metric="342" delta="+18" trend="up" />
        <SummaryCard label="Denials (7d)" metric="12" delta="-3" trend="down" />
        <SummaryCard label="Pending Review" metric="57" />
      </div>

      <ComponentsPreview />
    </div>
  );
}
