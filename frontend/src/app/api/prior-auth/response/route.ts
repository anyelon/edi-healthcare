const PRIOR_AUTH_API =
  process.env.PRIOR_AUTH_API_URL || "http://localhost:8083";

export async function POST(request: Request) {
  const formData = await request.formData();
  const response = await fetch(
    `${PRIOR_AUTH_API}/api/prior-auth/response`,
    {
      method: "POST",
      body: formData,
    }
  );
  const data = await response.json();
  return Response.json(data, { status: response.status });
}
