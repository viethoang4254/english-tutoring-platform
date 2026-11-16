import { expect, test } from "@playwright/test";
import { googleFixture, openGoogle } from "../support/google-calendar";
import { captureReview } from "../support/ui-review";

test("@google-review Student disconnected and connected OAuth return without Remember Me", async ({ page }, info) => {
  const state = await googleFixture(page);let panel = await openGoogle(page);
  await expect(panel.getByText("Chưa kết nối", { exact: true })).toBeVisible();
  await expect(panel).toContainText("Đồng bộ buổi học chưa hoạt động");
  await captureReview(page, info, "student-google-disconnected");
  await panel.getByRole("button", { name: "Kết nối Google Calendar", exact: true }).click();
  await expect(page).toHaveURL(/\/account\?googleCalendar=CONNECTED$/);
  panel = page.getByRole("region", { name: "Kết nối Google Calendar", exact: true });
  await expect(panel.getByText("Đã kết nối", { exact: true })).toBeVisible();
  await expect(panel).toContainText("calendar.fixture@example.test");
  expect(state.connects).toBe(1);expect(state.refreshes).toBe(1);
  expect(await page.evaluate(() => sessionStorage.getItem("google-calendar-return"))).toBeNull();
  await captureReview(page, info, "student-google-connected");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("@google-review approved unverified Teacher connected state and disconnect", async ({ page }, info) => {
  const state = await googleFixture(page, "TEACHER");state.status = "CONNECTED";
  const panel = await openGoogle(page, "TEACHER");
  await expect(panel.getByText("Đã kết nối", { exact: true })).toBeVisible();
  await expect(panel).toContainText("calendar.fixture@example.test");
  await captureReview(page, info, "teacher-google-connected");
  await panel.getByRole("button", { name: "Ngắt kết nối Google Calendar" }).click();
  await expect(panel.getByText("Chưa kết nối", { exact: true })).toBeVisible();expect(state.disconnects).toBe(1);
  await expect(panel.getByText("calendar.fixture@example.test", { exact: false })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("revoked authorization offers explicit reconnect without automatic mutation", async ({ page }) => {
  const state = await googleFixture(page);state.status = "RECONNECT_REQUIRED";
  const panel = await openGoogle(page);await expect(panel.getByText("Cần kết nối lại", { exact: true })).toBeVisible();
  await expect(panel.getByRole("button", { name: "Kết nối lại Google Calendar" })).toBeEnabled();expect(state.connects).toBe(0);
});

for (const code of [401, 403, 503]) test(`Google status ${code} is safe and retries only explicitly`, async ({ page }) => {
  const state = await googleFixture(page);state.http = code;const panel = await openGoogle(page);
  if (code === 401) { await expect(page).toHaveURL(/\/login\?next=/);return; }
  await expect(panel.getByRole("alert")).toBeVisible();await expect(page.getByText("PRIVATE", { exact: true })).toHaveCount(0);
  state.http = 200;await panel.getByRole("button", { name: "Tải lại kết nối" }).click();
  await expect(panel.getByText("Chưa kết nối", { exact: true })).toBeVisible();expect(state.connects).toBe(0);
});

test("disabled integration advertises no synchronization or connect action", async ({ page }) => {
  const state = await googleFixture(page);state.status = "UNAVAILABLE";
  const panel = await openGoogle(page);await expect(panel).toContainText("hiện chưa khả dụng");
  await expect(panel.getByRole("button", { name: "Kết nối Google Calendar", exact: true })).toHaveCount(0);expect(state.connects).toBe(0);
});

test("failed remote revocation leaves local connection disconnected", async ({ page }) => {
  const state = await googleFixture(page);state.status = "CONNECTED";state.remoteRevoked = false;
  const panel = await openGoogle(page);await expect(panel.getByText("Đã kết nối", { exact: true })).toBeVisible();
  await panel.getByRole("button", { name: "Ngắt kết nối Google Calendar" }).click();
  await expect(panel.getByText("Chưa kết nối", { exact: true })).toBeVisible();await expect(panel.getByRole("alert")).toContainText("Đã ngắt kết nối trong ứng dụng");expect(state.disconnects).toBe(1);
});

for (const scenario of ["missing","expired","malformed","refresh-failed","identity-failed","switched","denied","replayed"]) {
  test(`OAuth return enforces initiating session: ${scenario}`, async ({ page, context }) => {
    const state = await googleFixture(page);
    const panel = await openGoogle(page);
    await expect(panel.getByText("Chưa kết nối",{exact:true})).toBeVisible();
    // Hold simulated consent so the test can switch tabs before returning.
    const consent = "https://accounts.google.com/o/oauth2/v2/auth?state=synthetic-intent";
    await page.route(consent, route => route.fulfill({contentType:"text/html",body:"Synthetic consent"}));
    await panel.getByRole("button",{name:"Kết nối Google Calendar",exact:true}).click();
    await expect(page).toHaveURL(consent);
    const other = scenario === "switched" ? await context.newPage() : null;
    if (other) {
      await googleFixture(other);
      await other.route("**/api/v1/users/me", route => route.fulfill({json:{id:"4",role:"STUDENT",emailVerified:true}}));
      await openGoogle(other);
      await page.route("**/api/v1/users/me", route => route.fulfill({json:{id:"4",role:"STUDENT",emailVerified:true}}));
    }
    if (scenario === "refresh-failed" || scenario === "identity-failed")
      await page.route(scenario === "refresh-failed" ? "**/api/v1/auth/refresh" : "**/api/v1/users/me",
        route => route.fulfill({status:401,json:{code:"UNAUTHORIZED",message:"Unavailable"}}));
    await page.addInitScript(mode => {
      if (location.hostname !== "127.0.0.1") return;
      if (mode === "missing") sessionStorage.removeItem("google-calendar-return");
      if (mode === "expired") sessionStorage.setItem("google-calendar-return", JSON.stringify({accountId:"3",started:1}));
      if (mode === "malformed") sessionStorage.setItem("google-calendar-return","{");
      localStorage.setItem("remember-login","1");
    }, scenario);
    let logouts=0;
    page.on("request",request=>{if(new URL(request.url()).pathname.endsWith("/auth/logout"))logouts++;});
    await page.goto("/account?googleCalendar="+(scenario==="denied"?"CONSENT_DENIED":"CONNECTED"));
    if (scenario === "denied" || scenario === "replayed") {
      await expect(page.getByRole("region",{name:"Kết nối Google Calendar"})).toContainText(
        scenario==="denied"?"Bạn đã từ chối quyền truy cập Google.":"Đã kết nối Google Calendar.");
      if (scenario==="denied") return;
      await page.reload();
    }
    await expect(page.getByRole("main").getByRole("alert")).toContainText("Không thể xác minh phiên kết nối Google");
    await expect(page.locator("[data-dashboard-layout]")).toHaveCount(0);
    await expect(page.getByText("Đã kết nối Google Calendar.",{exact:true})).toHaveCount(0);
    expect(await page.evaluate(()=>sessionStorage.getItem("google-calendar-return"))).toBeNull();
    if (["missing","expired","malformed"].includes(scenario)) expect(state.refreshes).toBe(0);
    if (other) {
      await other.getByRole("button",{name:"Tải lại kết nối"}).click();
      await expect(other.getByText("Chưa kết nối",{exact:true})).toBeVisible();
      expect(await other.evaluate(()=>localStorage.getItem("remember-login"))).toBe("1");
    }
    expect(logouts).toBe(0);
  });
}
