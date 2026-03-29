"use client";

import { useState, useCallback } from "react";
import { parseEligibilityResponse } from "@/lib/api-client";
import type { EligibilityResponse } from "@/types";

export default function EligibilityResponsePage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<EligibilityResponse | null>(null);
  const [dragActive, setDragActive] = useState(false);

  const handleFile = useCallback(async (file: File) => {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const response = await parseEligibilityResponse(file);
      setResult(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to parse response");
    } finally {
      setLoading(false);
    }
  }, []);

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragActive(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  }

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-2">
        Eligibility Response (271)
      </h1>
      <p className="text-gray-600 mb-8">
        Upload an EDI 271 eligibility response file to view parsed coverage
        details.
      </p>

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={handleDrop}
        className={`max-w-xl rounded-lg border-2 border-dashed p-8 text-center transition-colors ${
          dragActive
            ? "border-blue-500 bg-blue-50"
            : "border-gray-300 bg-gray-50"
        }`}
      >
        <p className="text-sm text-gray-600 mb-4">
          Drag and drop an EDI 271 file here, or click to select
        </p>
        <input
          type="file"
          accept=".edi,.txt"
          onChange={handleChange}
          className="block mx-auto text-sm text-gray-500 file:mr-4 file:rounded-md file:border-0 file:bg-blue-600 file:px-4 file:py-2 file:text-sm file:font-medium file:text-white hover:file:bg-blue-700 file:cursor-pointer"
        />
      </div>

      {loading && (
        <p className="mt-4 text-sm text-gray-600">Parsing response...</p>
      )}
      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

      {result && (
        <div className="mt-8 space-y-6">
          <div className="rounded-lg border border-gray-200 bg-white p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Response Details
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
              <div>
                <dt className="font-medium text-gray-500">Status</dt>
                <dd
                  className={`mt-1 font-semibold ${result.eligibilityStatus?.toLowerCase().includes("active") ? "text-green-700" : "text-red-700"}`}
                >
                  {result.eligibilityStatus || result.status}
                </dd>
              </div>
              <div>
                <dt className="font-medium text-gray-500">Subscriber</dt>
                <dd className="mt-1">
                  {result.subscriberFirstName} {result.subscriberLastName}
                </dd>
              </div>
              <div>
                <dt className="font-medium text-gray-500">Member ID</dt>
                <dd className="mt-1 font-mono">{result.memberId}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-500">Payer</dt>
                <dd className="mt-1">{result.payerName}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-500">Group Number</dt>
                <dd className="mt-1 font-mono">{result.groupNumber}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-500">Coverage Period</dt>
                <dd className="mt-1">
                  {result.coverageStartDate} - {result.coverageEndDate}
                </dd>
              </div>
            </div>
          </div>

          {result.benefits && result.benefits.length > 0 && (
            <div className="rounded-lg border border-gray-200 bg-white p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Benefits
              </h2>
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="text-left py-2 pr-4 font-medium text-gray-500">
                        Type
                      </th>
                      <th className="text-left py-2 pr-4 font-medium text-gray-500">
                        Service
                      </th>
                      <th className="text-left py-2 pr-4 font-medium text-gray-500">
                        Coverage
                      </th>
                      <th className="text-left py-2 pr-4 font-medium text-gray-500">
                        In-Network
                      </th>
                      <th className="text-right py-2 pr-4 font-medium text-gray-500">
                        Amount
                      </th>
                      <th className="text-right py-2 pr-4 font-medium text-gray-500">
                        Percent
                      </th>
                      <th className="text-left py-2 font-medium text-gray-500">
                        Period
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.benefits.map((b, i) => (
                      <tr key={i} className="border-b border-gray-100">
                        <td className="py-2 pr-4">{b.benefitType}</td>
                        <td className="py-2 pr-4">{b.serviceType}</td>
                        <td className="py-2 pr-4">{b.coverageLevel}</td>
                        <td className="py-2 pr-4">
                          {b.inNetwork == null ? "-" : b.inNetwork ? "Yes" : "No"}
                        </td>
                        <td className="py-2 pr-4 text-right font-mono">
                          {b.amount != null ? `$${b.amount.toFixed(2)}` : "-"}
                        </td>
                        <td className="py-2 pr-4 text-right font-mono">
                          {b.percent != null ? `${b.percent}%` : "-"}
                        </td>
                        <td className="py-2">{b.timePeriod || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {result.errorMessage && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="text-sm text-red-700">{result.errorMessage}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
