"use client";

import { useState, useCallback } from "react";
import { toast } from "sonner";
import {
  Badge,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/design-system";
import { Dropzone } from "@/components/dropzone";
import { parseAcknowledgment } from "@/lib/api-client";
import type { EDI999Acknowledgment } from "@/types";

function statusVariant(status: string) {
  switch (status) {
    case "ACCEPTED":
      return "default";
    case "ACCEPTED_WITH_ERRORS":
      return "outline";
    case "REJECTED":
      return "destructive";
    case "PARTIALLY_ACCEPTED":
      return "outline";
    default:
      return "secondary";
  }
}

function statusLabel(status: string) {
  return status.replace(/_/g, " ");
}

export default function AcknowledgmentPage() {
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<EDI999Acknowledgment[]>([]);

  const handleFile = useCallback(async (file: File) => {
    setLoading(true);
    setResults([]);
    try {
      const data = await parseAcknowledgment(file);
      setResults(data);
      toast.success(
        "EDI 999 parsed: " + data.length + " transaction(s) found."
      );
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to parse acknowledgment"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Acknowledgment</h1>
        <Badge variant="outline">EDI 999</Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">
              Upload EDI 999 File
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Dropzone onFileSelect={handleFile} />
            {loading && (
              <p className="mt-3 text-sm text-muted-foreground">
                Parsing acknowledgment...
              </p>
            )}
          </CardContent>
        </Card>

        {results.length > 0 && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-semibold">
                  Group Summary
                </CardTitle>
                <Badge variant={statusVariant(results[0].groupStatus)}>
                  {statusLabel(results[0].groupStatus)}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-4 text-sm">
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Included
                  </p>
                  <p className="mt-1 text-lg font-semibold">
                    {results[0].transactionSetsIncluded}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Received
                  </p>
                  <p className="mt-1 text-lg font-semibold">
                    {results[0].transactionSetsReceived}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase text-muted-foreground">
                    Accepted
                  </p>
                  <p className="mt-1 text-lg font-semibold">
                    {results[0].transactionSetsAccepted}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {results.map((ack, i) => (
        <Card key={i} className="mt-6">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-semibold">
                Transaction {ack.acknowledgedTransactionSetId} — Control #
                {ack.acknowledgedTransactionControlNumber}
              </CardTitle>
              <Badge variant={statusVariant(ack.transactionStatus)}>
                {statusLabel(ack.transactionStatus)}
              </Badge>
            </div>
          </CardHeader>
          {ack.errors.length > 0 && (
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Segment</TableHead>
                    <TableHead>Position</TableHead>
                    <TableHead>Segment Error</TableHead>
                    <TableHead>Element Position</TableHead>
                    <TableHead>Element Error</TableHead>
                    <TableHead>Description</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {ack.errors.map((err, j) => (
                    <TableRow key={j}>
                      <TableCell className="font-mono">
                        {err.segmentId}
                      </TableCell>
                      <TableCell>{err.segmentPosition}</TableCell>
                      <TableCell className="font-mono">
                        {err.segmentErrorCode}
                      </TableCell>
                      <TableCell>{err.elementPosition || "-"}</TableCell>
                      <TableCell className="font-mono">
                        {err.elementErrorCode || "-"}
                      </TableCell>
                      <TableCell>{err.errorDescription}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          )}
        </Card>
      ))}
    </div>
  );
}
