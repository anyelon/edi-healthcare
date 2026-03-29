const REQUEST_API = process.env.REQUEST_API_URL || "http://localhost:8081";

export async function POST(request: Request) {
  const body = await request.json();
  const response = await fetch(
    `${REQUEST_API}/api/insurance/eligibility-request`,
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
      "Content-Type": response.headers.get("Content-Type") || "text/plain",
      "Content-Disposition":
        response.headers.get("Content-Disposition") || "",
    },
  });
}
