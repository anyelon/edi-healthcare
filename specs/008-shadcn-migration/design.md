# 007: Migrate Frontend to shadcn/ui

## Overview

Replace all raw Tailwind CSS UI in the Next.js frontend with shadcn/ui components, and restructure the layout from a top navbar to a collapsible sidebar. This is a pure UI layer swap — no behavior, routing, API, or business logic changes.

## Scope

**In scope:**
- Install and configure shadcn/ui in the frontend
- Replace the top navbar with a collapsible sidebar layout
- Migrate all 4 pages (Dashboard, Claims, Eligibility Request, Eligibility Response) to use shadcn components
- Extract two shared components: EDI preview and file dropzone
- Move "Seed Database" from dashboard page content to sidebar navigation

**Out of scope:**
- Backend changes
- New API endpoints or data-fetching (separate spec)
- Dark mode
- New features or pages
- Changes to API routes, api-client, or TypeScript types

## Tech Stack

- shadcn/ui (installed via `npx shadcn@latest init`)
- Default zinc theme, light-only
- CSS variables enabled
- Existing: Next.js 16, React 19, Tailwind v4, TypeScript

## shadcn Components

Install the following components:

| Component | Usage |
|-----------|-------|
| `sidebar` | Main app layout — collapsible sidebar navigation |
| `button` | All action buttons (submit, download, clear, seed) |
| `card` | Dashboard workflow cards, form containers, preview containers |
| `input` | Hidden file input in dropzone |
| `textarea` | Encounter ID / patient ID entry fields |
| `label` | Form field labels |
| `table` | Benefits table on eligibility response page |
| `badge` | EDI format tags (837P, 270, 271), status indicators |
| `alert` | Seed result display, error messages |
| `separator` | Visual dividers in sidebar and content |
| `scroll-area` | EDI preview content scrolling |
| `tooltip` | Sidebar collapsed state icon labels |
| `sonner` | Toast notifications for success/error feedback |

## Layout

### Sidebar Structure

```
EDI Healthcare [logo + brand]
────────────────────────────
WORKFLOWS
  Dashboard
  Claims (837)
  Eligibility Request (270)
  Eligibility Response (271)
────────────────────────────
DEV TOOLS
  Seed Database
```

- Uses shadcn `SidebarProvider` + `Sidebar` + `SidebarInset` in root `layout.tsx`
- Collapsible to icon-only mode (shadcn default behavior)
- Active page is highlighted
- "Seed Database" triggers the existing `/api/dev/seed` endpoint directly from the sidebar, with a toast for feedback

### Root Layout (`layout.tsx`)

Replaces the current `<header>` + `<nav>` with:
```
SidebarProvider
├── AppSidebar (new component)
└── SidebarInset
    └── {children}  (page content)
```

## File Structure

```
frontend/src/
├── components/
│   ├── ui/               ← shadcn auto-generated components
│   ├── app-sidebar.tsx   ← sidebar navigation component
│   ├── dropzone.tsx      ← custom file upload component
│   └── edi-preview.tsx   ← EDI content preview with download
├── app/
│   ├── layout.tsx        ← modified: sidebar layout
│   ├── globals.css       ← modified: shadcn CSS variables
│   ├── page.tsx          ← modified: dashboard with shadcn Card
│   ├── claims/page.tsx   ← modified: shadcn form + edi-preview
│   ├── eligibility-request/page.tsx  ← modified: shadcn form + edi-preview
│   ├── eligibility-response/page.tsx ← modified: dropzone + Table
│   └── api/              ← unchanged
├── lib/
│   ├── utils.ts          ← new: cn() helper (added by shadcn init)
│   └── api-client.ts     ← unchanged
└── types/
    └── index.ts          ← unchanged
```

## Page Designs

### Dashboard (`/`)

- Remove "Seed Database" button (moved to sidebar)
- Three workflow cards using `Card` + `CardHeader` + `CardContent` + `CardFooter`
  - Each card shows: title, EDI format `Badge`, description, and a `Button` link
- Seed result display (shown after seeding via sidebar) uses `Alert` component
- Cards in a 3-column responsive grid

### Claims (`/claims`)

- Form wrapped in `Card`
  - `Label` + `Textarea` for encounter IDs
  - `Button` to generate, `Button` (outline variant) to clear
- EDI output displayed via shared `edi-preview.tsx` component
- Success/error feedback via `sonner` toast

### Eligibility Request (`/eligibility-request`)

- Identical layout pattern to Claims page
- `Label` + `Textarea` for patient IDs
- Reuses the same `edi-preview.tsx` component
- Toast for feedback

### Eligibility Response (`/eligibility-response`)

- Two-column layout:
  - Left: Upload card with custom `dropzone.tsx` component
  - Right: Response details card with key-value grid and status `Badge`
- Below: Benefits `Table` with `TableHeader`, `TableBody`, `TableRow`, `TableCell`
  - Columns: Type, Service, Coverage Level, In-Network (Badge), Amount, Percent, Period

## Shared Components

### `edi-preview.tsx`

Reused by Claims and Eligibility Request pages.

**Props:**
- `content: string` — raw EDI text
- `filename: string` — download filename (e.g., "837_claim.edi")

**Structure:**
- `Card` with header showing "EDI Preview" title and a download `Button`
- `ScrollArea` containing monospace-formatted EDI content
- Download triggers a blob download of the content

### `dropzone.tsx`

Custom file upload for Eligibility Response page.

**Props:**
- `onFileSelect: (file: File) => void`
- `accept?: string` — file type filter (default: ".edi,.txt")

**Structure:**
- `Card`-styled container with dashed border
- Centered upload icon, title text, and hint text
- Hidden `<input type="file">` triggered by click
- Drag-and-drop handlers (`onDragOver`, `onDragEnter`, `onDragLeave`, `onDrop`)
- Visual states:
  - **Idle**: dashed border, muted background
  - **Drag-over**: highlighted border, slightly darker background
  - **File selected**: shows filename and a "Change file" link

### `app-sidebar.tsx`

Sidebar navigation component.

**Structure:**
- `Sidebar` + `SidebarContent` + `SidebarGroup` (x2: Workflows, Dev Tools)
- Each nav item uses `SidebarMenuButton` with icon and label
- Active state derived from current pathname via `usePathname()`
- "Seed Database" item calls `/api/dev/seed` and shows a toast with the result (toast only — no need to propagate seeded IDs to the dashboard since that's a concern for the data-driven forms spec)

## Migration Strategy

Big-bang migration in a single pass:

1. Install shadcn/ui and all required components
2. Set up sidebar layout in `layout.tsx` + create `app-sidebar.tsx`
3. Create shared components (`edi-preview.tsx`, `dropzone.tsx`)
4. Migrate each page (Dashboard → Claims → Eligibility Request → Eligibility Response)
5. Remove unused raw Tailwind markup and old layout code
6. Verify all pages render and function correctly

## What Does NOT Change

- `frontend/src/app/api/` — all BFF API routes untouched
- `frontend/src/lib/api-client.ts` — untouched
- `frontend/src/types/index.ts` — untouched
- Page routing structure — same URLs, same filenames
- All business logic — same `useState` patterns, same form handlers, same API calls
- Backend — zero changes
