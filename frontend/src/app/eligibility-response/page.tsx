"use client";

import { useState, useCallback } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Dropzone } from "@/components/dropzone";
import { parseEligibilityResponse } from "@/lib/api-client";
import type { EligibilityResponse } from "@/types";

export default function EligibilityResponsePage() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<EligibilityResponse | null>(null);

  const handleFile = useCallback(async (file: File) => {
    setLoading(true);
    setResult(null);
    try {
      const response = await parseEligibilityResponse(file);
      setResult(response);
      toast.success("EDI 271 response parsed successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to parse response"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">
          Eligibility Response
        </h1>
        <Badge variant="outline">EDI 271</Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Left: Upload */}
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">
              Upload EDI 271 File
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Dropzone onFileSelect={handleFile} />
            {loading && (
              <p className="mt-3 text-sm text-muted-foreground">
                Parsing response...
              </p>
            )}
          </CardContent>
        </Card>

        {/* Right: Response Details */}
        {result && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-semibold">
                  Response Details
                </CardTitle>
                <Badge
                  variant={
                    result.eligibilityStatus
                      ?.toLowerCase()
                      .includes("active")
                      ? "default"
                      : "destructive"
                  }
                >
                  {result.eligibilityStatus || result.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Subscriber
                  </p>
                  <p className="mt-1 font-medium">
                    {result.subscriberFirstName} {result.subscriberLastName}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Member ID
                  </p>
                  <p className="mt-1 font-mono">{result.memberId}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Payer
                  </p>
                  <p className="mt-1 font-medium">{result.payerName}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Group Number
                  </p>
                  <p className="mt-1 font-mono">{result.groupNumber}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Coverage Start
                  </p>
                  <p className="mt-1">{result.coverageStartDate}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Coverage End
                  </p>
                  <p className="mt-1">{result.coverageEndDate}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Benefits Table */}
      {result?.benefits && result.benefits.length > 0 && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="text-sm font-semibold">Benefits</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>Service</TableHead>
                  <TableHead>Coverage Level</TableHead>
                  <TableHead>In-Network</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead className="text-right">Percent</TableHead>
                  <TableHead>Period</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.benefits.map((b, i) => (
                  <TableRow key={i}>
                    <TableCell>{b.benefitType}</TableCell>
                    <TableCell>{b.serviceType}</TableCell>
                    <TableCell>{b.coverageLevel}</TableCell>
                    <TableCell>
                      {b.inNetwork == null ? (
                        "-"
                      ) : (
                        <Badge variant={b.inNetwork ? "default" : "secondary"}>
                          {b.inNetwork ? "Yes" : "No"}
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right font-mono">
                      {b.amount != null ? `$${b.amount.toFixed(2)}` : "-"}
                    </TableCell>
                    <TableCell className="text-right font-mono">
                      {b.percent != null ? `${b.percent}%` : "-"}
                    </TableCell>
                    <TableCell>{b.timePeriod || "-"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {/* Error Alert */}
      {result?.errorMessage && (
        <Alert variant="destructive" className="mt-6">
          <AlertDescription>{result.errorMessage}</AlertDescription>
        </Alert>
      )}
    </div>
  );
}
