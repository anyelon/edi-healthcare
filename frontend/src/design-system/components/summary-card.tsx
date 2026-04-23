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
