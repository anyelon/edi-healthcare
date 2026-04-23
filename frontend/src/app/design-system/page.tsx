import { Foundations } from "./sections/foundations";
import { PrimitivesPreview } from "./sections/primitives";
import { ComponentsPreview } from "./sections/components";
import { PatternsPreview } from "./sections/patterns";
import { DensityPreview } from "./sections/density";

export default function DesignSystemPage() {
  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-ds-xl py-ds-lg">
      <header className="flex flex-col gap-ds-xs">
        <p className="text-label-md uppercase tracking-wider text-muted-foreground">
          Internal
        </p>
        <h1 className="text-h1 font-[family-name:var(--font-heading)]">
          Design System Preview
        </h1>
        <p className="text-body-lg text-muted-foreground">
          Source of truth: DESIGN.md and specs/012-design-system.
        </p>
        <nav className="mt-ds-sm flex flex-wrap gap-ds-sm text-body-md text-ds-secondary">
          <a href="#foundations">Foundations</a>
          <a href="#primitives">Primitives</a>
          <a href="#components">Components</a>
          <a href="#patterns">Patterns</a>
          <a href="#density">Density</a>
        </nav>
      </header>

      <section id="foundations"><Foundations /></section>
      <section id="primitives"><PrimitivesPreview /></section>
      <section id="components"><ComponentsPreview /></section>
      <section id="patterns"><PatternsPreview /></section>
      <section id="density"><DensityPreview /></section>
    </div>
  );
}
