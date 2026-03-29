const CLAIMS_API = process.env.CLAIMS_API_URL || "http://localhost:8080";

export async function POST() {
  const response = await fetch(`${CLAIMS_API}/api/dev/seed`, {
    method: "POST",
  });
  const data = await response.json();
  return Response.json(data, { status: response.status });
}
