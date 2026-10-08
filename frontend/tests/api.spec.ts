import { afterEach, test } from "node:test";
import assert from "node:assert/strict";
import { apiRequest, ApiRequestError } from "../src/lib/api.ts";

const originalFetch = globalThis.fetch;
const originalOrigin = process.env.NEXT_PUBLIC_API_ORIGIN;

afterEach(() => {
  globalThis.fetch = originalFetch;
  if (originalOrigin === undefined) delete process.env.NEXT_PUBLIC_API_ORIGIN;
  else process.env.NEXT_PUBLIC_API_ORIGIN = originalOrigin;
});

function origin() {
  process.env.NEXT_PUBLIC_API_ORIGIN = "https://backend.example";
}

test("uses existing origin and preserves lossless DTO values", async () => {
  origin();
  const dto = { id: "9223372036854775807",
    money: { amount: "9999999999999.99", currency: "VND" },
    occurredAt: "2026-10-05T09:00:00Z" };
  globalThis.fetch = async (input, init) => {
    assert.equal(input, "https://backend.example/api/v1/example");
    assert.equal(new Headers(init?.headers).get("Accept"), "application/json");
    assert.equal(init?.credentials, undefined);
    assert.equal(init?.redirect, "error");
    return Response.json(dto);
  };
  assert.deepEqual(await apiRequest("/example"), dto);
});

test("structured failure preserves approved error and makes only one mutation attempt", async () => {
  origin();
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return Response.json({ code: "INVALID_REQUEST", message: "Invalid request.",
      fieldErrors: { name: "Invalid value." } }, { status: 400 });
  };
  await assert.rejects(apiRequest("/example", { method: "POST" }), (error: unknown) => {
    assert.ok(error instanceof ApiRequestError);
    assert.equal(error.status, 400);
    assert.equal(error.code, "INVALID_REQUEST");
    assert.deepEqual(error.fieldErrors, { name: "Invalid value." });
    return true;
  });
  assert.equal(calls, 1);
});

test("raw/malformed server errors do not enter the displayed error", async () => {
  origin();
  for (const response of [
    new Response("PRIVATE_SECRET internal stack", { status: 500 }),
    new Response("{PRIVATE_SECRET", { status: 500,
      headers: { "content-type": "application/json" } }),
    Response.json({ message: "PRIVATE_SECRET" }, { status: 500 }),
  ]) {
    globalThis.fetch = async () => response;
    await assert.rejects(apiRequest("/example"), (error: unknown) => {
      assert.ok(error instanceof ApiRequestError);
      assert.equal(error.message, "Request failed.");
      return true;
    });
  }
});

test("handles 204 and rejects invalid successful JSON responses", async () => {
  origin();
  globalThis.fetch = async () => new Response(null, { status: 204 });
  assert.equal(await apiRequest("/example", { method: "DELETE" }), undefined);
  globalThis.fetch = async () => new Response("<html>private</html>");
  await assert.rejects(apiRequest("/example"), { code: "INVALID_RESPONSE" });
});

test("rejects missing or credential-bearing configuration and escaped paths before fetch", async () => {
  let calls = 0;
  globalThis.fetch = async () => { calls++; return Response.json({}); };
  delete process.env.NEXT_PUBLIC_API_ORIGIN;
  await assert.rejects(apiRequest("/example"));
  process.env.NEXT_PUBLIC_API_ORIGIN = "https://user:PRIVATE_SECRET@backend.example";
  await assert.rejects(apiRequest("/example"), (e: unknown) => {
    assert.ok(e instanceof Error);
    assert.ok(!e.message.includes("PRIVATE_SECRET"));
    return true;
  });
  origin();
  for (const path of ["https://other.example", "//other.example", "/../../outside", "/%2e%2e/%2e%2e/outside"]) {
    await assert.rejects(apiRequest(path));
  }
  assert.equal(calls, 0);
});

test("network failures are not retried", async () => {
  origin();
  let calls = 0;
  globalThis.fetch = async () => { calls++; throw new TypeError("Failed to fetch"); };
  await assert.rejects(apiRequest("/example", { method: "POST" }), TypeError);
  assert.equal(calls, 1);
});
