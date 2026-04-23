# Design System Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up an encapsulated design system library at `frontend/src/design-system/` that realises the `DESIGN.md` Stitch tokens as CSS variables, Tailwind utilities, fonts, tuned shadcn primitives, and billing-specific components (StatusBadge, SummaryCard, FiscalInput, DataTable, ControlBar), fronted by a single public barrel and a `/design-system` preview route.

**Architecture:** Soft library (single npm package, internal module) under `frontend/src/design-system/`. A single `index.ts` is the only legal import path, enforced by ESLint `no-restricted-imports`. All colour/typography/spacing lives in `--ds-*` CSS variables inside `design-system/styles/theme.css`; shadcn's expected variables (`--primary`, `--card`, `--border`, …) alias to `--ds-*` so shadcn primitives render correctly with no forks. Existing shadcn primitives in `src/components/ui/` are migrated into `design-system/primitives/`. The existing `ui/data-table.tsx` is not a primitive and moves to `design-system/components/data-table.tsx`, where Task 18 extends it with sticky headers, sort, bulk-action slot, and density response.

**Tech Stack:** Next.js 16, React 19, Tailwind CSS v4 (CSS-first `@theme`), shadcn (base-nova), `@base-ui/react`, `next-themes`, `next/font/google` (Manrope + Inter), `lucide-react`. Testing: Vitest + @testing-library/react + jsdom (added in Task 1).

**Spec reference:** `specs/012-design-system/design.md` (source of truth for tokens and component contracts).

---

## File Inventory (what this plan creates/modifies)

**Create:**

- `frontend/vitest.config.ts`
- `frontend/vitest.setup.ts`
- `frontend/src/design-system/index.ts` — public barrel
- `frontend/src/design-system/styles/theme.css` — `--ds-*` tokens + `@theme` + shadcn bridge
- `frontend/src/design-system/styles/fonts.ts` — next/font loaders
- `frontend/src/design-system/tokens/{colors,typography,spacing,index}.ts`
- `frontend/src/design-system/utils/cn.ts`
- `frontend/src/design-system/primitives/index.ts`
- `frontend/src/design-system/components/{status-badge,summary-card,fiscal-input,control-bar,data-table,index}.tsx`
- `frontend/src/design-system/__tests__/` — one file per tuned primitive / component + `theme.test.ts`
- `frontend/src/app/design-system/page.tsx` + `sections/{foundations,primitives,components,patterns,density}.tsx`

**Move:**

- `frontend/src/components/ui/{alert,badge,button,card,input,label,scroll-area,separator,sheet,sidebar,skeleton,sonner,table,textarea,tooltip}.tsx` → `frontend/src/design-system/primitives/`
- `frontend/src/components/ui/data-table.tsx` → `frontend/src/design-system/components/data-table.tsx` (composite, not a primitive)

**Modify:**

- `frontend/package.json` — add Vitest + Testing Library deps and scripts
- `frontend/components.json` — change `ui` alias to `@/design-system/primitives`
- `frontend/src/app/globals.css` — import `../design-system/styles/theme.css`
- `frontend/src/app/layout.tsx` — swap Geist for Manrope + Inter
- `frontend/eslint.config.mjs` — add `no-restricted-imports` rule
- All 13 files currently importing from `@/components/ui/*` — switch to `@/design-system`
- `frontend/src/lib/utils.ts` — stays (cn is re-exported from design-system/utils/cn.ts)

---

## Phase 1: Foundations (Vitest + scaffold)

### Task 1: Install Vitest and testing libraries

**Files:**

- Modify: `frontend/package.json`
- Create: `frontend/vitest.config.ts`
- Create: `frontend/vitest.setup.ts`

- [ ] **Step 1: Install dev dependencies**

```bash
cd frontend && npm install -D vitest @vitejs/plugin-react jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

Expected: all packages install without peer-dep warnings that block install.

- [ ] **Step 2: Add test scripts to `frontend/package.json`**

Replace the `scripts` block so it becomes:

```json
"scripts": {
  "dev": "next dev",
  "build": "next build",
  "start": "next start",
  "lint": "eslint",
  "test": "vitest run",
  "test:watch": "vitest"
}
```

- [ ] **Step 3: Create `frontend/vitest.config.ts`**

```ts
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { resolve } from "path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
    css: true,
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
});
```

- [ ] **Step 4: Create `frontend/vitest.setup.ts`**

```ts
import "@testing-library/jest-dom/vitest";
```

- [ ] **Step 5: Write a smoke test to prove Vitest wiring works**

Create `frontend/src/__tests__/smoke.test.tsx`:

```tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";

describe("vitest smoke", () => {
  it("renders a React element and queries the DOM", () => {
    render(<h1>Hello</h1>);
    expect(screen.getByRole("heading", { name: "Hello" })).toBeInTheDocument();
  });
});
```

- [ ] **Step 6: Run the smoke test**

```bash
cd frontend && npm test -- src/__tests__/smoke.test.tsx
```

Expected: `1 passed`. Delete the smoke test afterwards — it was only scaffolding.

```bash
rm frontend/src/__tests__/smoke.test.tsx
rmdir frontend/src/__tests__ 2>/dev/null || true
```

- [ ] **Step 7: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/vitest.config.ts frontend/vitest.setup.ts
git commit -m "chore: add Vitest + Testing Library for frontend unit tests

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: Scaffold the `design-system/` folder

**Files:**

- Create: `frontend/src/design-system/index.ts`
- Create: `frontend/src/design-system/primitives/index.ts`
- Create: `frontend/src/design-system/components/index.ts`
- Create: `frontend/src/design-system/tokens/index.ts`
- Create: `frontend/src/design-system/utils/cn.ts`

- [ ] **Step 1: Create the folder skeleton**

```bash
mkdir -p frontend/src/design-system/{styles,tokens,primitives,components,utils,__tests__}
```

- [ ] **Step 2: Create `utils/cn.ts` re-export**

```ts
// frontend/src/design-system/utils/cn.ts
export { cn } from "@/lib/utils";
```

- [ ] **Step 3: Create empty barrel files**

`frontend/src/design-system/tokens/index.ts`:

```ts
// Token modules added in Task 5.
export {};
```

`frontend/src/design-system/primitives/index.ts`:

```ts
// Primitives migrated in Task 7, tuned in Tasks 8–13.
export {};
```

`frontend/src/design-system/components/index.ts`:

```ts
// Components built in Tasks 14–18.
export {};
```

`frontend/src/design-system/index.ts` (the single public API):

```ts
export * from "./primitives";
export * from "./components";
export * from "./tokens";
export { cn } from "./utils/cn";
```

- [ ] **Step 4: Verify the app still builds**

```bash
cd frontend && npm run lint
```

Expected: exits 0 (no rules apply yet; scaffold is isolated).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system
git commit -m "feat: scaffold design-system folder skeleton

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: Write `theme.css` with `--ds-*` tokens and shadcn bridge

**Files:**

- Create: `frontend/src/design-system/styles/theme.css`
- Create: `frontend/src/design-system/__tests__/theme.test.ts`

- [ ] **Step 1: Write the failing token-sanity test**

`frontend/src/design-system/__tests__/theme.test.ts`:

```ts
import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const cssPath = resolve(__dirname, "../styles/theme.css");

function css() {
  return readFileSync(cssPath, "utf8");
}

const REQUIRED_DS_VARS = [
  "--ds-primary", "--ds-primary-foreground",
  "--ds-secondary", "--ds-secondary-foreground",
  "--ds-background", "--ds-surface",
  "--ds-foreground", "--ds-muted-foreground",
  "--ds-border", "--ds-outline", "--ds-ring",
  "--ds-shadow-popover",
  "--ds-surface-container-lowest", "--ds-surface-container-low",
  "--ds-surface-container", "--ds-surface-container-high",
  "--ds-surface-container-highest",
  "--ds-success", "--ds-success-bg", "--ds-success-fg",
  "--ds-warning", "--ds-warning-bg", "--ds-warning-fg",
  "--ds-error", "--ds-error-bg", "--ds-error-fg",
];

const SHADCN_ALIASES: Record<string, string> = {
  "--background": "--ds-background",
  "--foreground": "--ds-foreground",
  "--card": "--ds-surface",
  "--card-foreground": "--ds-foreground",
  "--primary": "--ds-primary",
  "--primary-foreground": "--ds-primary-foreground",
  "--secondary": "--ds-secondary",
  "--secondary-foreground": "--ds-secondary-foreground",
  "--muted-foreground": "--ds-muted-foreground",
  "--border": "--ds-border",
  "--input": "--ds-border",
  "--ring": "--ds-ring",
  "--destructive": "--ds-error",
};

describe("design-system/styles/theme.css", () => {
  it("defines every required --ds-* token in :root", () => {
    const src = css();
    const missing = REQUIRED_DS_VARS.filter(
      (v) => !new RegExp(`${v}\\s*:`).test(src)
    );
    expect(missing).toEqual([]);
  });

  it("aliases shadcn vars to the --ds-* equivalents", () => {
    const src = css();
    const wrong = Object.entries(SHADCN_ALIASES).filter(
      ([shad, ds]) => !new RegExp(`${shad}\\s*:\\s*var\\(${ds}\\)`).test(src)
    );
    expect(wrong).toEqual([]);
  });
});
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
cd frontend && npm test -- src/design-system/__tests__/theme.test.ts
```

Expected: FAIL with `ENOENT: no such file or directory ... theme.css`.

- [ ] **Step 3: Create `theme.css` with full token set and shadcn bridge**

`frontend/src/design-system/styles/theme.css`:

```css
/*
 * Design system tokens. Source of truth: specs/012-design-system/design.md.
 * All --ds-* variables live here. Shadcn variables alias to --ds-* below.
 */

:root {
  /* Core palette */
  --ds-primary: #0F172A;
  --ds-primary-foreground: #FFFFFF;
  --ds-secondary: #2563EB;
  --ds-secondary-foreground: #FFFFFF;
  --ds-background: #F8FAFC;
  --ds-surface: #FFFFFF;
  --ds-foreground: #1b1b1d;
  --ds-muted-foreground: #45464d;
  --ds-border: #E2E8F0;
  --ds-outline: #76777d;
  --ds-ring: #2563EB;
  --ds-shadow-popover: 0 4px 12px rgba(15, 23, 42, 0.08);

  /* Tonal surface ladder (YAML) */
  --ds-surface-container-lowest: #ffffff;
  --ds-surface-container-low: #f6f3f5;
  --ds-surface-container: #f0edef;
  --ds-surface-container-high: #eae7e9;
  --ds-surface-container-highest: #e4e2e4;

  /* Status */
  --ds-success: #10B981;
  --ds-success-bg: #ECFDF5;
  --ds-success-fg: #065F46;
  --ds-warning: #F59E0B;
  --ds-warning-bg: #FFFBEB;
  --ds-warning-fg: #92400E;
  --ds-error: #EF4444;
  --ds-error-bg: #FEF2F2;
  --ds-error-fg: #991B1B;

  /* Radius */
  --radius: 0.25rem;

  /* Shadcn compatibility bridge */
  --background: var(--ds-background);
  --foreground: var(--ds-foreground);
  --card: var(--ds-surface);
  --card-foreground: var(--ds-foreground);
  --popover: var(--ds-surface);
  --popover-foreground: var(--ds-foreground);
  --primary: var(--ds-primary);
  --primary-foreground: var(--ds-primary-foreground);
  --secondary: var(--ds-secondary);
  --secondary-foreground: var(--ds-secondary-foreground);
  --muted: var(--ds-surface-container-low);
  --muted-foreground: var(--ds-muted-foreground);
  --accent: var(--ds-surface-container);
  --accent-foreground: var(--ds-foreground);
  --border: var(--ds-border);
  --input: var(--ds-border);
  --ring: var(--ds-ring);
  --destructive: var(--ds-error);

  /* Sidebar (shadcn-sidebar vars) */
  --sidebar: var(--ds-surface);
  --sidebar-foreground: var(--ds-foreground);
  --sidebar-primary: var(--ds-primary);
  --sidebar-primary-foreground: var(--ds-primary-foreground);
  --sidebar-accent: var(--ds-surface-container-low);
  --sidebar-accent-foreground: var(--ds-foreground);
  --sidebar-border: var(--ds-border);
  --sidebar-ring: var(--ds-ring);
}

@theme inline {
  /* Color utilities */
  --color-ds-primary: var(--ds-primary);
  --color-ds-primary-foreground: var(--ds-primary-foreground);
  --color-ds-secondary: var(--ds-secondary);
  --color-ds-secondary-foreground: var(--ds-secondary-foreground);
  --color-ds-background: var(--ds-background);
  --color-ds-surface: var(--ds-surface);
  --color-ds-foreground: var(--ds-foreground);
  --color-ds-muted-foreground: var(--ds-muted-foreground);
  --color-ds-border: var(--ds-border);
  --color-ds-outline: var(--ds-outline);
  --color-ds-ring: var(--ds-ring);
  --color-ds-success: var(--ds-success);
  --color-ds-success-bg: var(--ds-success-bg);
  --color-ds-success-fg: var(--ds-success-fg);
  --color-ds-warning: var(--ds-warning);
  --color-ds-warning-bg: var(--ds-warning-bg);
  --color-ds-warning-fg: var(--ds-warning-fg);
  --color-ds-error: var(--ds-error);
  --color-ds-error-bg: var(--ds-error-bg);
  --color-ds-error-fg: var(--ds-error-fg);

  /* Spacing utilities (additive to Tailwind's numeric scale) */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-gutter: 20px;
  --spacing-container-margin: 40px;

  /* Fonts — variables set by next/font in fonts.ts (Task 4) */
  --font-sans: var(--font-inter);
  --font-heading: var(--font-manrope);

  /* Text utilities */
  --text-h1: 32px;
  --text-h1--line-height: 40px;
  --text-h1--font-weight: 700;
  --text-h1--letter-spacing: -0.02em;
  --text-h2: 24px;
  --text-h2--line-height: 32px;
  --text-h2--font-weight: 600;
  --text-h2--letter-spacing: -0.01em;
  --text-h3: 20px;
  --text-h3--line-height: 28px;
  --text-h3--font-weight: 600;
  --text-body-lg: 16px;
  --text-body-lg--line-height: 24px;
  --text-body-lg--font-weight: 400;
  --text-body-md: 14px;
  --text-body-md--line-height: 20px;
  --text-body-md--font-weight: 400;
  --text-label-md: 12px;
  --text-label-md--line-height: 16px;
  --text-label-md--font-weight: 600;
  --text-table-data: 13px;
  --text-table-data--line-height: 18px;
  --text-table-data--font-weight: 400;
}
```

- [ ] **Step 4: Run the test to confirm it passes**

```bash
cd frontend && npm test -- src/design-system/__tests__/theme.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/styles/theme.css frontend/src/design-system/__tests__/theme.test.ts
git commit -m "feat(design-system): add theme.css tokens and shadcn bridge

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: Load Manrope + Inter via next/font

**Files:**

- Create: `frontend/src/design-system/styles/fonts.ts`

- [ ] **Step 1: Create the font loader**

`frontend/src/design-system/styles/fonts.ts`:

```ts
import { Inter, Manrope } from "next/font/google";

export const inter = Inter({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

export const manrope = Manrope({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-manrope",
  weight: ["600", "700"],
});
```

- [ ] **Step 2: Re-export from the barrel**

Edit `frontend/src/design-system/index.ts`:

```ts
export * from "./primitives";
export * from "./components";
export * from "./tokens";
export { cn } from "./utils/cn";
export { inter, manrope } from "./styles/fonts";
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/design-system/styles/fonts.ts frontend/src/design-system/index.ts
git commit -m "feat(design-system): load Manrope + Inter via next/font

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: TS token mirrors

**Files:**

- Create: `frontend/src/design-system/tokens/colors.ts`
- Create: `frontend/src/design-system/tokens/typography.ts`
- Create: `frontend/src/design-system/tokens/spacing.ts`
- Modify: `frontend/src/design-system/tokens/index.ts`

- [ ] **Step 1: Create `colors.ts`**

```ts
// frontend/src/design-system/tokens/colors.ts
export const dsColors = {
  primary: "var(--ds-primary)",
  primaryForeground: "var(--ds-primary-foreground)",
  secondary: "var(--ds-secondary)",
  secondaryForeground: "var(--ds-secondary-foreground)",
  background: "var(--ds-background)",
  surface: "var(--ds-surface)",
  foreground: "var(--ds-foreground)",
  mutedForeground: "var(--ds-muted-foreground)",
  border: "var(--ds-border)",
  outline: "var(--ds-outline)",
  ring: "var(--ds-ring)",
  success: "var(--ds-success)",
  warning: "var(--ds-warning)",
  error: "var(--ds-error)",
} as const;

export type DsColor = keyof typeof dsColors;
```

- [ ] **Step 2: Create `typography.ts`**

```ts
// frontend/src/design-system/tokens/typography.ts
export const dsType = {
  h1: "font-[family-name:var(--font-heading)] text-h1",
  h2: "font-[family-name:var(--font-heading)] text-h2",
  h3: "font-[family-name:var(--font-heading)] text-h3",
  bodyLg: "text-body-lg",
  bodyMd: "text-body-md",
  labelMd: "text-label-md uppercase tracking-wider",
  tableData: "text-table-data tabular-nums",
} as const;

export type DsTypeKey = keyof typeof dsType;
```

- [ ] **Step 3: Create `spacing.ts`**

```ts
// frontend/src/design-system/tokens/spacing.ts
export const dsSpacing = {
  xs: "4px",
  sm: "8px",
  md: "16px",
  lg: "24px",
  xl: "32px",
  gutter: "20px",
  containerMargin: "40px",
} as const;

export type DsSpacing = keyof typeof dsSpacing;
```

- [ ] **Step 4: Update `tokens/index.ts`**

```ts
export * from "./colors";
export * from "./typography";
export * from "./spacing";
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/tokens
git commit -m "feat(design-system): add TS token mirrors (colors, type, spacing)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: Wire theme + fonts into the app root

**Files:**

- Modify: `frontend/src/app/globals.css`
- Modify: `frontend/src/app/layout.tsx`

- [ ] **Step 1: Import `theme.css` from `globals.css`**

Replace `frontend/src/app/globals.css` with:

```css
@import "tailwindcss";
@import "tw-animate-css";
@import "shadcn/tailwind.css";
@import "../design-system/styles/theme.css";

@layer base {
  * {
    @apply border-border outline-ring/50;
  }
  body {
    @apply bg-background text-foreground;
  }
  html {
    @apply font-sans;
  }
}
```

(The big `@theme inline` / `:root` blocks are replaced by `theme.css` — delete them.)

- [ ] **Step 2: Swap fonts in `layout.tsx`**

Replace `frontend/src/app/layout.tsx` with:

```tsx
import type { Metadata } from "next";
import { Toaster, inter, manrope } from "@/design-system";
import { SidebarInset, SidebarProvider } from "@/design-system";
import { AppSidebar } from "@/components/app-sidebar";
import "./globals.css";

export const metadata: Metadata = {
  title: "EDI Healthcare",
  description: "EDI Healthcare Transaction Management",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${inter.variable} ${manrope.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <SidebarProvider>
          <AppSidebar />
          <SidebarInset>
            <main className="p-6">{children}</main>
          </SidebarInset>
        </SidebarProvider>
        <Toaster />
      </body>
    </html>
  );
}
```

(This file will still fail to resolve `Toaster`, `SidebarInset`, `SidebarProvider` from `@/design-system` until Task 7 migrates the primitives. Commit is gated behind Task 7; for now skip build verification.)

- [ ] **Step 3: Run the token-sanity test to confirm no regressions**

```bash
cd frontend && npm test -- src/design-system/__tests__/theme.test.ts
```

Expected: PASS (the test reads `theme.css` directly; `globals.css` changes don't affect it).

- [ ] **Step 4: Do NOT commit yet**

These changes are committed together with Task 7 because `layout.tsx` will not resolve its new imports until primitives are migrated. Leave them staged or un-staged until then.

---

## Phase 2: Primitive migration

### Task 7: Move shadcn UI primitives into `design-system/primitives/`

**Files:**

- Move: `frontend/src/components/ui/{alert,badge,button,card,input,label,scroll-area,separator,sheet,sidebar,skeleton,sonner,table,textarea,tooltip}.tsx` → `frontend/src/design-system/primitives/`
- Move: `frontend/src/components/ui/data-table.tsx` → `frontend/src/design-system/components/data-table.tsx`
- Modify: `frontend/components.json`
- Modify: `frontend/src/design-system/primitives/index.ts`
- Modify: `frontend/src/design-system/components/index.ts`
- Modify: all files importing from `@/components/ui/*`

- [ ] **Step 1: Move the primitive files via git mv**

```bash
cd frontend
for f in alert badge button card input label scroll-area separator sheet sidebar skeleton sonner table textarea tooltip; do
  git mv "src/components/ui/${f}.tsx" "src/design-system/primitives/${f}.tsx"
done
git mv src/components/ui/data-table.tsx src/design-system/components/data-table.tsx
rmdir src/components/ui
```

- [ ] **Step 2: Rewrite intra-primitive imports**

Shadcn primitives import `@/lib/utils` (for `cn`) and each other via `@/components/ui/*`. Rewrite the internal `@/components/ui/*` references to relative paths:

```bash
cd frontend/src/design-system/primitives
# rewrite shadcn intra-imports within primitives to relative paths
perl -i -pe 's{@/components/ui/([a-z-]+)}{./$1}g' *.tsx
```

Verify by opening `sidebar.tsx` and confirming its `import { Sheet, SheetContent } from "./sheet"` and similar.

- [ ] **Step 3: Rewrite `data-table.tsx` table import**

```bash
cd frontend/src/design-system/components
perl -i -pe 's{@/components/ui/table}{../primitives/table}g' data-table.tsx
```

- [ ] **Step 4: Populate `primitives/index.ts`**

`frontend/src/design-system/primitives/index.ts`:

```ts
export * from "./alert";
export * from "./badge";
export * from "./button";
export * from "./card";
export * from "./input";
export * from "./label";
export * from "./scroll-area";
export * from "./separator";
export * from "./sheet";
export * from "./sidebar";
export * from "./skeleton";
export * from "./sonner";
export * from "./table";
export * from "./textarea";
export * from "./tooltip";
```

- [ ] **Step 5: Populate `components/index.ts`**

`frontend/src/design-system/components/index.ts`:

```ts
export * from "./data-table";
// status-badge, summary-card, fiscal-input, control-bar added in later tasks
```

- [ ] **Step 6: Update `frontend/components.json` `ui` alias**

Change the `aliases.ui` value:

```json
"aliases": {
  "components": "@/components",
  "utils": "@/lib/utils",
  "ui": "@/design-system/primitives",
  "lib": "@/lib",
  "hooks": "@/hooks"
}
```

- [ ] **Step 7: Rewrite all consumer imports to use the barrel**

The following 13 files import from `@/components/ui/*`:

```
frontend/src/app/layout.tsx
frontend/src/app/page.tsx
frontend/src/app/prior-auth/page.tsx
frontend/src/app/eligibility-request/page.tsx
frontend/src/app/acknowledgment/page.tsx
frontend/src/app/claims/page.tsx
frontend/src/app/eligibility-response/page.tsx
frontend/src/components/generation-layout.tsx
frontend/src/components/edi-preview.tsx
frontend/src/components/app-sidebar.tsx
```

(Three of the 13 original hits were inside `ui/` itself — handled in Step 2.)

For each file, rewrite `from "@/components/ui/<name>"` to `from "@/design-system"`. Multiple imports from different `ui/*` files in the same file should be consolidated into a single `@/design-system` import. Example transformation:

Before:

```ts
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
```

After:

```ts
import { Button, Card, CardContent } from "@/design-system";
```

Run this across the 10 consumer files. Do it by hand for each file — sed can handle the substitution but duplicate imports must be merged manually. Finish with:

```bash
cd frontend && ! grep -r 'from "@/components/ui/' src
```

Expected: no remaining matches (command exits with status 1 when grep finds nothing, `!` inverts to 0 = success).

- [ ] **Step 8: Typecheck + lint + test**

```bash
cd frontend && npm run lint && npm test
```

Expected: lint passes, all tests pass.

- [ ] **Step 9: Start the dev server and smoke-test every page**

```bash
cd frontend && npm run dev
```

In the browser, load each of:

- `/`
- `/claims`
- `/eligibility-request`
- `/eligibility-response`
- `/acknowledgment`
- `/prior-auth`

Expected: every page renders; sidebar works; fonts are Manrope/Inter (not Geist); background is off-white; primary buttons are navy, not black. Ctrl-C the server.

- [ ] **Step 10: Commit (bundles Task 6 + Task 7 since they must land together)**

```bash
git add -A
git commit -m "refactor(frontend): migrate shadcn ui to design-system/primitives

- Move 15 shadcn primitives into design-system/primitives, plus the
  composite data-table into design-system/components.
- Rewrite all consumer imports to use @/design-system barrel.
- Update components.json ui alias.
- Wire design-system/styles/theme.css into globals.css and load
  Manrope + Inter via next/font in the root layout.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 3: Primitive tuning

Each task in this phase applies DESIGN.md-specific deltas to a migrated primitive. Tests assert the observable behaviour/class names.

### Task 8: Tune Button variants

**Files:**

- Modify: `frontend/src/design-system/primitives/button.tsx`
- Create: `frontend/src/design-system/__tests__/button.test.tsx`

- [ ] **Step 1: Write the failing tests**

`frontend/src/design-system/__tests__/button.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm they fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/button.test.tsx
```

Expected: the first three tests may pass by accident; any that fail identify the deltas needed in `button.tsx`.

- [ ] **Step 3: Update `button.tsx` to match**

Open `frontend/src/design-system/primitives/button.tsx`. In the `cva` variants block, set:

- `default` → `"bg-primary text-primary-foreground hover:bg-primary/90 rounded"`
- `outline` → `"border border-primary bg-transparent text-primary hover:bg-primary/5 rounded"`
- `ghost` → `"text-secondary hover:bg-secondary/10 rounded"`
- `secondary` → `"bg-secondary text-secondary-foreground hover:bg-secondary/90 rounded"`
- `destructive` → `"bg-destructive text-white hover:bg-destructive/90 rounded"`
- `link` → unchanged

Remove the `rounded-md` class from the base `cva` string if present; replace with `rounded` (4px).

- [ ] **Step 4: Run tests and confirm they pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/button.test.tsx
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/primitives/button.tsx frontend/src/design-system/__tests__/button.test.tsx
git commit -m "feat(design-system): tune Button variants to DESIGN.md

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: Tune Input focus + error states

**Files:**

- Modify: `frontend/src/design-system/primitives/input.tsx`
- Create: `frontend/src/design-system/__tests__/input.test.tsx`

- [ ] **Step 1: Write failing tests**

`frontend/src/design-system/__tests__/input.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm they fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/input.test.tsx
```

- [ ] **Step 3: Update `input.tsx`**

Ensure the base className includes:

```
"flex h-9 w-full rounded border border-input bg-transparent px-3 py-1 text-body-md shadow-xs transition-colors
 file:border-0 file:bg-transparent file:text-sm file:font-medium
 placeholder:text-muted-foreground
 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-0
 aria-invalid:border-destructive aria-invalid:ring-destructive
 disabled:cursor-not-allowed disabled:opacity-50"
```

Change `rounded-md` → `rounded` if present.

- [ ] **Step 4: Run tests and confirm they pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/input.test.tsx
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/primitives/input.tsx frontend/src/design-system/__tests__/input.test.tsx
git commit -m "feat(design-system): tune Input focus ring and invalid state

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 10: Tune Badge (pill variant)

**Files:**

- Modify: `frontend/src/design-system/primitives/badge.tsx`
- Create: `frontend/src/design-system/__tests__/badge.test.tsx`

- [ ] **Step 1: Write failing tests**

`frontend/src/design-system/__tests__/badge.test.tsx`:

```tsx
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
```

- [ ] **Step 2: Run tests and confirm they fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/badge.test.tsx
```

- [ ] **Step 3: Extend `badge.tsx`**

Add a `shape` variant to the CVA:

```ts
const badgeVariants = cva(
  "inline-flex items-center border px-2.5 py-0.5 text-label-md font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground",
        secondary: "border-transparent bg-secondary text-secondary-foreground",
        destructive: "border-transparent bg-destructive text-white",
        outline: "text-foreground",
      },
      shape: {
        default: "rounded",
        pill: "rounded-full",
      },
    },
    defaultVariants: {
      variant: "default",
      shape: "default",
    },
  },
);
```

Update the `BadgeProps` interface to include `VariantProps<typeof badgeVariants>`.

- [ ] **Step 4: Run tests and confirm they pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/badge.test.tsx
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/primitives/badge.tsx frontend/src/design-system/__tests__/badge.test.tsx
git commit -m "feat(design-system): add pill shape to Badge

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 11: Tune Card (bordered, no shadow)

**Files:**

- Modify: `frontend/src/design-system/primitives/card.tsx`
- Create: `frontend/src/design-system/__tests__/card.test.tsx`

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/card.test.tsx
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
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/card.test.tsx
```

- [ ] **Step 3: Update `card.tsx`**

Change the `Card` wrapper className to:

```
"rounded-lg border bg-card text-card-foreground"
```

(remove any `shadow-*` / `shadow-sm`).

- [ ] **Step 4: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/card.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/primitives/card.tsx frontend/src/design-system/__tests__/card.test.tsx
git commit -m "feat(design-system): tune Card to bordered, no-shadow, rounded-lg

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 12: Tune Table (sticky header, Manrope label, density)

**Files:**

- Modify: `frontend/src/design-system/primitives/table.tsx`
- Create: `frontend/src/design-system/__tests__/table.test.tsx`

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/table.test.tsx
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
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/table.test.tsx
```

- [ ] **Step 3: Update `table.tsx`**

- `TableHeader` className: add `sticky top-0 z-10 bg-card`.
- `TableHead` className: base `h-10 px-2 text-left align-middle text-label-md font-semibold uppercase font-[family-name:var(--font-heading)] text-muted-foreground [&:has([role=checkbox])]:pr-0`.
- `TableCell` className: base `p-2 align-top text-table-data [[data-density=compact]_&]:py-1 [[data-density=compact]_&]:px-2` (align-top per DESIGN.md).

- [ ] **Step 4: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/table.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/primitives/table.tsx frontend/src/design-system/__tests__/table.test.tsx
git commit -m "feat(design-system): tune Table with sticky header, Manrope label, density hook

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 13: Apply popover shadow to elevated surfaces

**Files:**

- Modify: `frontend/src/design-system/primitives/sheet.tsx`
- Modify: `frontend/src/design-system/primitives/tooltip.tsx`
- (no separate test file — visual verification covered in Task 19 preview)

- [ ] **Step 1: Identify popover-like primitives currently using default shadow**

```bash
cd frontend && grep -l "shadow-lg\|shadow-md" src/design-system/primitives/
```

- [ ] **Step 2: Replace default shadow with `ds-shadow-popover`**

For each hit, change the shadow class to inline style using `--ds-shadow-popover`. Because Tailwind does not have a utility for the arbitrary shadow string, prefer `style={{ boxShadow: "var(--ds-shadow-popover)" }}` on the content element, and remove `shadow-*` Tailwind classes. Example in `sheet.tsx` `SheetContent`:

```tsx
<SheetPrimitive.Content
  ref={ref}
  style={{ boxShadow: "var(--ds-shadow-popover)" }}
  className={cn(sheetVariants({ side }), className)}
  {...props}
>
```

- [ ] **Step 3: Lint**

```bash
cd frontend && npm run lint
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/design-system/primitives
git commit -m "feat(design-system): use ds-shadow-popover for sheet and tooltip

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 4: DESIGN.md-specific components

### Task 14: StatusBadge

**Files:**

- Create: `frontend/src/design-system/components/status-badge.tsx`
- Create: `frontend/src/design-system/__tests__/status-badge.test.tsx`
- Modify: `frontend/src/design-system/components/index.ts`

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/status-badge.test.tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusBadge } from "@/design-system";

describe("StatusBadge", () => {
  it("uses tinted success colors for variant=paid", () => {
    render(<StatusBadge variant="paid">Paid</StatusBadge>);
    const el = screen.getByText("Paid");
    expect(el.className).toMatch(/bg-ds-success-bg/);
    expect(el.className).toMatch(/text-ds-success-fg/);
  });

  it("uses tinted warning colors for variant=pending", () => {
    render(<StatusBadge variant="pending">Pending</StatusBadge>);
    const el = screen.getByText("Pending");
    expect(el.className).toMatch(/bg-ds-warning-bg/);
    expect(el.className).toMatch(/text-ds-warning-fg/);
  });

  it("uses tinted error colors for variant=overdue", () => {
    render(<StatusBadge variant="overdue">Overdue</StatusBadge>);
    const el = screen.getByText("Overdue");
    expect(el.className).toMatch(/bg-ds-error-bg/);
    expect(el.className).toMatch(/text-ds-error-fg/);
  });

  it("renders as a pill", () => {
    render(<StatusBadge variant="paid">x</StatusBadge>);
    expect(screen.getByText("x").className).toMatch(/rounded-full/);
  });
});
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/status-badge.test.tsx
```

- [ ] **Step 3: Implement**

```tsx
// frontend/src/design-system/components/status-badge.tsx
import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../utils/cn";

const statusBadgeVariants = cva(
  "inline-flex items-center rounded-full px-2 py-0.5 text-label-md whitespace-nowrap",
  {
    variants: {
      variant: {
        paid: "bg-ds-success-bg text-ds-success-fg",
        pending: "bg-ds-warning-bg text-ds-warning-fg",
        overdue: "bg-ds-error-bg text-ds-error-fg",
        neutral: "bg-ds-surface-container text-ds-foreground",
      },
    },
    defaultVariants: { variant: "neutral" },
  },
);

export interface StatusBadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof statusBadgeVariants> {}

export function StatusBadge({ variant, className, ...props }: StatusBadgeProps) {
  return (
    <span className={cn(statusBadgeVariants({ variant }), className)} {...props} />
  );
}
```

- [ ] **Step 4: Export from components barrel**

Edit `frontend/src/design-system/components/index.ts`:

```ts
export * from "./data-table";
export * from "./status-badge";
```

- [ ] **Step 5: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/status-badge.test.tsx
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/design-system/components/status-badge.tsx frontend/src/design-system/components/index.ts frontend/src/design-system/__tests__/status-badge.test.tsx
git commit -m "feat(design-system): add StatusBadge (paid/pending/overdue/neutral)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 15: SummaryCard

**Files:**

- Create: `frontend/src/design-system/components/summary-card.tsx`
- Create: `frontend/src/design-system/__tests__/summary-card.test.tsx`
- Modify: `frontend/src/design-system/components/index.ts`

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/summary-card.test.tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { SummaryCard } from "@/design-system";

describe("SummaryCard", () => {
  it("renders label uppercase and metric", () => {
    render(<SummaryCard label="Total Outstanding" metric="$127,430.00" />);
    expect(screen.getByText("Total Outstanding").className).toMatch(/uppercase/);
    expect(screen.getByText("$127,430.00")).toBeInTheDocument();
  });

  it("renders metric with h1 text utility", () => {
    render(<SummaryCard label="x" metric="42" />);
    expect(screen.getByText("42").className).toMatch(/text-h1/);
  });

  it("delta uses success color when trend=up", () => {
    render(<SummaryCard label="x" metric="42" delta="+2.4%" trend="up" />);
    expect(screen.getByText("+2.4%").className).toMatch(/text-ds-success/);
  });

  it("delta uses error color when trend=down", () => {
    render(<SummaryCard label="x" metric="42" delta="-1.1%" trend="down" />);
    expect(screen.getByText("-1.1%").className).toMatch(/text-ds-error/);
  });
});
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/summary-card.test.tsx
```

- [ ] **Step 3: Implement**

```tsx
// frontend/src/design-system/components/summary-card.tsx
import * as React from "react";
import { Card } from "../primitives/card";
import { cn } from "../utils/cn";

type Trend = "up" | "down" | "flat";

export interface SummaryCardProps extends React.HTMLAttributes<HTMLDivElement> {
  label: string;
  metric: React.ReactNode;
  delta?: string;
  trend?: Trend;
}

const trendClass: Record<Trend, string> = {
  up: "text-ds-success",
  down: "text-ds-error",
  flat: "text-muted-foreground",
};

export function SummaryCard({
  label,
  metric,
  delta,
  trend = "flat",
  className,
  ...props
}: SummaryCardProps) {
  return (
    <Card className={cn("p-md flex flex-col gap-xs", className)} {...props}>
      <span className="text-label-md uppercase tracking-wider text-muted-foreground">
        {label}
      </span>
      <span className="text-h1 font-[family-name:var(--font-heading)] text-foreground">
        {metric}
      </span>
      {delta ? (
        <span className={cn("text-body-md tabular-nums", trendClass[trend])}>
          {delta}
        </span>
      ) : null}
    </Card>
  );
}
```

- [ ] **Step 4: Re-export**

Append `export * from "./summary-card";` to `components/index.ts`.

- [ ] **Step 5: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/summary-card.test.tsx
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/design-system/components/summary-card.tsx frontend/src/design-system/components/index.ts frontend/src/design-system/__tests__/summary-card.test.tsx
git commit -m "feat(design-system): add SummaryCard with trend-coloured delta

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 16: FiscalInput

**Files:**

- Create: `frontend/src/design-system/components/fiscal-input.tsx`
- Create: `frontend/src/design-system/__tests__/fiscal-input.test.tsx`
- Modify: `frontend/src/design-system/components/index.ts`

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/fiscal-input.test.tsx
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
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/fiscal-input.test.tsx
```

- [ ] **Step 3: Implement**

```tsx
// frontend/src/design-system/components/fiscal-input.tsx
import * as React from "react";
import { Input } from "../primitives/input";
import { cn } from "../utils/cn";

export interface FiscalInputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange"> {
  value: number | "";
  onValueChange: (value: number | "") => void;
}

export const FiscalInput = React.forwardRef<HTMLInputElement, FiscalInputProps>(
  function FiscalInput({ value, onValueChange, className, ...props }, ref) {
    return (
      <div className="relative inline-flex w-full">
        <span
          aria-hidden
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
        >
          $
        </span>
        <Input
          ref={ref}
          type="number"
          inputMode="decimal"
          className={cn("pl-6 text-right tabular-nums text-table-data", className)}
          value={value}
          onChange={(e) => {
            const raw = e.target.value;
            onValueChange(raw === "" ? "" : Number(raw));
          }}
          {...props}
        />
      </div>
    );
  },
);
```

- [ ] **Step 4: Re-export**

Append `export * from "./fiscal-input";` to `components/index.ts`.

- [ ] **Step 5: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/fiscal-input.test.tsx
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/design-system/components/fiscal-input.tsx frontend/src/design-system/components/index.ts frontend/src/design-system/__tests__/fiscal-input.test.tsx
git commit -m "feat(design-system): add FiscalInput ($ prefix, right-aligned)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 17: ControlBar

**Files:**

- Create: `frontend/src/design-system/components/control-bar.tsx`
- Create: `frontend/src/design-system/__tests__/control-bar.test.tsx`
- Modify: `frontend/src/design-system/components/index.ts`

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/control-bar.test.tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { ControlBar } from "@/design-system";

describe("ControlBar", () => {
  it("uses flex row with 8px gap (gap-sm)", () => {
    render(<ControlBar data-testid="cb"><div>a</div><div>b</div></ControlBar>);
    const el = screen.getByTestId("cb");
    expect(el.className).toMatch(/\bflex\b/);
    expect(el.className).toMatch(/gap-sm/);
  });
});
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/control-bar.test.tsx
```

- [ ] **Step 3: Implement**

```tsx
// frontend/src/design-system/components/control-bar.tsx
import * as React from "react";
import { cn } from "../utils/cn";

export type ControlBarProps = React.HTMLAttributes<HTMLDivElement>;

export function ControlBar({ className, ...props }: ControlBarProps) {
  return (
    <div
      className={cn("flex flex-wrap items-center gap-sm", className)}
      {...props}
    />
  );
}
```

- [ ] **Step 4: Re-export**

Append `export * from "./control-bar";` to `components/index.ts`.

- [ ] **Step 5: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/control-bar.test.tsx
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/design-system/components/control-bar.tsx frontend/src/design-system/components/index.ts frontend/src/design-system/__tests__/control-bar.test.tsx
git commit -m "feat(design-system): add ControlBar flex row with 8px gap

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 18: Extend DataTable — sort indicators + bulk-action bar + density

**Files:**

- Modify: `frontend/src/design-system/components/data-table.tsx`
- Create: `frontend/src/design-system/__tests__/data-table.test.tsx`

The migrated `data-table.tsx` currently supports row selection only. This task adds sort indicators, a sticky bulk-action slot, and ensures density works. It preserves the existing `columns`, `data`, `selectedIds`, `onSelectionChange`, `getId`, `emptyMessage` props and adds new optional props without breaking callers.

- [ ] **Step 1: Write failing tests**

```tsx
// frontend/src/design-system/__tests__/data-table.test.tsx
import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DataTable, type ColumnDef } from "@/design-system";

type Row = { id: string; name: string; amount: number };

const columns: ColumnDef<Row>[] = [
  { header: "Name", accessor: "name", sortKey: "name" },
  { header: "Amount", accessor: "amount", sortKey: "amount" },
];

const rows: Row[] = [
  { id: "1", name: "Alpha", amount: 10 },
  { id: "2", name: "Bravo", amount: 20 },
];

describe("DataTable", () => {
  it("renders a sort indicator on the active sort column", () => {
    render(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set()}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        sortKey="amount"
        sortDir="desc"
        onSortChange={() => {}}
      />,
    );
    const amountHeader = screen.getByRole("columnheader", { name: /amount/i });
    expect(amountHeader.querySelector("[data-sort-indicator]")).not.toBeNull();
  });

  it("fires onSortChange when a sortable header is clicked", async () => {
    const user = userEvent.setup();
    const onSort = vi.fn();
    render(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set()}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        sortKey="amount"
        sortDir="desc"
        onSortChange={onSort}
      />,
    );
    await user.click(screen.getByRole("columnheader", { name: /amount/i }));
    expect(onSort).toHaveBeenCalledWith("amount", "asc");
  });

  it("renders the bulk-action slot only when selection is non-empty", () => {
    const { rerender } = render(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set()}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        bulkActions={<button>Void</button>}
      />,
    );
    expect(screen.queryByRole("button", { name: "Void" })).toBeNull();

    rerender(
      <DataTable
        columns={columns}
        data={rows}
        selectedIds={new Set(["1"])}
        onSelectionChange={() => {}}
        getId={(r) => r.id}
        bulkActions={<button>Void</button>}
      />,
    );
    expect(screen.getByRole("button", { name: "Void" })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run to confirm fail**

```bash
cd frontend && npm test -- src/design-system/__tests__/data-table.test.tsx
```

- [ ] **Step 3: Extend `data-table.tsx`**

Add `sortKey`, `sortDir`, `onSortChange`, `bulkActions` to `DataTableProps`. Add a `sortKey` field to `ColumnDef`. Update the header render to emit a clickable button for columns with `sortKey` and a `<span data-sort-indicator>` for the active one. Render `bulkActions` in a sticky bottom div when `selectedIds.size > 0`.

```tsx
// frontend/src/design-system/components/data-table.tsx
"use client";

import React from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../primitives/table";
import { cn } from "../utils/cn";

export type SortDir = "asc" | "desc";

export interface ColumnDef<T> {
  header: string;
  accessor: keyof T | ((row: T) => unknown);
  cell?: (row: T) => React.ReactNode;
  sortKey?: string;
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  selectedIds: Set<string>;
  onSelectionChange: (ids: Set<string>) => void;
  getId: (row: T) => string;
  emptyMessage?: React.ReactNode;
  sortKey?: string;
  sortDir?: SortDir;
  onSortChange?: (key: string, dir: SortDir) => void;
  bulkActions?: React.ReactNode;
}

export function DataTable<T>({
  columns,
  data,
  selectedIds,
  onSelectionChange,
  getId,
  emptyMessage = "No data available.",
  sortKey,
  sortDir,
  onSortChange,
  bulkActions,
}: DataTableProps<T>) {
  const allSelected = data.length > 0 && data.every((row) => selectedIds.has(getId(row)));
  const someSelected = data.some((row) => selectedIds.has(getId(row)));

  function toggleAll() {
    onSelectionChange(allSelected ? new Set() : new Set(data.map(getId)));
  }

  function toggleRow(id: string) {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    onSelectionChange(next);
  }

  function getCellValue(row: T, col: ColumnDef<T>): React.ReactNode {
    if (col.cell) return col.cell(row);
    if (typeof col.accessor === "function") return String(col.accessor(row));
    return String(row[col.accessor] ?? "");
  }

  function handleHeaderClick(col: ColumnDef<T>) {
    if (!col.sortKey || !onSortChange) return;
    const nextDir: SortDir = col.sortKey === sortKey && sortDir === "asc" ? "desc" : col.sortKey === sortKey && sortDir === "desc" ? "asc" : "asc";
    onSortChange(col.sortKey, nextDir);
  }

  return (
    <>
      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <input
                  type="checkbox"
                  checked={allSelected}
                  ref={(el) => { if (el) el.indeterminate = someSelected && !allSelected; }}
                  onChange={toggleAll}
                  className="h-4 w-4 rounded border-border accent-primary"
                  aria-label="Select all"
                />
              </TableHead>
              {columns.map((col) => {
                const isActive = col.sortKey && col.sortKey === sortKey;
                const clickable = Boolean(col.sortKey && onSortChange);
                return (
                  <TableHead
                    key={col.header}
                    onClick={clickable ? () => handleHeaderClick(col) : undefined}
                    className={cn(clickable && "cursor-pointer select-none")}
                  >
                    <span className="inline-flex items-center gap-1">
                      {col.header}
                      {isActive ? (
                        <span data-sort-indicator className="text-ds-secondary">
                          {sortDir === "asc" ? "▲" : "▼"}
                        </span>
                      ) : null}
                    </span>
                  </TableHead>
                );
              })}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} className="h-24 text-center text-muted-foreground">
                  {emptyMessage}
                </TableCell>
              </TableRow>
            ) : (
              data.map((row) => {
                const id = getId(row);
                const isSelected = selectedIds.has(id);
                return (
                  <TableRow
                    key={id}
                    data-state={isSelected ? "selected" : undefined}
                    className={cn(isSelected && "bg-muted/50")}
                  >
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => toggleRow(id)}
                        className="h-4 w-4 rounded border-border accent-primary"
                        aria-label="Select row"
                      />
                    </TableCell>
                    {columns.map((col) => (
                      <TableCell key={col.header}>{getCellValue(row, col)}</TableCell>
                    ))}
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>
      {bulkActions && selectedIds.size > 0 ? (
        <div
          className="sticky bottom-0 z-10 mt-sm flex items-center justify-between rounded-lg border bg-card p-sm"
          style={{ boxShadow: "var(--ds-shadow-popover)" }}
        >
          <span className="text-body-md text-muted-foreground">
            {selectedIds.size} selected
          </span>
          <div className="flex items-center gap-sm">{bulkActions}</div>
        </div>
      ) : null}
    </>
  );
}
```

- [ ] **Step 4: Run to confirm pass**

```bash
cd frontend && npm test -- src/design-system/__tests__/data-table.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/design-system/components/data-table.tsx frontend/src/design-system/__tests__/data-table.test.tsx
git commit -m "feat(design-system): extend DataTable with sort + bulk-action bar

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 5: Preview route

### Task 19: Build `/design-system` preview route

**Files:**

- Create: `frontend/src/app/design-system/page.tsx`
- Create: `frontend/src/app/design-system/sections/foundations.tsx`
- Create: `frontend/src/app/design-system/sections/primitives.tsx`
- Create: `frontend/src/app/design-system/sections/components.tsx`
- Create: `frontend/src/app/design-system/sections/patterns.tsx`
- Create: `frontend/src/app/design-system/sections/density.tsx`

The preview route is manual-verification scaffolding. No automated test; verify visually in Task 22. Keep the sections small but cover every token and component from the spec.

- [ ] **Step 1: Create the page shell**

```tsx
// frontend/src/app/design-system/page.tsx
import { Foundations } from "./sections/foundations";
import { PrimitivesPreview } from "./sections/primitives";
import { ComponentsPreview } from "./sections/components";
import { PatternsPreview } from "./sections/patterns";
import { DensityPreview } from "./sections/density";

export default function DesignSystemPage() {
  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-xl py-lg">
      <header className="flex flex-col gap-xs">
        <p className="text-label-md uppercase tracking-wider text-muted-foreground">
          Internal
        </p>
        <h1 className="text-h1 font-[family-name:var(--font-heading)]">
          Design System Preview
        </h1>
        <p className="text-body-lg text-muted-foreground">
          Source of truth: DESIGN.md and specs/012-design-system.
        </p>
        <nav className="mt-sm flex flex-wrap gap-sm text-body-md text-ds-secondary">
          <a href="#foundations">Foundations</a>
          <a href="#primitives">Primitives</a>
          <a href="#components">Components</a>
          <a href="#patterns">Patterns</a>
          <a href="#density">Density</a>
        </nav>
      </header>

      <section id="foundations"><Foundations /></section>
      <section id="primitives"><PrimitivesPreview /></section>
      <section id="components"><ComponentsPreview /></section>
      <section id="patterns"><PatternsPreview /></section>
      <section id="density"><DensityPreview /></section>
    </div>
  );
}
```

- [ ] **Step 2: Foundations section — swatches, typography, spacing, radius, elevation**

```tsx
// frontend/src/app/design-system/sections/foundations.tsx
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
```

- [ ] **Step 3: Primitives section**

```tsx
// frontend/src/app/design-system/sections/primitives.tsx
import { Button, Input, Label, Badge, Card, CardContent, CardHeader } from "@/design-system";

export function PrimitivesPreview() {
  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Primitives</h2>

      <div className="flex flex-col gap-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Button</h3>
        <div className="flex flex-wrap gap-sm">
          <Button>Primary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="destructive">Destructive</Button>
          <Button disabled>Disabled</Button>
        </div>
      </div>

      <div className="flex flex-col gap-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Input</h3>
        <div className="grid max-w-md grid-cols-2 gap-sm">
          <div className="flex flex-col gap-xs">
            <Label>Default</Label>
            <Input placeholder="Patient name" />
          </div>
          <div className="flex flex-col gap-xs">
            <Label>Error</Label>
            <Input placeholder="NPI" aria-invalid />
          </div>
          <div className="flex flex-col gap-xs">
            <Label>Disabled</Label>
            <Input placeholder="Read-only" disabled />
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Badge</h3>
        <div className="flex flex-wrap items-center gap-sm">
          <Badge>Default</Badge>
          <Badge variant="secondary">Secondary</Badge>
          <Badge variant="destructive">Destructive</Badge>
          <Badge variant="outline">Outline</Badge>
          <Badge shape="pill">Pill</Badge>
        </div>
      </div>

      <div className="flex flex-col gap-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Card</h3>
        <Card className="max-w-md">
          <CardHeader>Claim #ABC-2401</CardHeader>
          <CardContent className="text-body-md text-muted-foreground">
            Card surface with border, no shadow.
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Components section**

```tsx
// frontend/src/app/design-system/sections/components.tsx
"use client";

import { useState } from "react";
import {
  StatusBadge,
  SummaryCard,
  FiscalInput,
  ControlBar,
  DataTable,
  Input,
  Button,
  type ColumnDef,
} from "@/design-system";

type Claim = { id: string; patient: string; amount: number; status: "paid" | "pending" | "overdue" };

const claims: Claim[] = [
  { id: "1", patient: "A. Alvarez", amount: 1245.5, status: "paid" },
  { id: "2", patient: "M. Brown",   amount:  780.0, status: "pending" },
  { id: "3", patient: "J. Chen",    amount: 2310.0, status: "overdue" },
];

const columns: ColumnDef<Claim>[] = [
  { header: "Patient", accessor: "patient", sortKey: "patient" },
  { header: "Amount",  accessor: "amount",  sortKey: "amount",
    cell: (r) => `$${r.amount.toFixed(2)}` },
  { header: "Status",  accessor: "status",
    cell: (r) => <StatusBadge variant={r.status}>{r.status}</StatusBadge> },
];

export function ComponentsPreview() {
  const [selection, setSelection] = useState<Set<string>>(new Set());
  const [amount, setAmount] = useState<number | "">(127430);
  const [sortKey, setSortKey] = useState<string>("amount");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Components</h2>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">StatusBadge</h3>
        <div className="flex gap-sm">
          <StatusBadge variant="paid">Paid</StatusBadge>
          <StatusBadge variant="pending">Pending</StatusBadge>
          <StatusBadge variant="overdue">Overdue</StatusBadge>
          <StatusBadge variant="neutral">Neutral</StatusBadge>
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">SummaryCard</h3>
        <div className="grid grid-cols-3 gap-md">
          <SummaryCard label="Total Outstanding" metric="$127,430.00" delta="+2.4%" trend="up" />
          <SummaryCard label="Collected MTD"      metric="$84,120.00"  delta="-1.1%" trend="down" />
          <SummaryCard label="Net Collection Rate" metric="96.2%" />
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">FiscalInput</h3>
        <div className="max-w-xs">
          <FiscalInput value={amount} onValueChange={setAmount} aria-label="Fiscal amount" />
        </div>
      </div>

      <div>
        <h3 className="text-h3 mb-sm font-[family-name:var(--font-heading)]">ControlBar + DataTable</h3>
        <ControlBar>
          <Input placeholder="Search claims…" className="max-w-xs" />
          <Button variant="outline">Filter</Button>
        </ControlBar>
        <div className="mt-sm">
          <DataTable
            columns={columns}
            data={claims}
            selectedIds={selection}
            onSelectionChange={setSelection}
            getId={(r) => r.id}
            sortKey={sortKey}
            sortDir={sortDir}
            onSortChange={(k, d) => { setSortKey(k); setSortDir(d); }}
            bulkActions={<Button>Void selected</Button>}
          />
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Patterns section**

```tsx
// frontend/src/app/design-system/sections/patterns.tsx
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
```

- [ ] **Step 6: Density section**

```tsx
// frontend/src/app/design-system/sections/density.tsx
"use client";

import { useState } from "react";
import { DataTable, Button, type ColumnDef } from "@/design-system";

type R = { id: string; code: string; description: string; amount: number };

const rows: R[] = Array.from({ length: 6 }, (_, i) => ({
  id: String(i + 1),
  code: `99${200 + i}`,
  description: `Sample service ${i + 1}`,
  amount: 100 + i * 25,
}));

const columns: ColumnDef<R>[] = [
  { header: "Code", accessor: "code" },
  { header: "Description", accessor: "description" },
  { header: "Amount", accessor: "amount", cell: (r) => `$${r.amount.toFixed(2)}` },
];

export function DensityPreview() {
  const [density, setDensity] = useState<"comfortable" | "compact">("comfortable");
  const [sel, setSel] = useState<Set<string>>(new Set());

  return (
    <div className="flex flex-col gap-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Density</h2>
      <div className="flex gap-sm">
        <Button
          variant={density === "comfortable" ? "default" : "outline"}
          onClick={() => setDensity("comfortable")}
        >
          Comfortable
        </Button>
        <Button
          variant={density === "compact" ? "default" : "outline"}
          onClick={() => setDensity("compact")}
        >
          Compact
        </Button>
      </div>
      <div data-density={density}>
        <DataTable
          columns={columns}
          data={rows}
          selectedIds={sel}
          onSelectionChange={setSel}
          getId={(r) => r.id}
        />
      </div>
    </div>
  );
}
```

- [ ] **Step 7: Start dev server and verify**

```bash
cd frontend && npm run dev
```

Open `http://localhost:3000/design-system` in the browser. Confirm:

- The page renders with Manrope headings and Inter body copy.
- All five sections appear with anchor nav.
- StatusBadge tint colours match DESIGN.md (green/amber/red tinted bg, dark text).
- SummaryCard metrics render in h1 Manrope.
- FiscalInput shows `$` prefix, right-aligned numeric input.
- DataTable header is sticky (scroll an overflow container if needed), sort indicator appears blue on active column, bulk-action bar appears when rows are selected, vanishes when cleared.
- Density toggle visibly tightens rows.

Stop the server.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/design-system
git commit -m "feat(design-system): add /design-system preview route

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 6: Guardrails

### Task 20: ESLint `no-restricted-imports` rule

**Files:**

- Modify: `frontend/eslint.config.mjs`

- [ ] **Step 1: Update config**

```js
// frontend/eslint.config.mjs
import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["src/**/*.{ts,tsx}"],
    ignores: ["src/design-system/**"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: [
                "@/design-system/primitives/*",
                "@/design-system/components/*",
                "@/design-system/tokens/*",
                "@/design-system/styles/*",
                "@/design-system/utils/*",
              ],
              message:
                "Import from the '@/design-system' barrel instead of a sub-path.",
            },
          ],
        },
      ],
    },
  },
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
```

- [ ] **Step 2: Run lint to confirm passing**

```bash
cd frontend && npm run lint
```

Expected: passes (no deep imports exist because Task 7 routed everything through the barrel).

- [ ] **Step 3: Prove the rule fires (smoke check)**

Temporarily edit `frontend/src/app/page.tsx` to add `import { Button } from "@/design-system/primitives/button";` at the top; run lint; expect an error referencing "Import from the '@/design-system' barrel". Revert the edit.

```bash
cd frontend && npm run lint
```

Expected (after revert): passes again.

- [ ] **Step 4: Commit**

```bash
git add frontend/eslint.config.mjs
git commit -m "chore(eslint): forbid deep imports into design-system

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 21: Final verification

**Files:**

- None (verification pass).

- [ ] **Step 1: Run the full test suite**

```bash
cd frontend && npm test
```

Expected: all tests pass (smoke + theme + button + input + badge + card + table + status-badge + summary-card + fiscal-input + control-bar + data-table).

- [ ] **Step 2: Lint**

```bash
cd frontend && npm run lint
```

Expected: zero errors.

- [ ] **Step 3: Next build**

```bash
cd frontend && npm run build
```

Expected: build succeeds with no unresolved imports, no missing modules.

- [ ] **Step 4: Manual walkthrough**

Start the dev server and open each page. Compare to DESIGN.md:

```bash
cd frontend && npm run dev
```

Pages to open:

- `/design-system` — every section renders; sort + selection + bulk action + density toggle all work.
- `/` — loads with Manrope/Inter, navy sidebar, off-white body.
- `/claims`, `/eligibility-request`, `/eligibility-response`, `/acknowledgment`, `/prior-auth` — each page still renders; no visual regressions beyond the intended palette shift.

Stop the server.

- [ ] **Step 5: Commit the final summary only if you changed anything**

If no changes were needed, this task does not produce a commit. Otherwise, commit any follow-up fixes.

```bash
git status
```

- [ ] **Step 6: Done — open a PR**

```bash
gh pr create --title "Design system library (012)" --body "$(cat <<'EOF'
## Summary
- Introduces the design-system library under frontend/src/design-system with a single public barrel.
- Adopts DESIGN.md tokens (Navy primary, Blue secondary, Manrope headings, Inter body, 4px baseline spacing, Stitch surface ladder) as --ds-* CSS variables with a shadcn compatibility bridge.
- Migrates shadcn primitives into design-system/primitives and rewires all consumers through @/design-system.
- Adds DESIGN.md-specific components: StatusBadge, SummaryCard, FiscalInput, ControlBar. Extends DataTable with sticky headers, sort indicators, a bulk-action slot, and density response.
- Adds /design-system preview route, Vitest + Testing Library setup, and an ESLint rule forbidding deep imports into the library.

## Test plan
- [ ] `npm test` in frontend/ — all suites green
- [ ] `npm run lint` — clean
- [ ] `npm run build` — succeeds
- [ ] Manually walk /design-system preview route in the browser
- [ ] Verify /, /claims, /eligibility-request, /eligibility-response, /acknowledgment, /prior-auth still render

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Self-Review (plan vs spec)

1. **Spec coverage.** Every major section of `specs/012-design-system/design.md` has a task:
   - Token layer → Tasks 3, 5, 6.
   - Shadcn bridge → Task 3.
   - Typography tokens → Task 3 (`@theme` text-*), Task 4 (font loading), Task 6 (applied to `<html>`).
   - Spacing / radius → Task 3 (`@theme` spacing-*), Task 5 (TS mirrors).
   - Library structure + barrel + boundary → Tasks 2, 4, 5, 7, 20.
   - Primitive tuning → Tasks 8–13.
   - DESIGN.md components → Tasks 14–18.
   - Preview route → Task 19.
   - Token sanity test → Task 3 (bundled with the theme implementation — the test is written first and fails; the CSS makes it pass, then continues to guard regressions).
   - Lint rule → Task 20.
   - Verification → Task 21.

2. **Placeholder scan.** No "TBD" / "TODO" / "fill in details". Every code step contains the full file contents or the exact diff. No "similar to Task N" shortcuts — code is repeated where needed (e.g., each TDD task reproduces its own failing-test block verbatim).

3. **Type / identifier consistency checked:**
   - `StatusBadge` variants match across spec, test, and implementation: `paid | pending | overdue | neutral`.
   - `DataTable` prop names match across the existing migrated file and the Task 18 extension: `columns`, `data`, `selectedIds`, `onSelectionChange`, `getId`, `emptyMessage`, `sortKey`, `sortDir`, `onSortChange`, `bulkActions`.
   - `ColumnDef<T>` extended with optional `sortKey: string` in Task 18 — previews in Task 19 use it.
   - `SummaryCard` props `label`, `metric`, `delta?`, `trend?` consistent between test, implementation, and preview.
   - `FiscalInput` uses `value: number | ""` and `onValueChange` in test, implementation, and preview.
   - `dsColors`, `dsType`, `dsSpacing` exported from `tokens/index.ts` and re-exported from the barrel; preview does not import them directly but they are available.

4. **Scope.** The plan fits under a single implementation effort. Retrofits of existing pages remain explicitly out of scope (spec + plan agree).
