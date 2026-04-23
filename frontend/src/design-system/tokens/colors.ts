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
