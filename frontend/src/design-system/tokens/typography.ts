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
