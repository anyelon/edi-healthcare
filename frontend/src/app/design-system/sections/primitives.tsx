import { Button, Input, Label, Badge, Card, CardContent, CardHeader } from "@/design-system";

export function PrimitivesPreview() {
  return (
    <div className="flex flex-col gap-ds-lg">
      <h2 className="text-h2 font-[family-name:var(--font-heading)]">Primitives</h2>

      <div className="flex flex-col gap-ds-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Button</h3>
        <div className="flex flex-wrap gap-ds-sm">
          <Button>Primary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="destructive">Destructive</Button>
          <Button disabled>Disabled</Button>
        </div>
      </div>

      <div className="flex flex-col gap-ds-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Input</h3>
        <div className="grid max-w-md grid-cols-2 gap-ds-sm">
          <div className="flex flex-col gap-ds-xs">
            <Label>Default</Label>
            <Input placeholder="Patient name" />
          </div>
          <div className="flex flex-col gap-ds-xs">
            <Label>Error</Label>
            <Input placeholder="NPI" aria-invalid />
          </div>
          <div className="flex flex-col gap-ds-xs">
            <Label>Disabled</Label>
            <Input placeholder="Read-only" disabled />
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-ds-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Badge</h3>
        <div className="flex flex-wrap items-center gap-ds-sm">
          <Badge>Default</Badge>
          <Badge variant="secondary">Secondary</Badge>
          <Badge variant="destructive">Destructive</Badge>
          <Badge variant="outline">Outline</Badge>
          <Badge shape="pill">Pill</Badge>
        </div>
      </div>

      <div className="flex flex-col gap-ds-sm">
        <h3 className="text-h3 font-[family-name:var(--font-heading)]">Card</h3>
        <Card className="max-w-md">
          <CardHeader>Claim #ABC-2401</CardHeader>
          <CardContent className="text-body-md text-muted-foreground">
            Card surface with border, no shadow.
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
