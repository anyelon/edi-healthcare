"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { EdiPreview } from "@/components/edi-preview";
import { generateClaim } from "@/lib/api-client";

export default function ClaimsPage() {
  const [encounterIds, setEncounterIds] = useState("");
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setPreview(null);

    const ids = encounterIds
      .split(/[,\n]/)
      .map((id) => id.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      toast.error("Please enter at least one encounter ID.");
      setLoading(false);
      return;
    }

    try {
      const blob = await generateClaim(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 837 claim generated successfully.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to generate claim");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Claims Generation</h1>
        <Badge variant="outline">EDI 837P</Badge>
      </div>

      <div className="max-w-2xl space-y-6">
        <Card>
          <form onSubmit={handleSubmit}>
            <CardHeader>
              <CardTitle className="text-sm font-semibold">
                Generate EDI 837P Claims
              </CardTitle>
              <CardDescription>
                Enter encounter IDs to generate a professional claims file
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Label htmlFor="encounterIds">Encounter IDs</Label>
                <Textarea
                  id="encounterIds"
                  value={encounterIds}
                  onChange={(e) => setEncounterIds(e.target.value)}
                  placeholder="Enter encounter IDs, one per line or comma-separated..."
                  rows={4}
                />
                <p className="text-xs text-muted-foreground">
                  Enter one or more encounter IDs, separated by commas or newlines
                </p>
              </div>
            </CardContent>
            <CardFooter className="gap-2">
              <Button type="submit" disabled={loading}>
                {loading ? "Generating..." : "Generate Claims"}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setEncounterIds("");
                  setPreview(null);
                }}
              >
                Clear
              </Button>
            </CardFooter>
          </form>
        </Card>

        {preview && <EdiPreview content={preview} filename="837_claim.edi" />}
      </div>
    </div>
  );
}
