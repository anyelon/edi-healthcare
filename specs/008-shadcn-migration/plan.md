# shadcn/ui Frontend Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all raw Tailwind CSS UI with shadcn/ui components and restructure the layout from a top navbar to a collapsible sidebar.

**Architecture:** Big-bang migration of the existing Next.js 16 frontend. Install shadcn/ui, replace the root layout with a sidebar shell, extract two shared components (EDI preview + dropzone), then migrate each of the 4 pages. No backend, API route, or business logic changes.

**Tech Stack:** Next.js 16, React 19, Tailwind v4, shadcn/ui, TypeScript, Lucide React icons

---

## File Map

| Action | File | Responsibility |
|--------|------|---------------|
| Modify | `frontend/package.json` | Add shadcn dependencies |
| Create | `frontend/components.json` | shadcn configuration |
| Modify | `frontend/src/app/globals.css` | shadcn CSS variables + Tailwind v4 |
| Create | `frontend/src/lib/utils.ts` | `cn()` class merge helper |
| Create | `frontend/src/components/ui/*.tsx` | Auto-generated shadcn components |
| Create | `frontend/src/components/app-sidebar.tsx` | Sidebar navigation |
| Modify | `frontend/src/app/layout.tsx` | Sidebar shell layout |
| Create | `frontend/src/components/edi-preview.tsx` | Shared EDI content preview |
| Create | `frontend/src/components/dropzone.tsx` | Custom file upload |
| Modify | `frontend/src/app/page.tsx` | Dashboard with shadcn Card |
| Modify | `frontend/src/app/claims/page.tsx` | Claims form with shadcn |
| Modify | `frontend/src/app/eligibility-request/page.tsx` | Eligibility request form with shadcn |
| Modify | `frontend/src/app/eligibility-response/page.tsx` | Response viewer with shadcn Table + dropzone |

---

## Task 1: Install shadcn/ui and All Components

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/components.json`
- Modify: `frontend/src/app/globals.css`
- Create: `frontend/src/lib/utils.ts`
- Create: `frontend/src/components/ui/*.tsx` (auto-generated)

- [ ] **Step 1: Initialize shadcn/ui**

Run from the `frontend/` directory:

```bash
cd frontend && npx shadcn@latest init -d
```

The `-d` flag uses defaults (New York style, Zinc color, CSS variables enabled). This creates:
- `components.json` — shadcn config
- `src/lib/utils.ts` — the `cn()` helper
- Updates `globals.css` with CSS variable definitions

**Important:** After init, check `globals.css`. shadcn will add its CSS variables. The file should start with `@import "tailwindcss";` (the existing Tailwind v4 import) followed by shadcn's variable definitions. If shadcn overwrites the file, you need to ensure `@import "tailwindcss";` is present at the top and the `@theme inline` block with font variables is preserved.

The final `globals.css` should look like:

```css
@import "tailwindcss";

@plugin "tailwindcss-animate";

@custom-variant dark (&:is(.dark *));

@theme inline {
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --font-sans: var(--font-geist-sans);
  --font-mono: var(--font-geist-mono);
  --radius: 0.625rem;
  --color-sidebar-ring: var(--sidebar-ring);
  --color-sidebar-border: var(--sidebar-border);
  --color-sidebar-accent-foreground: var(--sidebar-accent-foreground);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-primary-foreground: var(--sidebar-primary-foreground);
  --color-sidebar-primary: var(--sidebar-primary);
  --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-background: var(--sidebar-background);
  --color-chart-5: var(--chart-5);
  --color-chart-4: var(--chart-4);
  --color-chart-3: var(--chart-3);
  --color-chart-2: var(--chart-2);
  --color-chart-1: var(--chart-1);
  --color-ring: var(--ring);
  --color-input: var(--input);
  --color-border: var(--border);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-destructive: var(--destructive);
  --color-accent-foreground: var(--accent-foreground);
  --color-accent: var(--accent);
  --color-popover-foreground: var(--popover-foreground);
  --color-popover: var(--popover);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-secondary: var(--secondary);
  --color-primary-foreground: var(--primary-foreground);
  --color-primary: var(--primary);
  --color-muted-foreground: var(--muted-foreground);
  --color-muted: var(--muted);
  --color-card-foreground: var(--card-foreground);
  --color-card: var(--card);
}

:root {
  --background: oklch(1 0 0);
  --foreground: oklch(0.145 0 0);
  --card: oklch(1 0 0);
  --card-foreground: oklch(0.145 0 0);
  --popover: oklch(1 0 0);
  --popover-foreground: oklch(0.145 0 0);
  --primary: oklch(0.205 0 0);
  --primary-foreground: oklch(0.985 0 0);
  --secondary: oklch(0.97 0 0);
  --secondary-foreground: oklch(0.205 0 0);
  --muted: oklch(0.97 0 0);
  --muted-foreground: oklch(0.556 0 0);
  --accent: oklch(0.97 0 0);
  --accent-foreground: oklch(0.205 0 0);
  --destructive: oklch(0.577 0.245 27.325);
  --destructive-foreground: oklch(0.577 0.245 27.325);
  --border: oklch(0.922 0 0);
  --input: oklch(0.922 0 0);
  --ring: oklch(0.708 0 0);
  --chart-1: oklch(0.646 0.222 41.116);
  --chart-2: oklch(0.6 0.118 184.704);
  --chart-3: oklch(0.398 0.07 227.392);
  --chart-4: oklch(0.828 0.189 84.429);
  --chart-5: oklch(0.769 0.188 70.08);
  --sidebar-background: oklch(0.985 0 0);
  --sidebar-foreground: oklch(0.145 0 0);
  --sidebar-primary: oklch(0.205 0 0);
  --sidebar-primary-foreground: oklch(0.985 0 0);
  --sidebar-accent: oklch(0.97 0 0);
  --sidebar-accent-foreground: oklch(0.205 0 0);
  --sidebar-border: oklch(0.922 0 0);
  --sidebar-ring: oklch(0.708 0 0);
}

body {
  background: var(--background);
  color: var(--foreground);
  font-family: Arial, Helvetica, sans-serif;
}
```

Remove the dark mode `@media (prefers-color-scheme: dark)` block from the old `globals.css` — we are light-only.

- [ ] **Step 2: Install all required shadcn components**

Run each command from the `frontend/` directory:

```bash
cd frontend && npx shadcn@latest add sidebar button card input textarea label table badge alert separator scroll-area tooltip sonner -y
```

This installs all component files into `src/components/ui/`. The `-y` flag auto-confirms.

- [ ] **Step 3: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds with no errors. If there are CSS import issues, check that `globals.css` has the correct `@import "tailwindcss"` at the top and the `@theme inline` block.

- [ ] **Step 4: Commit**

```bash
git add frontend/
git commit -m "feat(frontend): install shadcn/ui with all required components"
```

---

## Task 2: Create Sidebar Layout and Root Layout

**Files:**
- Create: `frontend/src/components/app-sidebar.tsx`
- Modify: `frontend/src/app/layout.tsx`

- [ ] **Step 1: Create the app-sidebar component**

Create `frontend/src/components/app-sidebar.tsx`:

```tsx
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  FileText,
  ArrowRightLeft,
  FileSearch,
  Database,
} from "lucide-react";
import { toast } from "sonner";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarHeader,
  SidebarSeparator,
} from "@/components/ui/sidebar";
import { seedDatabase } from "@/lib/api-client";

const workflowItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/claims", label: "Claims (837)", icon: FileText },
  { href: "/eligibility-request", label: "Eligibility Request (270)", icon: ArrowRightLeft },
  { href: "/eligibility-response", label: "Eligibility Response (271)", icon: FileSearch },
];

export function AppSidebar() {
  const pathname = usePathname();

  async function handleSeed() {
    try {
      const result = await seedDatabase();
      toast.success(
        `Database seeded: ${result.patientIds.length} patients, ${result.encounterIds.length} encounters`
      );
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Seed failed");
    }
  }

  return (
    <Sidebar>
      <SidebarHeader className="px-4 py-4">
        <Link href="/" className="flex items-center gap-2 font-bold text-base">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-primary text-primary-foreground text-xs font-bold">
            E
          </div>
          EDI Healthcare
        </Link>
      </SidebarHeader>

      <SidebarSeparator />

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Workflows</SidebarGroupLabel>
          <SidebarMenu>
            {workflowItems.map((item) => (
              <SidebarMenuItem key={item.href}>
                <SidebarMenuButton asChild isActive={pathname === item.href}>
                  <Link href={item.href}>
                    <item.icon className="h-4 w-4" />
                    <span>{item.label}</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            ))}
          </SidebarMenu>
        </SidebarGroup>

        <SidebarSeparator />

        <SidebarGroup>
          <SidebarGroupLabel>Dev Tools</SidebarGroupLabel>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton onClick={handleSeed}>
                <Database className="h-4 w-4" />
                <span>Seed Database</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
```

- [ ] **Step 2: Rewrite the root layout**

Replace the entire contents of `frontend/src/app/layout.tsx` with:

```tsx
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { Toaster } from "@/components/ui/sonner";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

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
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
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

- [ ] **Step 3: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds. The app should render with a sidebar and existing page content (unstyled pages are fine at this point).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/app-sidebar.tsx frontend/src/app/layout.tsx
git commit -m "feat(frontend): add shadcn sidebar layout and replace top navbar"
```

---

## Task 3: Create Shared EDI Preview Component

**Files:**
- Create: `frontend/src/components/edi-preview.tsx`

- [ ] **Step 1: Create the edi-preview component**

Create `frontend/src/components/edi-preview.tsx`:

```tsx
"use client";

import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { downloadBlob } from "@/lib/api-client";

interface EdiPreviewProps {
  content: string;
  filename: string;
}

export function EdiPreview({ content, filename }: EdiPreviewProps) {
  function handleDownload() {
    downloadBlob(new Blob([content], { type: "text/plain" }), filename);
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0 pb-3">
        <CardTitle className="text-sm font-semibold">EDI Preview</CardTitle>
        <Button variant="outline" size="sm" onClick={handleDownload}>
          <Download className="mr-1 h-3 w-3" />
          Download {filename}
        </Button>
      </CardHeader>
      <CardContent>
        <ScrollArea className="h-60 rounded-md border bg-muted/50 p-4">
          <pre className="font-mono text-xs whitespace-pre-wrap">{content}</pre>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds. The component is not used yet — just confirming no import errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/edi-preview.tsx
git commit -m "feat(frontend): add shared EDI preview component with shadcn Card"
```

---

## Task 4: Create Shared Dropzone Component

**Files:**
- Create: `frontend/src/components/dropzone.tsx`

- [ ] **Step 1: Create the dropzone component**

Create `frontend/src/components/dropzone.tsx`:

```tsx
"use client";

import { useState, useRef, useCallback } from "react";
import { Upload, FileText } from "lucide-react";
import { cn } from "@/lib/utils";

interface DropzoneProps {
  onFileSelect: (file: File) => void;
  accept?: string;
}

export function Dropzone({ onFileSelect, accept = ".edi,.txt" }: DropzoneProps) {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = useCallback(
    (file: File) => {
      setSelectedFile(file.name);
      onFileSelect(file);
    },
    [onFileSelect]
  );

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragActive(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  }

  return (
    <div
      onClick={() => inputRef.current?.click()}
      onDragOver={(e) => {
        e.preventDefault();
        setDragActive(true);
      }}
      onDragEnter={(e) => {
        e.preventDefault();
        setDragActive(true);
      }}
      onDragLeave={() => setDragActive(false)}
      onDrop={handleDrop}
      className={cn(
        "cursor-pointer rounded-lg border-2 border-dashed p-10 text-center transition-colors",
        dragActive
          ? "border-primary bg-accent"
          : "border-muted-foreground/25 bg-muted/50 hover:border-muted-foreground/50"
      )}
    >
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        onChange={handleChange}
        className="hidden"
      />
      {selectedFile ? (
        <div className="flex flex-col items-center gap-2">
          <FileText className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm font-medium">{selectedFile}</p>
          <p className="text-xs text-muted-foreground">Click or drop to change file</p>
        </div>
      ) : (
        <div className="flex flex-col items-center gap-2">
          <Upload className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm font-medium">Drop your EDI file here</p>
          <p className="text-xs text-muted-foreground">Accepts .edi and .txt files</p>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/dropzone.tsx
git commit -m "feat(frontend): add custom file dropzone component with shadcn styling"
```

---

## Task 5: Migrate Dashboard Page

**Files:**
- Modify: `frontend/src/app/page.tsx`

- [ ] **Step 1: Rewrite the dashboard page**

Replace the entire contents of `frontend/src/app/page.tsx` with:

```tsx
import Link from "next/link";
import { FileText, ArrowRightLeft, FileSearch } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const workflows = [
  {
    href: "/claims",
    title: "Claims Generation",
    badge: "EDI 837P",
    description: "Generate professional claims from patient encounters.",
    detail: "Submit encounter IDs to generate downloadable EDI claim files.",
    icon: FileText,
    primary: true,
  },
  {
    href: "/eligibility-request",
    title: "Eligibility Request",
    badge: "EDI 270",
    description: "Create eligibility inquiry files for insurance verification.",
    detail: "Submit patient IDs to generate downloadable eligibility inquiry files.",
    icon: ArrowRightLeft,
    primary: false,
  },
  {
    href: "/eligibility-response",
    title: "Eligibility Response",
    badge: "EDI 271",
    description: "Parse and view eligibility response data.",
    detail: "Upload EDI 271 files to extract and display eligibility details.",
    icon: FileSearch,
    primary: false,
  },
];

export default function Dashboard() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          EDI healthcare transaction management system.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {workflows.map((wf) => (
          <Card key={wf.href}>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-semibold">{wf.title}</CardTitle>
                <Badge variant="outline">{wf.badge}</Badge>
              </div>
              <CardDescription>{wf.description}</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-xs text-muted-foreground">{wf.detail}</p>
            </CardContent>
            <CardFooter>
              <Button
                asChild
                variant={wf.primary ? "default" : "outline"}
                className="w-full"
              >
                <Link href={wf.href}>
                  <wf.icon className="mr-2 h-4 w-4" />
                  {wf.title} &rarr;
                </Link>
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  );
}
```

Note: This page is now a server component (no `"use client"`). The seed database logic has moved entirely to the sidebar. The seed result state (showing IDs) is removed — the sidebar shows a toast instead, per the spec.

- [ ] **Step 2: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/page.tsx
git commit -m "feat(frontend): migrate dashboard to shadcn Card components"
```

---

## Task 6: Migrate Claims Page

**Files:**
- Modify: `frontend/src/app/claims/page.tsx`

- [ ] **Step 1: Rewrite the claims page**

Replace the entire contents of `frontend/src/app/claims/page.tsx` with:

```tsx
"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { EdiPreview } from "@/components/edi-preview";
import { generateClaim } from "@/lib/api-client";

export default function ClaimsPage() {
  const [encounterIds, setEncounterIds] = useState("");
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setPreview(null);

    const ids = encounterIds
      .split(/[,\n]/)
      .map((id) => id.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      toast.error("Please enter at least one encounter ID.");
      setLoading(false);
      return;
    }

    try {
      const blob = await generateClaim(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 837 claim generated successfully.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to generate claim");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Claims Generation</h1>
        <Badge variant="outline">EDI 837P</Badge>
      </div>

      <div className="max-w-2xl space-y-6">
        <Card>
          <form onSubmit={handleSubmit}>
            <CardHeader>
              <CardTitle className="text-sm font-semibold">
                Generate EDI 837P Claims
              </CardTitle>
              <CardDescription>
                Enter encounter IDs to generate a professional claims file
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Label htmlFor="encounterIds">Encounter IDs</Label>
                <Textarea
                  id="encounterIds"
                  value={encounterIds}
                  onChange={(e) => setEncounterIds(e.target.value)}
                  placeholder="Enter encounter IDs, one per line or comma-separated..."
                  rows={4}
                />
                <p className="text-xs text-muted-foreground">
                  Enter one or more encounter IDs, separated by commas or newlines
                </p>
              </div>
            </CardContent>
            <CardFooter className="gap-2">
              <Button type="submit" disabled={loading}>
                {loading ? "Generating..." : "Generate Claims"}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setEncounterIds("");
                  setPreview(null);
                }}
              >
                Clear
              </Button>
            </CardFooter>
          </form>
        </Card>

        {preview && <EdiPreview content={preview} filename="837_claim.edi" />}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/claims/page.tsx
git commit -m "feat(frontend): migrate claims page to shadcn components"
```

---

## Task 7: Migrate Eligibility Request Page

**Files:**
- Modify: `frontend/src/app/eligibility-request/page.tsx`

- [ ] **Step 1: Rewrite the eligibility request page**

Replace the entire contents of `frontend/src/app/eligibility-request/page.tsx` with:

```tsx
"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { EdiPreview } from "@/components/edi-preview";
import { generateEligibilityRequest } from "@/lib/api-client";

export default function EligibilityRequestPage() {
  const [patientIds, setPatientIds] = useState("");
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setPreview(null);

    const ids = patientIds
      .split(/[,\n]/)
      .map((id) => id.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      toast.error("Please enter at least one patient ID.");
      setLoading(false);
      return;
    }

    try {
      const blob = await generateEligibilityRequest(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 270 eligibility request generated successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to generate request"
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">
          Eligibility Request
        </h1>
        <Badge variant="outline">EDI 270</Badge>
      </div>

      <div className="max-w-2xl space-y-6">
        <Card>
          <form onSubmit={handleSubmit}>
            <CardHeader>
              <CardTitle className="text-sm font-semibold">
                Generate EDI 270 Inquiry
              </CardTitle>
              <CardDescription>
                Enter patient IDs to generate an eligibility inquiry file
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Label htmlFor="patientIds">Patient IDs</Label>
                <Textarea
                  id="patientIds"
                  value={patientIds}
                  onChange={(e) => setPatientIds(e.target.value)}
                  placeholder="Enter patient IDs, one per line or comma-separated..."
                  rows={4}
                />
                <p className="text-xs text-muted-foreground">
                  Enter one or more patient IDs, separated by commas or newlines
                </p>
              </div>
            </CardContent>
            <CardFooter className="gap-2">
              <Button type="submit" disabled={loading}>
                {loading ? "Generating..." : "Generate Request"}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setPatientIds("");
                  setPreview(null);
                }}
              >
                Clear
              </Button>
            </CardFooter>
          </form>
        </Card>

        {preview && <EdiPreview content={preview} filename="270_inquiry.edi" />}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/eligibility-request/page.tsx
git commit -m "feat(frontend): migrate eligibility request page to shadcn components"
```

---

## Task 8: Migrate Eligibility Response Page

**Files:**
- Modify: `frontend/src/app/eligibility-response/page.tsx`

- [ ] **Step 1: Rewrite the eligibility response page**

Replace the entire contents of `frontend/src/app/eligibility-response/page.tsx` with:

```tsx
"use client";

import { useState, useCallback } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Dropzone } from "@/components/dropzone";
import { parseEligibilityResponse } from "@/lib/api-client";
import type { EligibilityResponse } from "@/types";

export default function EligibilityResponsePage() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<EligibilityResponse | null>(null);

  const handleFile = useCallback(async (file: File) => {
    setLoading(true);
    setResult(null);
    try {
      const response = await parseEligibilityResponse(file);
      setResult(response);
      toast.success("EDI 271 response parsed successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to parse response"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">
          Eligibility Response
        </h1>
        <Badge variant="outline">EDI 271</Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Left: Upload */}
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">
              Upload EDI 271 File
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Dropzone onFileSelect={handleFile} />
            {loading && (
              <p className="mt-3 text-sm text-muted-foreground">
                Parsing response...
              </p>
            )}
          </CardContent>
        </Card>

        {/* Right: Response Details */}
        {result && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-semibold">
                  Response Details
                </CardTitle>
                <Badge
                  variant={
                    result.eligibilityStatus
                      ?.toLowerCase()
                      .includes("active")
                      ? "default"
                      : "destructive"
                  }
                >
                  {result.eligibilityStatus || result.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Subscriber
                  </p>
                  <p className="mt-1 font-medium">
                    {result.subscriberFirstName} {result.subscriberLastName}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Member ID
                  </p>
                  <p className="mt-1 font-mono">{result.memberId}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Payer
                  </p>
                  <p className="mt-1 font-medium">{result.payerName}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Group Number
                  </p>
                  <p className="mt-1 font-mono">{result.groupNumber}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Coverage Start
                  </p>
                  <p className="mt-1">{result.coverageStartDate}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Coverage End
                  </p>
                  <p className="mt-1">{result.coverageEndDate}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Benefits Table */}
      {result?.benefits && result.benefits.length > 0 && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="text-sm font-semibold">Benefits</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>Service</TableHead>
                  <TableHead>Coverage Level</TableHead>
                  <TableHead>In-Network</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead className="text-right">Percent</TableHead>
                  <TableHead>Period</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.benefits.map((b, i) => (
                  <TableRow key={i}>
                    <TableCell>{b.benefitType}</TableCell>
                    <TableCell>{b.serviceType}</TableCell>
                    <TableCell>{b.coverageLevel}</TableCell>
                    <TableCell>
                      {b.inNetwork == null ? (
                        "-"
                      ) : (
                        <Badge variant={b.inNetwork ? "default" : "secondary"}>
                          {b.inNetwork ? "Yes" : "No"}
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right font-mono">
                      {b.amount != null ? `$${b.amount.toFixed(2)}` : "-"}
                    </TableCell>
                    <TableCell className="text-right font-mono">
                      {b.percent != null ? `${b.percent}%` : "-"}
                    </TableCell>
                    <TableCell>{b.timePeriod || "-"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {/* Error Alert */}
      {result?.errorMessage && (
        <Alert variant="destructive" className="mt-6">
          <AlertDescription>{result.errorMessage}</AlertDescription>
        </Alert>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/eligibility-response/page.tsx
git commit -m "feat(frontend): migrate eligibility response page to shadcn Table and Dropzone"
```

---

## Task 9: Final Verification and Lint

**Files:** None — verification only.

- [ ] **Step 1: Run the linter**

```bash
cd frontend && npm run lint
```

Expected: No errors. Fix any issues that appear (likely unused imports from old code).

- [ ] **Step 2: Run a production build**

```bash
cd frontend && npm run build
```

Expected: Build succeeds with no errors or warnings about missing modules.

- [ ] **Step 3: Visual smoke test**

Start the dev server and manually check each page:

```bash
cd frontend && npm run dev
```

Open `http://localhost:3000` and verify:
1. Sidebar renders with all nav items (Dashboard, Claims, Eligibility Request, Eligibility Response, Seed Database)
2. Sidebar collapses when toggled
3. Active page is highlighted in sidebar
4. Dashboard shows 3 workflow cards with badges
5. Claims page shows form card with textarea and buttons
6. Eligibility Request page shows form card with textarea and buttons
7. Eligibility Response page shows upload dropzone card
8. Seed Database in sidebar triggers a toast notification (requires MongoDB running)

- [ ] **Step 4: Commit any lint fixes**

If there were lint fixes:

```bash
git add frontend/
git commit -m "fix(frontend): resolve lint issues from shadcn migration"
```
