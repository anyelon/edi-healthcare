---
name: Precision Billing
colors:
  surface: '#fcf8fa'
  surface-dim: '#dcd9db'
  surface-bright: '#fcf8fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3f5'
  surface-container: '#f0edef'
  surface-container-high: '#eae7e9'
  surface-container-highest: '#e4e2e4'
  on-surface: '#1b1b1d'
  on-surface-variant: '#45464d'
  inverse-surface: '#303032'
  inverse-on-surface: '#f3f0f2'
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
  tertiary-container: '#271901'
  on-tertiary-container: '#98805d'
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
  tertiary-fixed: '#fcdeb5'
  tertiary-fixed-dim: '#dec29a'
  on-tertiary-fixed: '#271901'
  on-tertiary-fixed-variant: '#574425'
  background: '#fcf8fa'
  on-background: '#1b1b1d'
  surface-variant: '#e4e2e4'
typography:
  h1:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  h2:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  h3:
    fontFamily: Manrope
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  table-data:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  container-margin: 40px
  gutter: 20px
---

## Brand & Style
The design system is engineered for the high-stakes environment of healthcare financial management. It prioritizes clarity, institutional trust, and meticulous organization to reduce cognitive load for billing administrators and healthcare providers. 

The aesthetic follows a **Corporate / Modern** style, utilizing a disciplined grid, generous whitespace, and a restrained color palette. This approach ensures that complex data sets—such as patient ledgers and insurance claims—remain legible and actionable. The interface should feel like a precise instrument: reliable, responsive, and authoritative.

## Colors
The color strategy employs a deep Navy (#0F172A) as the Primary foundation to evoke stability and institutional authority. A vibrant Blue (#2563EB) serves as the Secondary color for primary actions, links, and active states, providing a clear path for user interaction.

Semantic tokens are strictly mapped to billing statuses to ensure immediate visual recognition:
- **Paid**: Utilizes Success Green (#10B981), signaling completed transactions.
- **Pending**: Utilizes Warning Amber (#F59E0B), signaling items requiring attention or processing.
- **Overdue**: Utilizes Error Red (#EF4444), signaling urgent priority and financial risk.

The background uses a subtle off-white (#F8FAFC) to reduce screen glare during extended use, while borders use a soft Slate (#E2E8F0) to define structure without adding visual noise.

## Typography
This design system utilizes a dual-font pairing to balance modern refinement with utilitarian efficiency. **Manrope** is used for headlines to provide a sophisticated, professional character. **Inter** is used for all body text, tabular data, and labels due to its exceptional legibility at small sizes and its neutral, systematic feel.

For financial tables, maintain a 13px font size for data rows to maximize information density without sacrificing readability. Use semi-bold weights for labels and headers to create a clear typographic hierarchy.

## Layout & Spacing
The layout follows a **Fixed Grid** model for desktop dashboards to ensure data columns remain predictable and aligned across different modules. A 12-column grid is standard, with 40px outer margins and 20px gutters.

The spacing rhythm is based on a 4px baseline unit. Internal component padding should strictly adhere to these increments (e.g., 8px for tight groupings, 16px for standard card padding) to maintain a cohesive, rhythmic feel throughout the application.

## Elevation & Depth
To maintain a high-trust, professional aesthetic, this design system avoids heavy shadows. Instead, it utilizes **Tonal Layers** and **Low-Contrast Outlines**.

Depth is communicated through:
- **Level 0 (Background)**: #F8FAFC.
- **Level 1 (Cards/Surface)**: White background with a 1px border (#E2E8F0).
- **Level 2 (Modals/Popovers)**: White background with a soft, diffused shadow (0px 4px 12px rgba(15, 23, 42, 0.08)) to indicate temporary interaction layers.

This flat-but-layered approach ensures the UI feels modern and clean, avoiding the "clutter" often associated with older healthcare software.

## Shapes
The shape language is **Soft** (roundedness: 1). This involves a standard 0.25rem (4px) corner radius for most UI elements like input fields and buttons. Larger components, such as dashboard cards, may use a 0.5rem (8px) radius. 

These subtle curves soften the technical nature of billing data, making the software feel approachable while maintaining the structural integrity of a professional tool.

## Components
- **Buttons**: Primary buttons use the Navy background with white text. Secondary buttons use a transparent background with a Navy border and text. Ghost buttons use Blue text for subtle actions.
- **Status Chips**: Use high-contrast combinations. For 'Paid', use a light green background with dark green text. For 'Overdue', use a light red background with dark red text.
- **Input Fields**: Use a 1px border (#E2E8F0). On focus, the border shifts to Blue (#2563EB) with a soft 2px glow.
- **Data Tables**: Use subtle alternating row stripes or clear divider lines. Headers should be sticky with a Manrope semi-bold label.
- **Cards**: All data summary containers should have a 1px border and no shadow, unless they are being actively hovered.
- **Search & Filters**: Positioned at the top of data views, using consistent 8px spacing between elements to create a compact, efficient "Control Bar."
