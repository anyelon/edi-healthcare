# 012 — Design System Library

## Goal

Stand up a single, encapsulated design system library inside the frontend that realises the tokens and guidance in `DESIGN.md` (the Google Stitch-generated spec) as actual CSS variables, Tailwind theme utilities, and React components. Existing pages should automatically pick up the new palette and typography. Custom billing components required by DESIGN.md (StatusBadge, SummaryCard, FiscalInput, DataTable, ControlBar) ship as part of v1. Retrofitting existing pages to the new components is explicitly out of scope.

## Non-Goals

- Retrofitting existing pages (`claims`, `eligibility-request`, `eligibility-response`, `acknowledgment`, `prior-auth`) to the new components — tracked as a follow-up spec.
- Global density preference persisted per user — density toggle exists on the preview route only for v1.
- Dark mode — DESIGN.md defines a single light palette; dark tokens are deferred.
- Publishing the library as an external npm package — it lives as an internal module.

## Source-of-Truth Resolution

`DESIGN.md` contains two partially overlapping sources: the Stitch-exported YAML front matter and the hand-written prose. Where they conflict, this spec resolves as follows:

- **Semantic brand colors (primary, secondary, background, border) → prose wins.** Prose values are prescriptive and cited directly in the component guidance ("focus shifts to Blue (#2563EB)").
- **Surface tonal ladder (`surface-container-lowest` → `-highest`) → YAML wins.** The M3-style ladder is structural and the prose does not contradict it.
- **Status colors (success / warning / error) → prose values adopted.** YAML has only `error`; prose provides the full set (#10B981 / #F59E0B / #EF4444).
- **Typography, spacing, radius scales → YAML values used as-is.** These are unambiguous.

## Token Layer

All tokens live in `frontend/src/design-system/styles/theme.css` and are exposed as CSS custom properties prefixed `--ds-*`. Shadcn's expected variables are declared as aliases of the `--ds-*` set so shadcn primitives render correctly with no fork.

### Core palette

| Role | Token | Value | Source |
|---|---|---|---|
| Primary (brand / nav) | `--ds-primary` | `#0F172A` | Prose |
| Primary foreground | `--ds-primary-foreground` | `#FFFFFF` | Derived |
| Secondary (actions / links / focus) | `--ds-secondary` | `#2563EB` | Prose |
| Secondary foreground | `--ds-secondary-foreground` | `#FFFFFF` | Derived |
| Background (Level 0) | `--ds-background` | `#F8FAFC` | Prose |
| Surface (Level 1 / cards) | `--ds-surface` | `#FFFFFF` | Prose |
| Foreground | `--ds-foreground` | `#1b1b1d` | YAML `on-surface` |
| Muted foreground | `--ds-muted-foreground` | `#45464d` | YAML `on-surface-variant` |
| Border | `--ds-border` | `#E2E8F0` | Prose |
| Outline (stronger) | `--ds-outline` | `#76777d` | YAML |
| Ring (focus) | `--ds-ring` | `#2563EB` | Prose |
| Modal shadow | `--ds-shadow-popover` | `0 4px 12px rgba(15, 23, 42, 0.08)` | Prose |

### Tonal surface ladder (YAML, kept for nested panels)

| Token | Value |
|---|---|
| `--ds-surface-container-lowest` | `#ffffff` |
| `--ds-surface-container-low` | `#f6f3f5` |
| `--ds-surface-container` | `#f0edef` |
| `--ds-surface-container-high` | `#eae7e9` |
| `--ds-surface-container-highest` | `#e4e2e4` |

### Semantic status tokens

| Status | Solid | Tinted bg | Tinted fg |
|---|---|---|---|
| Success (Paid) | `--ds-success: #10B981` | `--ds-success-bg: #ECFDF5` | `--ds-success-fg: #065F46` |
| Warning (Pending) | `--ds-warning: #F59E0B` | `--ds-warning-bg: #FFFBEB` | `--ds-warning-fg: #92400E` |
| Error (Overdue) | `--ds-error: #EF4444` | `--ds-error-bg: #FEF2F2` | `--ds-error-fg: #991B1B` |

### Typography

Fonts loaded via `next/font/google` in `design-system/styles/fonts.ts`; exposed as CSS variables `--font-heading` (Manrope) and `--font-sans` (Inter). Applied to `<html>` in the root layout.

Tailwind v4 `@theme` text utilities (defined in `theme.css`):

| Utility | Font | Size / Line | Weight | Tracking |
|---|---|---|---|---|
| `text-h1` | Manrope | 32 / 40 | 700 | -0.02em |
| `text-h2` | Manrope | 24 / 32 | 600 | -0.01em |
| `text-h3` | Manrope | 20 / 28 | 600 | — |
| `text-body-lg` | Inter | 16 / 24 | 400 | — |
| `text-body-md` | Inter | 14 / 20 | 400 | — |
| `text-label-md` | Inter | 12 / 16 | 600 | — |
| `text-table-data` | Inter | 13 / 18 | 400 | — (includes `tabular-nums`) |

### Spacing

4px baseline. Added to Tailwind's `@theme` spacing scale so utilities like `p-md`, `gap-sm`, `m-xl` resolve. These are additive to Tailwind's numeric defaults, not replacements — existing usages of `p-4`, `gap-2`, etc. continue to work.

| Token | Value | Example utility |
|---|---|---|
| `xs` | 4px | `p-xs` |
| `sm` | 8px | `gap-sm` |
| `md` | 16px | `p-md` |
| `lg` | 24px | `mt-lg` |
| `xl` | 32px | `py-xl` |
| `container-margin` | 40px | `px-container-margin` |
| `gutter` | 20px | `gap-gutter` |

### Radius

| Token | Value |
|---|---|
| `rounded-sm` | 0.125rem (2px) |
| `rounded` | 0.25rem (4px) — default |
| `rounded-md` | 0.375rem (6px) |
| `rounded-lg` | 0.5rem (8px) |
| `rounded-xl` | 0.75rem (12px) |
| `rounded-full` | 9999px |

### Elevation

- **Cards**: 1px `--ds-border`, no shadow. Optional hover shadow is a future concern.
- **Popovers / modals**: white surface, `--ds-shadow-popover`.
- No other shadow is used.

### Shadcn compatibility bridge

All shadcn variables alias to `--ds-*`:

```css
--background:       var(--ds-background);
--foreground:       var(--ds-foreground);
--card:             var(--ds-surface);
--card-foreground:  var(--ds-foreground);
--primary:          var(--ds-primary);
--primary-foreground: var(--ds-primary-foreground);
--secondary:        var(--ds-secondary);
--secondary-foreground: var(--ds-secondary-foreground);
--muted:            var(--ds-surface-container-low);
--muted-foreground: var(--ds-muted-foreground);
--accent:           var(--ds-surface-container);
--border:           var(--ds-border);
--input:            var(--ds-border);
--ring:             var(--ds-ring);
--destructive:      var(--ds-error);
/* sidebar + popover + chart vars map analogously */
```

Shadcn primitives therefore continue to work unchanged; palette swaps happen by editing `--ds-*` only.

## Library Structure

Lives at `frontend/src/design-system/`. A soft library — single-package, but with a single public API (`index.ts`) enforced by lint rule.

```
design-system/
  index.ts                    # public API — only legal import path
  tokens/
    colors.ts                 # TS exports mirroring --ds-* values
    typography.ts             # dsType.h1, dsType.tableData, ...
    spacing.ts                # dsSpacing.md, dsSpacing.gutter
    index.ts
  styles/
    theme.css                 # --ds-* vars + @theme mapping + shadcn bridge
    fonts.ts                  # next/font loaders (Manrope, Inter)
  primitives/                 # shadcn-owned, tuned to DESIGN.md
    button.tsx
    card.tsx
    input.tsx
    label.tsx
    badge.tsx
    table.tsx
    dialog.tsx
    dropdown-menu.tsx
    select.tsx
    tabs.tsx
    tooltip.tsx
    sheet.tsx
    separator.tsx
    sonner.tsx
    index.ts
  components/                 # DESIGN.md-specific compositions
    status-badge.tsx
    summary-card.tsx
    fiscal-input.tsx
    data-table.tsx
    control-bar.tsx
    index.ts
  utils/
    cn.ts
```

### Public API

`design-system/index.ts` re-exports:

- Primitives: `Button`, `Card`, `Input`, `Label`, `Badge`, `Table`, `Dialog`, `DropdownMenu`, `Select`, `Tabs`, `Tooltip`, `Sheet`, `Separator`, `Toaster`.
- Components: `StatusBadge`, `SummaryCard`, `FiscalInput`, `DataTable`, `ControlBar`.
- Tokens: `dsColors`, `dsType`, `dsSpacing`.
- Fonts: `manrope`, `inter`.

The rest of the app imports from `@/design-system` only.

### Boundary enforcement

- ESLint `no-restricted-imports` rule forbids `@/design-system/primitives/*` and `@/design-system/components/*` deep imports from outside the design system.
- `components.json` `ui` alias updated to `@/design-system/primitives` so future `shadcn add` commands land in the right folder.
- Existing `src/components/ui/*` files move into `design-system/primitives/`.
- Existing non-primitive components (`app-sidebar`, `dropzone`, `edi-preview`, `generation-layout`) stay in `src/components/` and update their imports to pull from `@/design-system`.

## Primitives — DESIGN.md Adjustments

Baseline shadcn primitives are imported once and then tuned. Key deltas from shadcn defaults:

- **Button**: `rounded` = 4px. Primary variant = `--ds-primary` navy background, white text. Secondary variant = transparent bg, 1px `--ds-primary` border + text. Ghost variant = `--ds-secondary` (blue) text, no border. Focus ring = 2px `--ds-ring`.
- **Input**: 1px `--ds-border`. Focus: border switches to `--ds-secondary`, plus a 2px soft glow. Error: `--ds-error` border + trailing error icon slot.
- **Badge**: Pill shape (`rounded-full`) when used as status indicator; rectangular (`rounded`) for generic labels. Size targets 20px height.
- **Card**: White surface, 1px `--ds-border`, no shadow. `rounded-lg` (8px).
- **Table**: Sticky header row. Header uses `--font-heading` + `text-label-md`. Sort indicator in `--ds-secondary`. Supports `data-density="compact"` on an ancestor to reduce row padding from 0.75rem to 0.25rem.
- **Dialog / DropdownMenu / Select / Tooltip**: Use `--ds-shadow-popover` for the elevation.

## DESIGN.md-Specific Components

### StatusBadge

```tsx
<StatusBadge variant="paid">Paid</StatusBadge>
```

- Variants: `paid | pending | overdue | neutral`.
- Pill shape.
- Uses the tinted bg / tinted fg tokens for each status.
- 20px height, 8px horizontal padding, `text-label-md`.

### SummaryCard

```tsx
<SummaryCard label="TOTAL OUTSTANDING" metric="$127,430.00" delta="+2.4%" trend="up" />
```

- Rendered as a `Card` (1px border, no shadow).
- `label` uses `text-label-md` uppercase.
- `metric` uses `text-h1`.
- Optional `delta` with `trend: 'up' | 'down' | 'flat'` — coloured via status tokens (`up`=success, `down`=error, `flat`=muted).

### FiscalInput

```tsx
<FiscalInput value={127430} onValueChange={...} />
```

- Wraps `Input`.
- Fixed `$` prefix icon (non-interactive, absolutely positioned).
- Right-aligned `text-table-data` with `tabular-nums`.
- Numeric keyboard on mobile, formats on blur.

### DataTable

```tsx
<DataTable
  columns={columns}
  rows={rows}
  sortKey="amount"
  sortDir="desc"
  onSortChange={...}
  selection={selection}
  onSelectionChange={...}
  bulkActions={<Button ...>Void selected</Button>}
/>
```

- Sticky header, semi-bold Manrope label (`text-label-md`).
- Active sort column indicator in `--ds-secondary`.
- Optional row selection with checkbox column.
- When `selection.size > 0`, renders a sticky bulk-action bar at the bottom of the viewport using the `bulkActions` slot.
- Honours ancestor `data-density="compact"`.

### ControlBar

```tsx
<ControlBar>
  <Input placeholder="Search claims…" />
  <Select ... />
  <Button>Filter</Button>
</ControlBar>
```

- Horizontal flex row, 8px gap between children.
- Designed to sit directly above a DataTable.

## Preview Route

Path: `frontend/src/app/design-system/page.tsx`. Not linked from the sidebar; internal documentation.

Sections (anchor-linked):

1. **Foundations** — color swatches (core + tonal ladder + status), typography specimens, spacing ruler, radius scale, elevation samples.
2. **Primitives** — every primitive in every variant × state (default / hover / focus / disabled / error).
3. **Components** — StatusBadge (all variants), SummaryCard (with/without delta), FiscalInput (empty / filled / error), DataTable (10 sample rows, sorted, with selection), ControlBar.
4. **Patterns** — dashboard row (3 SummaryCards + DataTable), form row (FiscalInputs), filter bar + table combo.
5. **Density** — side-by-side comfortable vs compact DataTable with a toggle.

## Verification

- **Manual (primary):** walk the `/design-system` preview route in the browser and compare against DESIGN.md. This is the definitive check.
- **Token sanity test:** a unit test parses `theme.css` and asserts (a) each required `--ds-*` var is defined; (b) each shadcn var aliases to a `--ds-*` var. Fails CI if a token is dropped. (The frontend has no JS test runner configured today; the implementation plan selects one — Vitest is the working assumption.)
- **Primitive / component unit tests:** each tuned primitive and each DESIGN.md-specific component ships with render tests and behaviour tests driven by TDD. Examples:
  - StatusBadge renders correct class names for each variant.
  - FiscalInput prefixes `$` and right-aligns.
  - DataTable renders sticky header, fires `onSortChange`, shows bulk bar only when selection is non-empty, respects compact density.
- **Lint rule test:** a minimal ESLint rule test that imports from a deep path and expects a lint error.

TDD is mandatory per project convention — write the failing test first, commit, then implement.

## Build Order (sequencing for the implementation plan)

1. Scaffold `design-system/` folder, empty `index.ts` barrel.
2. Token layer: `theme.css` (`--ds-*` + `@theme` + shadcn bridge), `fonts.ts`, TS token mirrors. Smoke-test: existing pages still render; palette and font visibly change.
3. Migrate `src/components/ui/*` → `design-system/primitives/*`; update `components.json` `ui` alias; rewrite imports project-wide; verify no regressions.
4. Tune primitives to DESIGN.md (Button, Input, Badge, Card, Table, Dialog/Popover shadow).
5. Build DESIGN.md-specific components (TDD each): StatusBadge → SummaryCard → FiscalInput → ControlBar → DataTable.
6. Preview route `/design-system` with the five section groups.
7. Add `no-restricted-imports` lint rule + the token-sanity test.

## Open Questions

None at spec-approval time. The YAML/prose resolution is locked above. The prose notes on "Alignment: top-aligned" and a "12-column fluid grid" are layout conventions for pages that use the library and will be consulted during the retrofit follow-up, not the library build.

## Follow-ups (explicitly out of v1)

- Retrofit existing pages (claims, eligibility-request, eligibility-response, acknowledgment, prior-auth) to the new components.
- Global density preference wired into user settings.
- Dark mode palette.
- Storybook integration (if desired later) — the preview route is the interim substitute.
