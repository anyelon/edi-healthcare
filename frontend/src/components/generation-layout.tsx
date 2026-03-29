"use client";

import { X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EdiPreview } from "@/components/edi-preview";

interface GenerationLayoutProps {
  title: string;
  description: string;
  badgeLabel: string;
  selectedCount: number;
  totalCount: number;
  isLoading: boolean;
  onGenerateAll: () => void;
  onGenerateSelected: () => void;
  preview: string | null;
  previewFilename: string;
  onClosePreview: () => void;
  children: React.ReactNode;
}

export function GenerationLayout({
  title,
  description,
  badgeLabel,
  selectedCount,
  totalCount,
  isLoading,
  onGenerateAll,
  onGenerateSelected,
  preview,
  previewFilename,
  onClosePreview,
  children,
}: GenerationLayoutProps) {
  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
            <Badge variant="outline">{badgeLabel}</Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={onGenerateAll}
            disabled={isLoading || totalCount === 0}
          >
            {isLoading ? "Generating..." : "Generate All"}
          </Button>
          <Button
            onClick={onGenerateSelected}
            disabled={isLoading || selectedCount === 0}
          >
            {isLoading
              ? "Generating..."
              : `Generate Selected (${selectedCount})`}
          </Button>
        </div>
      </div>

      <div className="flex gap-6">
        <div className={preview ? "flex-1 min-w-0" : "w-full"}>
          {children}
        </div>

        {preview && (
          <div className="w-[400px] shrink-0 space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold">EDI Preview</h2>
              <Button variant="ghost" size="icon-xs" onClick={onClosePreview}>
                <X className="h-4 w-4" />
              </Button>
            </div>
            <EdiPreview content={preview} filename={previewFilename} />
          </div>
        )}
      </div>
    </div>
  );
}
