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
