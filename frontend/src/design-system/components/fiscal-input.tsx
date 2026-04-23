import * as React from "react";
import { Input } from "../primitives/input";
import { cn } from "../utils/cn";

export interface FiscalInputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange"> {
  value: number | "";
  onValueChange: (value: number | "") => void;
}

export const FiscalInput = React.forwardRef<HTMLInputElement, FiscalInputProps>(
  function FiscalInput({ value, onValueChange, className, ...props }, ref) {
    return (
      <div className="relative inline-flex w-full">
        <span
          aria-hidden
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
        >
          $
        </span>
        <Input
          ref={ref}
          type="number"
          inputMode="decimal"
          className={cn("pl-6 text-right tabular-nums text-table-data", className)}
          value={value}
          onChange={(e) => {
            const raw = e.target.value;
            onValueChange(raw === "" ? "" : Number(raw));
          }}
          {...props}
        />
      </div>
    );
  },
);
