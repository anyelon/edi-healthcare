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
import { generateEligibilityRequest } from "@/lib/api-client";

export default function EligibilityRequestPage() {
  const [patientIds, setPatientIds] = useState("");
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setPreview(null);

    const ids = patientIds
      .split(/[,\n]/)
      .map((id) => id.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      toast.error("Please enter at least one patient ID.");
      setLoading(false);
      return;
    }

    try {
      const blob = await generateEligibilityRequest(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 270 eligibility request generated successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to generate request"
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">
          Eligibility Request
        </h1>
        <Badge variant="outline">EDI 270</Badge>
      </div>

      <div className="max-w-2xl space-y-6">
        <Card>
          <form onSubmit={handleSubmit}>
            <CardHeader>
              <CardTitle className="text-sm font-semibold">
                Generate EDI 270 Inquiry
              </CardTitle>
              <CardDescription>
                Enter patient IDs to generate an eligibility inquiry file
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Label htmlFor="patientIds">Patient IDs</Label>
                <Textarea
                  id="patientIds"
                  value={patientIds}
                  onChange={(e) => setPatientIds(e.target.value)}
                  placeholder="Enter patient IDs, one per line or comma-separated..."
                  rows={4}
                />
                <p className="text-xs text-muted-foreground">
                  Enter one or more patient IDs, separated by commas or newlines
                </p>
              </div>
            </CardContent>
            <CardFooter className="gap-2">
              <Button type="submit" disabled={loading}>
                {loading ? "Generating..." : "Generate Request"}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setPatientIds("");
                  setPreview(null);
                }}
              >
                Clear
              </Button>
            </CardFooter>
          </form>
        </Card>

        {preview && <EdiPreview content={preview} filename="270_inquiry.edi" />}
      </div>
    </div>
  );
}
