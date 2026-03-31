const PRIOR_AUTH_API =
  process.env.PRIOR_AUTH_API_URL || "http://localhost:8083";

export async function POST(request: Request) {
  const body = await request.json();
  const response = await fetch(
    `${PRIOR_AUTH_API}/api/prior-auth/generate`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );

  const data = await response.arrayBuffer();
  return new Response(data, {
    status: response.status,
    headers: {
      "Content-Type":
        response.headers.get("Content-Type") || "text/plain",
      "Content-Disposition":
        response.headers.get("Content-Disposition") || "",
    },
  });
}
