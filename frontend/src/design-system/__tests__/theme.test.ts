import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const cssPath = resolve(__dirname, "../styles/theme.css");

function css() {
  return readFileSync(cssPath, "utf8");
}

const REQUIRED_DS_VARS = [
  "--ds-primary", "--ds-primary-foreground",
  "--ds-secondary", "--ds-secondary-foreground",
  "--ds-background", "--ds-surface",
  "--ds-foreground", "--ds-muted-foreground",
  "--ds-border", "--ds-outline", "--ds-ring",
  "--ds-shadow-popover",
  "--ds-surface-container-lowest", "--ds-surface-container-low",
  "--ds-surface-container", "--ds-surface-container-high",
  "--ds-surface-container-highest",
  "--ds-success", "--ds-success-bg", "--ds-success-fg",
  "--ds-warning", "--ds-warning-bg", "--ds-warning-fg",
  "--ds-error", "--ds-error-bg", "--ds-error-fg",
];

const SHADCN_ALIASES: Record<string, string> = {
  "--background": "--ds-background",
  "--foreground": "--ds-foreground",
  "--card": "--ds-surface",
  "--card-foreground": "--ds-foreground",
  "--primary": "--ds-primary",
  "--primary-foreground": "--ds-primary-foreground",
  "--secondary": "--ds-secondary",
  "--secondary-foreground": "--ds-secondary-foreground",
  "--muted-foreground": "--ds-muted-foreground",
  "--border": "--ds-border",
  "--input": "--ds-border",
  "--ring": "--ds-ring",
  "--destructive": "--ds-error",
};

describe("design-system/styles/theme.css", () => {
  it("defines every required --ds-* token in :root", () => {
    const src = css();
    const missing = REQUIRED_DS_VARS.filter(
      (v) => !new RegExp(`${v}\\s*:`).test(src)
    );
    expect(missing).toEqual([]);
  });

  it("aliases shadcn vars to the --ds-* equivalents", () => {
    const src = css();
    const wrong = Object.entries(SHADCN_ALIASES).filter(
      ([shad, ds]) => !new RegExp(`${shad}\\s*:\\s*var\\(${ds}\\)`).test(src)
    );
    expect(wrong).toEqual([]);
  });
});
