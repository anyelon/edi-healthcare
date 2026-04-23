const coreSwatches = [
  { name: "primary", var: "--ds-primary" },
  { name: "secondary", var: "--ds-secondary" },
  { name: "background", var: "--ds-background" },
  { name: "surface", var: "--ds-surface" },
  { name: "foreground", var: "--ds-foreground" },
  { name: "muted-foreground", var: "--ds-muted-foreground" },
  { name: "border", var: "--ds-border" },
  { name: "outline", var: "--ds-outline" },
];

const tonalSwatches = [
  "--ds-surface-container-lowest",
  "--ds-surface-container-low",
  "--ds-surface-container",
  "--ds-surface-container-high",
  "--ds-surface-container-highest",
];

const statusSwatches = [
  { name: "success", solid: "--ds-success", bg: "--ds-success-bg", fg: "--ds-success-fg" },
  { name: "warning", solid: "--ds-warning", bg: "--ds-warning-bg", fg: "--ds-warning-fg" },
  { name: "error", solid: "--ds-error", bg: "--ds-error-bg", fg: "--ds-error-fg" },
];

const typeSamples = [
  { name: "h1", cls: "text-h1 font-[family-name:var(--font-heading)]" },
  { name: "h2", cls: "text-h2 font-[family-name:var(--font-heading)]" },
  { name: "h3", cls: "text-h3 font-[family-name:var(--font-heading)]" },
  { name: "body-lg", cls: "text-body-lg" },
  { name: "body-md", cls: "text-body-md" },
  { name: "label-md", cls: "text-label-md uppercase tracking-wider" },
  { name: "table-data", cls: "text-table-data tabular-nums" },
];

const spacingRow = [
  { name: "xs", value: "4px", cls: "w-1" },
  { name: "sm", value: "8px", cls: "w-2" },
  { name: "md", value: "16px", cls: "w-4" },
  { name: "lg", value: "24px", cls: "w-6" },
  { name: "xl", value: "32px", cls: "w-8" },
  { name: "gutter", value: "20px", cls: "w-5" },
  { name: "container-margin", value: "40px", cls: "w-10" },
];

const radiusStops = [
  { name: "sm", cls: "rounded-sm" },
  { name: "default", cls: "rounded" },
  { name: "md", cls: "rounded-md" },
  { name: "lg", cls: "rounded-lg" },
  { name: "xl", cls: "rounded-xl" },
  { name: "full", cls: "rounded-full" },
];

export function Foundations() {
  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Foundations</h2>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Core palette</h3>
        <div className="grid grid-cols-4 gap-sm">
          {coreSwatches.map((s) => (
            <div key={s.name} className="flex flex-col gap-xs">
              <div
                className="h-16 w-full rounded border"
                style={{ background: `var(${s.var})` }}
              />
              <span className="text-body-md">{s.name}</span>
              <span className="text-label-md text-muted-foreground">{s.var}</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Tonal ladder</h3>
        <div className="flex gap-xs">
          {tonalSwatches.map((v) => (
            <div
              key={v}
              className="h-12 flex-1 rounded border"
              style={{ background: `var(${v})` }}
              title={v}
            />
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Status</h3>
        <div className="grid grid-cols-3 gap-sm">
          {statusSwatches.map((s) => (
            <div key={s.name} className="flex flex-col gap-xs">
              <div className="flex h-10 rounded border">
                <div className="flex-1" style={{ background: `var(${s.solid})` }} />
                <div className="flex-1" style={{ background: `var(${s.bg})` }} />
                <div className="flex-1" style={{ background: `var(${s.fg})` }} />
              </div>
              <span className="text-body-md">{s.name}</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Typography</h3>
        <div className="flex flex-col gap-sm">
          {typeSamples.map((t) => (
            <div key={t.name} className="flex items-baseline gap-md">
              <span className="w-28 text-label-md text-muted-foreground uppercase tracking-wider">
                {t.name}
              </span>
              <span className={t.cls}>The quick brown fox jumps over the lazy dog</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Spacing</h3>
        <div className="flex flex-col gap-xs">
          {spacingRow.map((s) => (
            <div key={s.name} className="flex items-center gap-md">
              <span className="w-40 text-label-md text-muted-foreground">{s.name} ({s.value})</span>
              <div className={`${s.cls} h-2 rounded bg-ds-primary`} />
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Radius</h3>
        <div className="flex items-end gap-sm">
          {radiusStops.map((r) => (
            <div key={r.name} className="flex flex-col items-center gap-xs">
              <div className={`h-16 w-16 border ${r.cls} bg-card`} />
              <span className="text-label-md text-muted-foreground">{r.name}</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">Elevation</h3>
        <div className="flex gap-md">
          <div className="h-24 w-48 rounded-lg border bg-card p-sm">Level 1 — card</div>
          <div
            className="h-24 w-48 rounded-lg border bg-card p-sm"
            style={{ boxShadow: "var(--ds-shadow-popover)" }}
          >
            Level 2 — popover
          </div>
        </div>
      </div>
    </div>
  );
}
