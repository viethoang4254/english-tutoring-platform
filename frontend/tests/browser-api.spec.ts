import { afterEach, test } from "node:test";
import assert from "node:assert/strict";
import { authMutation, authorized, signIn, safeMessage, loginMessage, loginValidation, validPassword } from "../src/features/auth/browser-api.ts";
import { ApiRequestError } from "../src/lib/api.ts";
const original = globalThis.fetch;
const originalOrigin = process.env.NEXT_PUBLIC_API_ORIGIN;
afterEach(() => { globalThis.fetch = original; if (originalOrigin === undefined) delete process.env.NEXT_PUBLIC_API_ORIGIN; else process.env.NEXT_PUBLIC_API_ORIGIN = originalOrigin; });
test("cookie mutations bootstrap CSRF and send no Bearer or refresh body", async () => {
  process.env.NEXT_PUBLIC_API_ORIGIN = "http://localhost:8080";
  let calls = 0;
  globalThis.fetch = async (url, init) => {
    calls++; assert.equal(init?.credentials, "include"); assert.equal(init?.cache, "no-store");
    assert.equal(new Headers(init?.headers).has("Authorization"), false);
    if (String(url).endsWith("/csrf")) return Response.json({ token: "csrf" });
    assert.equal(new Headers(init?.headers).get("X-XSRF-TOKEN"), "csrf");
    assert.equal(init?.body, undefined); return new Response(null, { status: 204 });
  };
  await authMutation("/auth/logout"); assert.equal(calls, 2);
});
test("Bearer mutations omit cookies and never retry network failures", async () => {
  process.env.NEXT_PUBLIC_API_ORIGIN = "http://localhost:8080"; let calls = 0;
  globalThis.fetch = async (_, init) => { calls++; assert.equal(init?.credentials, "omit"); assert.equal(new Headers(init?.headers).get("Authorization"), "Bearer test"); throw new TypeError("network"); };
  await assert.rejects(authorized("test", "/teacher/applications/1/resubmit", { introduction: "x" })); assert.equal(calls, 1);
});
test("password boundaries mirror code-point UTF-8 and malformed UTF-16 rules", () => {
  for (const password of ["12345678", " ".repeat(8), "a".repeat(64), "é".repeat(36), "😀".repeat(18)]) assert.equal(validPassword(password), true);
  for (const password of ["short", "a".repeat(65), "é".repeat(37), "😀".repeat(19), "a".repeat(8) + "\ud800"]) assert.equal(validPassword(password), false);
});
test("safe errors do not render arbitrary server messages or private values", () => {
  for (const status of [400, 401, 403, 404, 409, 500, 503]) assert.ok(!safeMessage(new ApiRequestError(status, "UNKNOWN", "PRIVATE")).includes("PRIVATE"));
  assert.match(safeMessage(new ApiRequestError(503, "VERIFICATION_DELIVERY_UNAVAILABLE", "PRIVATE")), /Tài khoản của bạn vẫn được giữ lại/);
});

test("Admin approval uses bodyless POST with Bearer and no cookies", async () => {
  process.env.NEXT_PUBLIC_API_ORIGIN = "http://localhost:8080";
  globalThis.fetch = async (_, init) => {
    assert.equal(init?.method, "POST"); assert.equal(init?.body, undefined);
    assert.equal(init?.credentials, "omit"); assert.equal(new Headers(init?.headers).get("Authorization"), "Bearer test");
    assert.equal(new Headers(init?.headers).has("Content-Type"), false);
    return new Response(null, { status: 204 });
  };
  await authorized("test", "/admin/teacher-applications/1/approve", undefined, "POST");
});


test("explicit recovery retains CSRF cookie transport and never retries", async () => {
  process.env.NEXT_PUBLIC_API_ORIGIN = "http://localhost:8080";
  const paths: string[] = [];
  globalThis.fetch = async (url, init) => {
    const path = new URL(String(url)).pathname; paths.push(path);
    if (path.endsWith("/csrf")) return Response.json({ token: "csrf" });
    if (path.endsWith("/refresh")) {
      assert.equal(init?.credentials, "include"); assert.equal(init?.body, undefined);
      assert.equal(new Headers(init?.headers).get("X-XSRF-TOKEN"), "csrf");
      return Response.json({ accessToken: "test", tokenType: "Bearer" });
    }
    assert.equal(new Headers(init?.headers).get("Authorization"), "Bearer test");
    return Response.json({ id: "1", role: "STUDENT", emailVerified: true });
  };
  assert.equal((await signIn()).account.id, "1");
  assert.deepEqual(paths, ["/api/v1/auth/csrf", "/api/v1/auth/refresh", "/api/v1/users/me"]);
  let attempts = 0;
  globalThis.fetch = async url => {
    if (String(url).endsWith("/csrf")) return Response.json({ token: "csrf" });
    attempts++; throw new TypeError("network");
  };
  await assert.rejects(signIn()); assert.equal(attempts, 1);
});

test("login validation and errors never call invalid credentials an expired session", () => {
  assert.equal(loginValidation("", ""), "Vui lòng nhập email.");
  assert.equal(loginValidation("broken", "password"), "Email không hợp lệ.");
  assert.equal(loginValidation("a@example.invalid", ""), "Vui lòng nhập mật khẩu.");
  assert.equal(loginValidation(" a@example.invalid ", "password"), "");
  assert.equal(loginMessage(new ApiRequestError(401,"AUTHENTICATION_REQUIRED","private")), "Email hoặc mật khẩu không đúng.");
  assert.equal(loginMessage(new TypeError("private")), "Không thể kết nối đến máy chủ. Vui lòng thử lại.");
  assert.equal(loginMessage(new ApiRequestError(503,"UNAVAILABLE","private")), "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.");
});
test("remember preference travels only in JSON, passwords never in URLs", async () => {
  process.env.NEXT_PUBLIC_API_ORIGIN="http://localhost:8080";
  for(const rememberMe of [false,true]) {
    globalThis.fetch=async(url,init)=>{
      assert.equal(new URL(String(url)).search, "");
      if(String(url).endsWith("/csrf"))return Response.json({token:"fixture"});
      if(String(url).endsWith("/login")) {
        assert.deepEqual(JSON.parse(String(init?.body)), {email:"a@example.invalid",password:"fixture-password",rememberMe});
        return Response.json({accessToken:"fixture",tokenType:"Bearer"});
      }
      return Response.json({id:"1",role:"TEACHER",emailVerified:false});
    };
    assert.equal((await signIn({email:"a@example.invalid",password:"fixture-password",rememberMe})).account.role,"TEACHER");
  }
});

test("remembered restoration is single-flight and stores only a non-secret flag", async () => {
  const {rememberLogin,restoreRememberedSession}=await import("../src/features/auth/remember-session.ts");
  const descriptor=Object.getOwnPropertyDescriptor(globalThis,"localStorage");
  const saved=new Map<string,string>();
  Object.defineProperty(globalThis,"localStorage",{configurable:true,value:{getItem:(key:string)=>saved.get(key)??null,setItem:(key:string,value:string)=>saved.set(key,value),removeItem:(key:string)=>saved.delete(key)}});
  try {
    let calls=0;
    process.env.NEXT_PUBLIC_API_ORIGIN="http://localhost:8080";
    globalThis.fetch=async(url)=>{
      calls++;
      if(String(url).endsWith("/csrf"))return Response.json({token:"csrf"});
      if(String(url).endsWith("/refresh"))return Response.json({accessToken:"private-access",tokenType:"Bearer"});
      return Response.json({id:"1",role:"STUDENT",emailVerified:false});
    };
    assert.equal(await restoreRememberedSession(),null);assert.equal(calls,0);
    rememberLogin(true);
    const first=restoreRememberedSession(),second=restoreRememberedSession();
    assert.equal(first,second);assert.equal((await first)?.account.id,"1");assert.equal(calls,3);
    assert.deepEqual([...saved],[["remember-login","1"]]);
    rememberLogin(false);assert.equal(await restoreRememberedSession(),null);assert.equal(saved.size,0);
    rememberLogin(true);globalThis.fetch=async()=>{throw new TypeError("offline");};
    assert.equal(await restoreRememberedSession(),null);
    assert.equal(saved.size,0); // No retry after an uncertain rotation on the next load.
  } finally {if(descriptor)Object.defineProperty(globalThis,"localStorage",descriptor);else Reflect.deleteProperty(globalThis,"localStorage");}
});
