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
