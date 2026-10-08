export class ApiRequestError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors?: Readonly<Record<string, string>>;

  constructor(status: number, code: string, message: string,
    fieldErrors?: Readonly<Record<string, string>>) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

function apiUrl(path: string): string {
  const origin = process.env.NEXT_PUBLIC_API_ORIGIN;
  if (!origin) throw new Error("NEXT_PUBLIC_API_ORIGIN is required.");

  let url: URL;
  try {
    url = new URL(origin);
  } catch {
    throw new Error("NEXT_PUBLIC_API_ORIGIN must be an HTTP(S) origin.");
  }
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username || url.password || url.pathname !== "/" || url.search || url.hash
  ) {
    throw new Error("NEXT_PUBLIC_API_ORIGIN must be an HTTP(S) origin.");
  }

  // Relative API paths only: never let a caller override the trusted API origin.
  if (!path.startsWith("/") || path.startsWith("//") || path.includes("\\") ||
      path.includes("#")) {
    throw new Error("Expected a relative API path.");
  }
  const target = new URL(`/api/v1${path}`, url.origin);
  if (target.origin !== url.origin || !target.pathname.startsWith("/api/v1/")) {
    throw new Error("Expected a relative API path.");
  }
  return target.href;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T | undefined> {
  const url = apiUrl(path);
  const headers = new Headers(init.headers);
  if (!headers.has("Accept")) headers.set("Accept", "application/json");

  // One fetch call. No retries, credential setup, refresh, redirects or logging.
  const response = await fetch(url, { ...init, headers, redirect: "error" });
  if (response.status === 204) return undefined;

  const isJson = response.headers.get("content-type")?.split(";")[0].trim() ===
    "application/json";
  let body: unknown;
  if (isJson) {
    try {
      body = await response.json();
    } catch {
      // Never turn a raw HTML/error body into a user-facing message.
    }
  }

  if (!response.ok) {
    if (isObject(body) && typeof body.code === "string" &&
        typeof body.message === "string" &&
        (body.fieldErrors === undefined ||
          (isObject(body.fieldErrors) &&
            Object.values(body.fieldErrors).every((value) => typeof value === "string")))) {
      throw new ApiRequestError(response.status, body.code, body.message,
        body.fieldErrors as Record<string, string> | undefined);
    }
    throw new ApiRequestError(response.status, "HTTP_ERROR", "Request failed.");
  }
  if (!isJson || body === undefined) {
    throw new ApiRequestError(response.status, "INVALID_RESPONSE",
      "The server returned an invalid response.");
  }
  // T is the caller's approved DTO contract, not runtime schema validation.
  // Strings (IDs, amounts, timestamps) are preserved without numeric/date coercion.
  return body as T;
}
