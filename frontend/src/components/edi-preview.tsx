"use client";

import { Download } from "lucide-react";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  ScrollArea,
} from "@/design-system";
import { downloadBlob } from "@/lib/api-client";

interface EdiPreviewProps {
  content: string;
  filename: string;
}

export function EdiPreview({ content, filename }: EdiPreviewProps) {
  function handleDownload() {
    downloadBlob(new Blob([content], { type: "text/plain" }), filename);
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0 pb-3">
        <CardTitle className="text-sm font-semibold">EDI Preview</CardTitle>
        <Button variant="outline" size="sm" onClick={handleDownload}>
          <Download className="mr-1 h-3 w-3" />
          Download {filename}
        </Button>
      </CardHeader>
      <CardContent>
        <ScrollArea className="h-60 rounded-md border bg-muted/50 p-4">
          <pre className="font-mono text-xs whitespace-pre-wrap">{content}</pre>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
