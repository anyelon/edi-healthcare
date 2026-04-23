---
name: Precision Billing System
colors:
  surface: '#fbf8ff'
  surface-dim: '#dad9e3'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f2fd'
  surface-container: '#eeedf7'
  surface-container-high: '#e8e7f1'
  surface-container-highest: '#e3e1ec'
  on-surface: '#1a1b22'
  on-surface-variant: '#45464d'
  inverse-surface: '#2f3038'
  inverse-on-surface: '#f1effa'
  outline: '#76777d'
  outline-variant: '#c6c6cd'
  surface-tint: '#565e74'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#131b2e'
  on-primary-container: '#7c839b'
  inverse-primary: '#bec6e0'
  secondary: '#0051d5'
  on-secondary: '#ffffff'
  secondary-container: '#316bf3'
  on-secondary-container: '#fefcff'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#00201d'
  on-tertiary-container: '#0c9488'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#dbe1ff'
  secondary-fixed-dim: '#b4c5ff'
  on-secondary-fixed: '#00174b'
  on-secondary-fixed-variant: '#003ea8'
  tertiary-fixed: '#89f5e7'
  tertiary-fixed-dim: '#6bd8cb'
  on-tertiary-fixed: '#00201d'
  on-tertiary-fixed-variant: '#005049'
  background: '#fbf8ff'
  on-background: '#1a1b22'
  surface-variant: '#e3e1ec'
typography:
  h1:
    fontFamily: Public Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  h2:
    fontFamily: Public Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  h3:
    fontFamily: Public Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-base:
    fontFamily: Public Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Public Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  data-mono:
    fontFamily: Public Sans
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-caps:
    fontFamily: Public Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  container-margin: 2rem
  gutter: 1rem
  section-gap: 2rem
  component-padding-x: 1rem
  component-padding-y: 0.75rem
  density-compact: 0.5rem
  density-comfortable: 1rem
---

## Brand & Style

This design system is engineered for the high-stakes environment of US healthcare finance. The brand personality is rooted in **Precision, Reliability, and Transparency**. It must facilitate the rapid processing of complex insurance claims and patient billing while maintaining an atmosphere of calm authority.

The aesthetic follows a **Corporate Modern** approach with **Minimalist** leanings. By prioritizing functional clarity over decorative elements, the system reduces cognitive load for billing specialists. The interface utilizes a "Data-First" philosophy, ensuring that critical fiscal information is never obscured. High-density information is managed through rigorous alignment and a restrained color application that highlights actionable discrepancies.

## Colors

The palette is anchored by the neutrals provided in the brand profile, supplemented with a professional "Healthcare Blue" spectrum. 

- **Primary & Secondary:** A deep Navy (#0F172A) provides institutional weight for navigation and headings, while a vibrant blue (#2563EB) is reserved for primary actions and interactive states.
- **Neutrals:** We utilize the extracted #71717A for secondary text and borders, ensuring a soft but legible contrast. #F9FAFB is the standard background for grouping elements like cards or sidebar containers.
- **Functional Status:** Crucial for billing, these colors are highly saturated to ensure "Overdue" (Error), "Pending" (Warning), and "Paid" (Success) statuses are immediately identifiable in dense tables.

## Typography

**Public Sans** is selected for its institutional clarity and exceptional legibility in data-heavy contexts. It provides a neutral, authoritative tone required for healthcare administration.

- **Data Clarity:** For monetary values and CPT codes, use the `data-mono` style. While not a true monospace, Public Sans's tabular figures ensure that numbers align vertically in columns.
- **Hierarchy:** Use `label-caps` for table headers and section overlines to differentiate metadata from primary content.
- **Readability:** Body text is set with a slightly increased line height (1.5x) to prevent "text-walling" in patient notes or insurance policy descriptions.

## Layout & Spacing

The system employs a **12-column fluid grid** with fixed sidebars. The layout is designed to maximize "above-the-fold" visibility for claim summaries.

- **Grid:** Use a 16px (1rem) base unit for the spacing rhythm. 
- **Density Toggles:** Because billing users often work with hundreds of rows, the system supports a "Compact" mode. In this mode, vertical padding in tables and lists is reduced from 0.75rem to 0.25rem.
- **Alignment:** All data points must be top-aligned in rows to maintain a consistent scanning baseline across heterogeneous content (e.g., a row containing both a multi-line address and a single-line dollar amount).

## Elevation & Depth

To maintain a "clean healthcare" feel, this design system avoids heavy shadows. Instead, it uses **Tonal Layers** and **Low-Contrast Outlines** to create depth.

- **Surface Levels:** The primary background is #FFFFFF. Secondary containers (like sidebars or card backgrounds) use #F9FAFB. 
- **Borders:** Use 1px borders in #E4E4E7 (Zinc-200) for card containers and table row separators.
- **Active Elevation:** Only use ambient shadows (e.g., `box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1)`) for temporary overlays like dropdown menus or modals. This keeps the main workspace feeling flat and focused.

## Shapes

The shape language is **Soft (0.25rem)**. This provides a professional, "standardized" appearance that feels modern without appearing overly "bubbly" or consumer-oriented.

- **Buttons & Inputs:** Standardized at 4px (0.25rem) corner radius.
- **Large Containers:** Cards and Modals may use up to 8px (0.5rem) to provide a clear visual distinction from the background.
- **Status Badges:** Use a fully rounded (pill) shape to differentiate status indicators from interactive buttons.

## Components

Following Shadcn UI patterns, components are functional and unadorned.

- **Data Tables:** The core of the application. Headers should be sticky, with "Primary Blue" indicators for sorted columns. Include a "Bulk Action" bar that appears at the bottom of the viewport when rows are selected.
- **Status Badges:** Use a light tinted background of the status color (e.g., 10% opacity) with a high-contrast text color for maximum readability.
- **Input Fields:** Use a clear focus ring in `secondary_color_hex` (Blue). Error states must include both a red border and a trailing error icon for accessibility.
- **Summary Cards:** Used at the top of dashboards to show "Total Outstanding," "Net Collection Rate," etc. These use `h1` typography for the metric and `label-caps` for the description.
- **Fiscal Inputs:** Currency inputs should always include a fixed "$" prefix icon and right-aligned text to mimic accounting standards.