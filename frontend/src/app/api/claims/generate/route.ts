const CLAIMS_API = process.env.CLAIMS_API_URL || "http://localhost:8080";

export async function POST(request: Request) {
  const body = await request.json();
  const response = await fetch(`${CLAIMS_API}/api/claims/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await response.arrayBuffer();
  return new Response(data, {
    status: response.status,
    headers: {
      "Content-Type": response.headers.get("Content-Type") || "text/plain",
      "Content-Disposition":
        response.headers.get("Content-Disposition") || "",
    },
  });
}
