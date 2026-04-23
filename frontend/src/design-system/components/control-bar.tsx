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
