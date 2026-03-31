import type { SeedResult, EligibilityResponse, EncounterResponse, PatientResponse, PriorAuthResponse } from "@/types";

export async function seedDatabase(): Promise<SeedResult> {
  const res = await fetch("/api/dev/seed", { method: "POST" });
  if (!res.ok) throw new Error(`Seed failed: ${res.statusText}`);
  return res.json();
}

export async function generateClaim(encounterIds: string[]): Promise<Blob> {
  const res = await fetch("/api/claims/generate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ encounterIds }),
  });
  if (!res.ok) throw new Error(`Claim generation failed: ${res.statusText}`);
  return res.blob();
}

export async function generateEligibilityRequest(
  patientIds: string[]
): Promise<Blob> {
  const res = await fetch("/api/insurance/eligibility-request", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ patientIds }),
  });
  if (!res.ok) throw new Error(`Eligibility request failed: ${res.statusText}`);
  return res.blob();
}

export async function parseEligibilityResponse(
  file: File
): Promise<EligibilityResponse> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch("/api/insurance/eligibility-response", {
    method: "POST",
    body: formData,
  });
  if (!res.ok) throw new Error(`Response parsing failed: ${res.statusText}`);
  return res.json();
}

export async function fetchEncounters(): Promise<EncounterResponse[]> {
  const res = await fetch("/api/encounters");
  if (!res.ok) throw new Error(`Failed to fetch encounters: ${res.statusText}`);
  return res.json();
}

export async function fetchPatients(): Promise<PatientResponse[]> {
  const res = await fetch("/api/patients");
  if (!res.ok) throw new Error(`Failed to fetch patients: ${res.statusText}`);
  return res.json();
}

export async function generatePriorAuth(
  encounterIds: string[]
): Promise<Blob> {
  const res = await fetch("/api/prior-auth/generate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ encounterIds }),
  });
  if (!res.ok)
    throw new Error(`Prior auth generation failed: ${res.statusText}`);
  return res.blob();
}

export async function parsePriorAuthResponse(
  file: File
): Promise<PriorAuthResponse> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch("/api/prior-auth/response", {
    method: "POST",
    body: formData,
  });
  if (!res.ok)
    throw new Error(`Prior auth response parsing failed: ${res.statusText}`);
  return res.json();
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
