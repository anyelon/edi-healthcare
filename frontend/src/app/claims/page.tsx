"use client";

import { useState } from "react";
import { generateClaim, downloadBlob } from "@/lib/api-client";

export default function ClaimsPage() {
  const [encounterIds, setEncounterIds] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setPreview(null);

    const ids = encounterIds
      .split(/[,\n]/)
      .map((id) => id.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      setError("Please enter at least one encounter ID.");
      setLoading(false);
      return;
    }

    try {
      const blob = await generateClaim(ids);
      const text = await blob.text();
      setPreview(text);
      downloadBlob(new Blob([text], { type: "text/plain" }), "837_claim.edi");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to generate claim");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-2">
        Claims Generation (837)
      </h1>
      <p className="text-gray-600 mb-8">
        Generate EDI 837P professional claim files from patient encounters.
      </p>

      <form onSubmit={handleSubmit} className="max-w-xl">
        <label
          htmlFor="encounterIds"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Encounter IDs
        </label>
        <textarea
          id="encounterIds"
          value={encounterIds}
          onChange={(e) => setEncounterIds(e.target.value)}
          placeholder="Enter encounter IDs, separated by commas or newlines"
          rows={4}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <button
          type="submit"
          disabled={loading}
          className="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "Generating..." : "Generate 837 Claim"}
        </button>
      </form>

      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

      {preview && (
        <div className="mt-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-2">
            EDI 837 Preview
          </h2>
          <pre className="rounded-md bg-gray-50 border border-gray-200 p-4 text-xs font-mono overflow-x-auto whitespace-pre-wrap max-h-96">
            {preview}
          </pre>
        </div>
      )}
    </div>
  );
}
