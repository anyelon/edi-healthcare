"use client";

import { useState } from "react";
import Link from "next/link";
import { seedDatabase } from "@/lib/api-client";
import type { SeedResult } from "@/types";

const workflows = [
  {
    href: "/claims",
    title: "Claims Generation (837)",
    description:
      "Generate EDI 837P professional claim files from patient encounters.",
  },
  {
    href: "/eligibility-request",
    title: "Eligibility Request (270)",
    description:
      "Generate EDI 270 eligibility inquiry files for patient insurance verification.",
  },
  {
    href: "/eligibility-response",
    title: "Eligibility Response (271)",
    description:
      "Upload and parse EDI 271 eligibility response files to view coverage details.",
  },
];

export default function Dashboard() {
  const [seeding, setSeeding] = useState(false);
  const [seedResult, setSeedResult] = useState<SeedResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSeed() {
    setSeeding(true);
    setError(null);
    try {
      const result = await seedDatabase();
      setSeedResult(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Seed failed");
    } finally {
      setSeeding(false);
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">
          EDI healthcare transaction management system.
        </p>
      </div>

      <div className="mb-8 rounded-lg border border-gray-200 bg-gray-50 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">
          Database Setup
        </h2>
        <p className="text-sm text-gray-600 mb-4">
          Seed the database with sample patients, encounters, payers, and
          insurance records for testing.
        </p>
        <button
          onClick={handleSeed}
          disabled={seeding}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {seeding ? "Seeding..." : "Seed Database"}
        </button>
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        {seedResult && (
          <div className="mt-4 rounded-md bg-white p-4 text-sm">
            <p className="font-medium text-green-700 mb-2">
              Database seeded successfully!
            </p>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-gray-700">
              <dt className="font-medium">Patients:</dt>
              <dd>{seedResult.patientIds.length}</dd>
              <dt className="font-medium">Encounters:</dt>
              <dd>{seedResult.encounterIds.length}</dd>
              <dt className="font-medium">Patient IDs:</dt>
              <dd className="font-mono text-xs break-all">
                {seedResult.patientIds.join(", ")}
              </dd>
              <dt className="font-medium">Encounter IDs:</dt>
              <dd className="font-mono text-xs break-all">
                {seedResult.encounterIds.join(", ")}
              </dd>
            </dl>
          </div>
        )}
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        {workflows.map((wf) => (
          <Link
            key={wf.href}
            href={wf.href}
            className="block rounded-lg border border-gray-200 p-6 hover:border-blue-300 hover:shadow-md transition-all"
          >
            <h3 className="text-lg font-semibold text-gray-900">{wf.title}</h3>
            <p className="mt-2 text-sm text-gray-600">{wf.description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
