const RESPONSE_API = process.env.RESPONSE_API_URL || "http://localhost:8082";

export async function POST(request: Request) {
  const formData = await request.formData();
  const response = await fetch(
    `${RESPONSE_API}/api/insurance/acknowledgment`,
    {
      method: "POST",
      body: formData,
    }
  );
  const data = await response.json();
  return Response.json(data, { status: response.status });
}
