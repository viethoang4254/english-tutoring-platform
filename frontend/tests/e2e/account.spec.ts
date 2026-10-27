import { accountPanel } from "../support/account-navigation";
import { expect, test, type Page } from "@playwright/test";

const initial = { applicationId: "9007199254740993", status: "REJECTED", submittedAt: "2026-10-13T00:00:00Z", reviewedAt: "2026-10-13T01:00:00Z", rejectionReason: "Please describe your experience." };
async function backend(page: Page, state = "REJECTED", role = "TEACHER") {
  const calls: { path: string; body: unknown; authorization?: string; csrf?: string }[] = [];
  let status = { ...initial, status: state };
  await page.route("**/api/v1/**", async route => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    if (request.method() === "OPTIONS") { await route.fulfill({ status: 204, headers: { "access-control-allow-origin": "http://127.0.0.1:3000", "access-control-allow-credentials": "true", "access-control-allow-headers": "content-type,authorization,x-xsrf-token" } }); return; }
    calls.push({ path, body: request.postData() ? request.postDataJSON() : null, authorization: request.headers().authorization, csrf: request.headers()["x-xsrf-token"] });
    let body: unknown;
    if (path.endsWith("/csrf")) body = { token: "csrf-test" };
    else if (path.endsWith("/login") || path.endsWith("/refresh")) body = { accessToken: "memory-only-test-token", tokenType: "Bearer", issuedAt: initial.submittedAt, expiresAt: "2026-10-13T00:15:00Z" };
    else if (path.endsWith("/users/me")) body = { id: "42", role, emailVerified: true };
    else if (path.endsWith("/onboarding")) body = status;
    else if (path.endsWith("/resubmit")) { status = { ...initial, applicationId: "9007199254740994", status: "PENDING" }; body = status; }
    else if (path.endsWith("/register/teacher")) body = { verificationRequired: true };
    else { await route.fulfill({ status: path.endsWith("/resend") ? 202 : 204 }); return; }
    await route.fulfill({ status: path.endsWith("/resubmit") || path.endsWith("/register/teacher") ? 201 : 200, json: body });
  });
  return calls;
}
async function login(page: Page, navigate = true) {
  await page.goto("/account");
  await page.getByLabel("Email", { exact: true }).fill("teacher@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  if (navigate) await accountPanel(page,"Hồ sơ gia sư");
}
async function application(page: Page) {
  await page.getByLabel("Chuyên môn").fill(" English ");
  await page.getByLabel("Kinh nghiệm giảng dạy").fill(" Experience ");
  await page.getByLabel("Giới thiệu").fill(" Introduction ");
}

for (const state of ["PENDING", "APPROVED", "REJECTED"]) test(`Teacher sees ${state} and responsive layout`, async ({ page }) => {
  const calls = await backend(page, state); await login(page);
  await expect(page.getByText(({ PENDING: "Chờ xét duyệt", APPROVED: "Đã phê duyệt", REJECTED: "Bị từ chối" } as Record<string, string>)[state], { exact: true })).toBeVisible();
  if (state === "REJECTED") await expect(page.getByText(/Please describe your experience/)).toBeVisible();
  else await expect(page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true })).toHaveCount(0);
  expect(calls.find(c => c.path.endsWith("/login"))?.csrf).toBe("csrf-test");
  expect(calls.find(c => c.path.endsWith("/login"))?.body).toEqual({ email: "teacher@example.test", password: " password " });
  expect(calls.find(c => c.path.endsWith("/onboarding"))?.authorization).toBe("Bearer memory-only-test-token");
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("resubmits exact fields and preserves string IDs without duplicate clicks", async ({ page }) => {
  const calls = await backend(page); await login(page); await application(page);
  await page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true }).evaluate((button: HTMLButtonElement) => { button.click(); button.click(); });
  await expect(page.getByText("Chờ xét duyệt", { exact: true })).toBeVisible();
  const submitted = calls.filter(c => c.path.endsWith("/resubmit"));
  expect(submitted).toHaveLength(1);
  expect(submitted[0].path).toContain("9007199254740993/resubmit");
  expect(submitted[0].body).toEqual({ specialization: " English ", teachingExperience: " Experience ", introduction: " Introduction " });
});

test("uncertain mutation reconciles and failed lookup keeps resubmission disabled", async ({ page }) => {
  const calls = await backend(page); await login(page); await application(page);
  const initialReads = calls.filter(c => c.path.endsWith("/onboarding")).length;
  await page.route("**/resubmit", route => route.abort());
  await page.route("**/teacher/onboarding", route => route.fulfill({ status: 503, json: { code: "AUTHENTICATION_UNAVAILABLE", message: "PRIVATE" } }));
  await page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Chưa xác nhận được kết quả");
  await expect(page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true })).toBeDisabled();
  await expect(page.getByText("PRIVATE")).toHaveCount(0);
  await page.unroute("**/teacher/onboarding");
  await page.getByRole("button", { name: "Cập nhật trạng thái hồ sơ" }).click();
  await expect(page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true })).toBeEnabled();
  expect(calls.filter(c => c.path.endsWith("/onboarding")).length).toBe(initialReads + 1);
});

test("reload does not silently recover and logout clears the local session", async ({ page }) => {
  const calls = await backend(page, "PENDING"); await login(page);
  await expect(page.getByText("Chờ xét duyệt", { exact: true })).toBeVisible(); await page.reload();
  expect(calls.filter(c => c.path.endsWith("/refresh"))).toHaveLength(0);
  await expect(page.getByRole("button", { name: "Tiếp tục đăng nhập" })).toHaveCount(0);
  await login(page);
  await expect(page.getByText("Chờ xét duyệt", { exact: true })).toBeVisible();
  const toggle = page.locator('button[aria-controls="dashboard-navigation"]'); if(await toggle.isVisible())await toggle.click();
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page.getByRole("button", { name: "Đăng nhập", exact: true })).toBeVisible();
  await expect(page.getByRole("status")).toContainText("kết thúc phiên đăng nhập hiện tại");
  expect(calls.find(c => c.path.endsWith("/logout"))?.csrf).toBe("csrf-test");
});

test("failed logout clears local data but does not claim server success", async ({ page }) => {
  await backend(page, "PENDING"); await login(page);
  await page.route("**/auth/logout", route => route.fulfill({ status: 503, json: { code: "AUTHENTICATION_UNAVAILABLE", message: "PRIVATE" } }));
  const toggle = page.locator('button[aria-controls="dashboard-navigation"]'); if(await toggle.isVisible())await toggle.click();
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("chưa xác nhận được việc đăng xuất trên máy chủ");
  await expect(page.getByRole("button", { name: "Đăng nhập", exact: true })).toBeVisible();
  await expect(page.getByText("Chờ xét duyệt", { exact: true })).toHaveCount(0);
});

test("register, verify, resend and then sign in use approved contracts", async ({ page }) => {
  const calls = await backend(page, "PENDING"); await page.goto("/account");
  await expect(page.getByRole("button", { name: "Đăng ký gia sư", exact: true })).toHaveCount(0);
  await page.goto("/");
  await page.getByRole("link", { name: "Đăng ký giảng viên" }).first().click();
  await page.getByLabel("Email", { exact: true }).fill("teacher@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByLabel("Họ và tên").fill(" Teacher "); await application(page);
  await page.getByRole("button", { name: "Tạo tài khoản gia sư" }).click();
  await expect(page.getByRole("status")).toContainText("Bạn chưa đăng nhập");
  expect(calls.find(c => c.path.endsWith("/register/teacher"))?.body).toEqual({ email: "teacher@example.test", password: " password ", fullName: " Teacher ", specialization: " English ", teachingExperience: " Experience ", introduction: " Introduction " });
  expect(calls.filter(c => c.path.endsWith("/login"))).toHaveLength(0);
  await page.getByLabel("Địa chỉ email nhận lại mã").fill("teacher@example.test");
  await page.getByRole("button", { name: "Gửi lại email xác minh" }).click();
  await expect(page.getByRole("status")).toContainText("Nếu đủ điều kiện");
  await page.getByLabel("Mã xác minh").fill("test-code");
  await page.getByRole("button", { name: "Xác minh mã" }).click();
  await expect(page.getByRole("status")).toContainText("Email đã được xác minh");
  expect(calls.find(c => c.path.endsWith("/email-verification"))?.body).toEqual({ token: "test-code" });
});

test("blank application input is rejected without a request", async ({ page }) => {
  const calls = await backend(page); await login(page); await application(page);
  await page.getByLabel("Chuyên môn").fill("   ");
  await page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Vui lòng kiểm tra thông tin đã nhập");
  expect(calls.filter(c => c.path.endsWith("/resubmit"))).toHaveLength(0);
});

for (const status of [400, 401, 403, 404, 409, 500, 503]) test(`safe onboarding ${status} response`, async ({ page }) => {
  await backend(page);
  await page.route("**/teacher/onboarding", route => route.fulfill({ status, json: { code: "TEST_ERROR", message: "PRIVATE DATABASE VALUE" } }));
  await login(page, status !== 401); await expect(page.getByRole("status")).not.toBeEmpty();
  await expect(page.getByText("PRIVATE DATABASE VALUE")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true })).toHaveCount(0);
});

test("Student cannot enter Teacher onboarding", async ({ page }) => {
  const calls = await backend(page, "PENDING", "STUDENT"); await login(page, false);
  await expect(page.getByText("Chỉ tài khoản gia sư đã xác minh email mới có thể truy cập hồ sơ đăng ký gia sư.")).toHaveCount(0);
  expect(calls.filter(c => c.path.endsWith("/onboarding"))).toHaveLength(0);
});

test("email expiry and resend limits produce safe actionable messages", async ({ page }) => {
  await backend(page); await page.goto("/account"); await page.goto("/verify-email");
  await page.route("**/auth/email-verification", route => route.fulfill({ status: 400, json: { code: "INVALID_VERIFICATION_CREDENTIAL", message: "PRIVATE" } }));
  await page.getByLabel("Mã xác minh").fill("expired"); await page.getByRole("button", { name: "Xác minh mã" }).click();
  await expect(page.getByRole("status")).toContainText("không hợp lệ, đã hết hạn hoặc đã được sử dụng");
  await page.route("**/resend", route => route.fulfill({ status: 429, json: { code: "VERIFICATION_RATE_LIMITED", message: "PRIVATE" } }));
  await page.getByLabel("Địa chỉ email nhận lại mã").fill("teacher@example.test"); await page.getByRole("button", { name: "Gửi lại email xác minh" }).click();
  await expect(page.getByRole("status")).toContainText("tối đa 5 lần gửi lại");
});

test("uncertain submission reconciles a committed PENDING result without replay", async ({ page }) => {
  await backend(page); await login(page); await application(page);
  let submissions = 0;
  await page.route("**/resubmit", async route => {
    submissions++;
    await page.route("**/teacher/onboarding", lookup => lookup.fulfill({ json: { ...initial, status: "PENDING", applicationId: "9007199254740994" } }));
    await route.abort();
  });
  await page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true }).click();
  await expect(page.getByText("Chờ xét duyệt", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Gửi lại hồ sơ", exact: true })).toHaveCount(0);
  expect(submissions).toBe(1);
});

test("CSRF bootstrap failure cannot perform login", async ({ page }) => {
  const calls = await backend(page);
  await page.route("**/auth/csrf", route => route.fulfill({ status: 403, json: { code: "ACCESS_DENIED", message: "PRIVATE" } }));
  await login(page, false); await expect(page.getByRole("status")).toContainText("không được phép");
  expect(calls.filter(c => c.path.endsWith("/login"))).toHaveLength(0);
});

test("unverified login and SMTP delivery failure retain recovery guidance", async ({ page }) => {
  const calls = await backend(page);
  await page.route("**/auth/login", route => route.fulfill({ status: 403, json: { code: "EMAIL_VERIFICATION_REQUIRED", message: "PRIVATE" } }));
  await login(page, false); await expect(page.getByRole("status")).toContainText("Vui lòng xác minh email");
  expect(calls.filter(c => c.path.endsWith("/onboarding"))).toHaveLength(0);
  await page.goto("/verify-email");
  await page.route("**/resend", route => route.fulfill({ status: 503, json: { code: "VERIFICATION_DELIVERY_UNAVAILABLE", message: "PRIVATE" } }));
  await page.getByLabel("Địa chỉ email nhận lại mã").fill("teacher@example.test"); await page.getByRole("button", { name: "Gửi lại email xác minh" }).click();
  await expect(page.getByRole("status")).toContainText("Tài khoản của bạn vẫn được giữ lại");
});


test("Sign In omits recovery controls and never silently recovers", async ({ page }) => {
  const calls = await backend(page);
  await page.goto("/account");
  await expect(page.getByText("Đã có phiên đăng nhập?", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Tiếp tục đăng nhập" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Xác minh email để đăng nhập" })).toHaveCount(0);
  expect(calls).toHaveLength(0);
});

test("native required and email validation use Vietnamese and clear after input", async ({ page }) => {
  const calls = await backend(page);
  await page.goto("/account");
  const email = page.getByLabel("Email", { exact: true });
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  await expect(email).toHaveJSProperty("validationMessage", "Vui lòng điền thông tin này.");
  await email.fill("invalid");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  await expect(email).toHaveJSProperty("validationMessage", "Vui lòng nhập địa chỉ email hợp lệ.");
  expect(calls).toHaveLength(0);
  await email.fill("teacher@example.test");
  await expect(email).toHaveJSProperty("validationMessage", "");
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  await accountPanel(page,"Hồ sơ gia sư");
  await expect(page.getByText("Bị từ chối", { exact: true })).toBeVisible();
  await expect(page.locator("time")).toHaveAttribute("datetime", initial.submittedAt);
  await expect(page.locator("time")).toContainText("13/10/2026");
});
