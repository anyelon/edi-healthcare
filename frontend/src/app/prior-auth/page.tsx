"use client";

import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { GenerationLayout } from "@/components/generation-layout";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { Dropzone } from "@/components/dropzone";
import {
  fetchEncounters,
  generatePriorAuth,
  parsePriorAuthResponse,
} from "@/lib/api-client";
import type { EncounterResponse, PriorAuthResponse } from "@/types";

const columns: ColumnDef<EncounterResponse>[] = [
  { header: "Patient", accessor: "patientName" },
  { header: "Date of Service", accessor: "dateOfService" },
  { header: "Provider", accessor: "providerName" },
  {
    header: "Needs Auth",
    accessor: "procedures",
    cell: (row) => {
      const authProcs = (row.procedures ?? []).filter((p) => p.needsAuth);
      return (
        <div className="flex flex-wrap gap-1">
          {authProcs.length > 0 ? (
            authProcs.map((p) => (
              <Badge
                key={p.procedureCode}
                variant="secondary"
                className="bg-purple-500/10 text-purple-600 dark:text-purple-400"
              >
                {p.procedureCode}
              </Badge>
            ))
          ) : (
            <span className="text-xs text-muted-foreground">—</span>
          )}
        </div>
      );
    },
  },
  {
    header: "Auth Status",
    accessor: "authorizationNumber",
    cell: (row) => (
      <Badge variant={row.authorizationNumber ? "default" : "secondary"}>
        {row.authorizationNumber ? "Authorized" : "Pending"}
      </Badge>
    ),
  },
];

export default function PriorAuthPage() {
  const [tab, setTab] = useState<"generate" | "response">("generate");

  // Generate tab state
  const [encounters, setEncounters] = useState<EncounterResponse[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Response tab state
  const [responseLoading, setResponseLoading] = useState(false);
  const [result, setResult] = useState<PriorAuthResponse | null>(null);

  function loadEncounters() {
    setError(null);
    setFetching(true);
    fetchEncounters()
      .then(setEncounters)
      .catch((err) => {
        const message =
          err instanceof Error ? err.message : "Failed to fetch encounters";
        setError(message);
        toast.error(message);
      })
      .finally(() => setFetching(false));
  }

  useEffect(() => {
    loadEncounters();
  }, []);

  async function handleGenerate(ids: string[]) {
    setLoading(true);
    try {
      const blob = await generatePriorAuth(ids);
      const text = await blob.text();
      setPreview(text);
      toast.success("EDI 278 prior auth request generated successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to generate prior auth"
      );
    } finally {
      setLoading(false);
    }
  }

  const handleFile = useCallback(async (file: File) => {
    setResponseLoading(true);
    setResult(null);
    try {
      const response = await parsePriorAuthResponse(file);
      setResult(response);
      toast.success("EDI 278 response parsed successfully.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Failed to parse prior auth response"
      );
    } finally {
      setResponseLoading(false);
    }
  }, []);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold tracking-tight">
            Prior Authorization
          </h1>
          <Badge variant="outline">EDI 278</Badge>
        </div>
        <div className="flex gap-2">
          <Button
            variant={tab === "generate" ? "default" : "outline"}
            onClick={() => setTab("generate")}
          >
            Generate Request
          </Button>
          <Button
            variant={tab === "response" ? "default" : "outline"}
            onClick={() => setTab("response")}
          >
            Parse Response
          </Button>
        </div>
      </div>

      {tab === "generate" && (
        <GenerationLayout
          title=""
          description=""
          badgeLabel=""
          selectedCount={selectedIds.size}
          totalCount={encounters.length}
          isLoading={loading}
          onGenerateAll={() => handleGenerate(encounters.map((e) => e.id))}
          onGenerateSelected={() => handleGenerate([...selectedIds])}
          preview={preview}
          previewFilename="278_prior_auth.edi"
          onClosePreview={() => setPreview(null)}
        >
          {fetching ? (
            <div className="flex h-64 items-center justify-center text-muted-foreground">
              Loading encounters...
            </div>
          ) : error ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
              <p>{error}</p>
              <Button variant="outline" onClick={loadEncounters}>
                Retry
              </Button>
            </div>
          ) : (
            <DataTable
              columns={columns}
              data={encounters}
              selectedIds={selectedIds}
              onSelectionChange={setSelectedIds}
              getId={(e) => e.id}
              emptyMessage="No encounters found. Try seeding the database from the sidebar."
            />
          )}
        </GenerationLayout>
      )}

      {tab === "response" && (
        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-semibold">
                Upload EDI 278 Response File
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Dropzone onFileSelect={handleFile} />
              {responseLoading && (
                <p className="mt-3 text-sm text-muted-foreground">
                  Parsing response...
                </p>
              )}
            </CardContent>
          </Card>

          {result && (
            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-semibold">
                  Response Details
                </CardTitle>
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
                      Payer ID
                    </p>
                    <p className="mt-1 font-mono">{result.payerId}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {result?.decisions && result.decisions.length > 0 && (
            <Card className="md:col-span-2">
              <CardHeader>
                <CardTitle className="text-sm font-semibold">
                  Authorization Decisions
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Encounter ID</TableHead>
                      <TableHead>Decision</TableHead>
                      <TableHead>Authorization Number</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {result.decisions.map((d) => (
                      <TableRow key={d.encounterId}>
                        <TableCell className="font-mono text-xs">
                          {d.encounterId}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              d.action === "CERTIFIED" ? "default" : "destructive"
                            }
                          >
                            {d.action}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-mono">
                          {d.authorizationNumber || "—"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
