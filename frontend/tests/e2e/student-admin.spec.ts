import { expect, test, type Page } from "@playwright/test";

const applicationId = "9007199254740993";
const detail = { id: applicationId, teacherId: "9007199254740994", status: "PENDING", specialization: " English ", teachingExperience: " Experience ", introduction: "<script>private()</script>", submittedAt: "2026-10-13T00:00:00Z", reviewedBy: null, reviewedAt: null, rejectionReason: null };
async function backend(page: Page, role = "ADMIN", verified = true) {
  const calls: { path: string; method: string; body: unknown; authorization?: string; csrf?: string }[] = [];
  await page.route("**/api/v1/**", async route => {
    const request = route.request(); const url = new URL(request.url()); const path = url.pathname;
    if (request.method() === "OPTIONS") { await route.fulfill({ status: 204 }); return; }
    calls.push({ path: path + url.search, method: request.method(), body: request.postData() ? request.postDataJSON() : null, authorization: request.headers().authorization, csrf: request.headers()["x-xsrf-token"] });
    if (path.endsWith("/csrf")) await route.fulfill({ json: { token: "csrf-test" } });
    else if (path.endsWith("/login")) await route.fulfill({ json: { accessToken: "test-token", tokenType: "Bearer" } });
    else if (path.endsWith("/users/me")) await route.fulfill({ json: { id: "3", role, emailVerified: verified } });
    else if (path.endsWith("/register/student")) await route.fulfill({ status: 201, json: { verificationRequired: false } });
    else if (path.endsWith("/pending")) await route.fulfill({ json: { items: [ { id: applicationId, teacherId: detail.teacherId, status: "PENDING", submittedAt: detail.submittedAt } ], page: Number(url.searchParams.get("page")), size: 20, hasNext: false } });
    else if (path.endsWith(`/${applicationId}`)) await route.fulfill({ json: detail });
    else if (path.endsWith("/onboarding")) await route.fulfill({ json: { applicationId, status: "PENDING", submittedAt: detail.submittedAt, reviewedAt: null, rejectionReason: null } });
    else await route.fulfill({ status: 204 });
  });
  return calls;
}
async function login(page: Page) {
  await page.goto("/account"); await page.getByLabel("Email", { exact: true }).fill("account@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByRole("button", { name: "Đăng nhập" }).click();
}
async function openDetail(page: Page) {
  await login(page); await page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" }).click();
  await page.getByRole("button", { name: `Xem hồ sơ ${applicationId}` }).click();
}
async function studentForm(page: Page, password = " password ") {
  await page.goto("/account"); await page.goto("/register");
  await page.getByLabel("Tên người dùng", { exact: true }).fill("Student_01");
  await page.getByLabel("Số điện thoại", { exact: true }).fill("0912345678");
  await page.getByLabel("Email", { exact: true }).fill("student@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password); await page.getByLabel("Xác nhận mật khẩu", { exact: true }).fill(password); await page.getByLabel("Họ và tên").fill(" Student Name ");
}

test("Student registers with exactly five fields then immediately signs in", async ({ page }) => {
  const calls = await backend(page, "STUDENT", false); await studentForm(page);
  await expect(page.getByLabel("Chuyên môn")).toHaveCount(0);
  await page.getByRole("button", { name: "Tạo tài khoản học viên" }).evaluate((button: HTMLButtonElement) => { button.click(); button.click(); });
  await expect(page.getByRole("status")).toContainText("Đăng ký thành công");
  const requests = calls.filter(c => c.path.endsWith("/register/student")); expect(requests).toHaveLength(1);
  expect(requests[0].body).toEqual({ email: "student@example.test", password: " password ", fullName: " Student Name ", username: "Student_01", phone: "0912345678" });
  expect(requests[0].csrf).toBe("csrf-test"); expect(calls.some(c => c.path.endsWith("/login"))).toBe(false);
  await expect(page).toHaveURL(/\/login\?registered=1$/);
  expect(calls.some(c => c.path.includes("verification"))).toBe(false);
  await login(page);
  await expect(page.getByText("Đã đăng nhập với vai trò học viên.")).toBeVisible();
});

test("Student password and blank name validation prevent requests", async ({ page }) => {
  const calls = await backend(page); await studentForm(page, "short");
  await page.getByRole("button", { name: "Tạo tài khoản học viên" }).click();
  await expect(page.getByRole("status")).toContainText("Vui lòng kiểm tra thông tin đã nhập");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("valid password"); await page.getByLabel("Xác nhận mật khẩu", { exact: true }).fill("valid password"); await page.getByLabel("Họ và tên").fill("   ");
  await page.getByRole("button", { name: "Tạo tài khoản học viên" }).click();
  expect(calls.filter(c => c.path.endsWith("/register/student"))).toHaveLength(0);
});

for (const status of [400, 409, 500, 503]) test(`Student registration ${status} stays safe without automatic retry`, async ({ page }) => {
  await backend(page); let requests = 0;
  await page.route("**/register/student", async route => { requests++; await route.fulfill({ status, json: { code: status === 409 ? "REGISTRATION_CONFLICT" : "ERROR", message: "PRIVATE SQL" } }); });
  await studentForm(page); await page.getByRole("button", { name: "Tạo tài khoản học viên" }).click();
  await expect(page.getByRole("status")).not.toBeEmpty(); await expect(page.getByText("PRIVATE SQL")).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "Đăng ký học viên" })).toBeVisible(); expect(requests).toBe(1);
});

test("Admin sees safe detail and approves with an empty POST exactly once", async ({ page }) => {
  const calls = await backend(page); await openDetail(page);
  await expect(page.getByText(detail.introduction, { exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole("button", { name: "Phê duyệt hồ sơ" }).evaluate((button: HTMLButtonElement) => { button.click(); button.click(); });
  await expect(page.getByText("Đã phê duyệt hồ sơ.", { exact: true })).toBeVisible();
  const requests = calls.filter(c => c.path.endsWith("/approve")); expect(requests).toHaveLength(1);
  expect(requests[0]).toMatchObject({ method: "POST", body: null, authorization: "Bearer test-token" });
  await expect.poll(() => calls.filter(c => c.path.includes("/pending?")).length).toBe(2);
});

test("Admin rejects with exact preserved reason and rejects blank input", async ({ page }) => {
  const calls = await backend(page); await openDetail(page);
  await page.getByLabel("Lý do từ chối", { exact: true }).fill("   "); await page.getByRole("button", { name: "Từ chối hồ sơ" }).click();
  await expect(page.getByRole("status").first()).toContainText("Vui lòng kiểm tra thông tin đã nhập"); expect(calls.some(c => c.path.endsWith("/reject"))).toBe(false);
  await page.getByLabel("Lý do từ chối", { exact: true }).fill(" More experience needed. "); await page.getByRole("button", { name: "Từ chối hồ sơ" }).click();
  await expect(page.getByText("Đã từ chối hồ sơ.", { exact: true })).toBeVisible();
  expect(calls.find(c => c.path.endsWith("/reject"))?.body).toEqual({ reason: " More experience needed. " });
});

test("queue pagination and empty results use approved fixed-size query", async ({ page }) => {
  await backend(page);
  const pages: string[] = [];
  await page.route("**/pending?*", async route => { const url = new URL(route.request().url()); pages.push(url.search);
    const index = Number(url.searchParams.get("page"));
    await route.fulfill({ json: { items: [], page: index, size: 20, hasNext: index === 0 } });
  });
  await login(page); await page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" }).click();
  await expect(page.getByText("Không có hồ sơ chờ xét duyệt trên trang này.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Trang trước" })).toBeDisabled();
  await page.getByRole("button", { name: "Trang sau" }).click(); await expect(page.getByText("Trang 2", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Trang sau" })).toBeDisabled();
  await page.getByRole("button", { name: "Trang trước" }).click(); await expect(page.getByText("Trang 1", { exact: true })).toBeVisible();
  expect(pages).toEqual(["?page=0&size=20", "?page=1&size=20", "?page=0&size=20"]);
});

for (const [role, verified] of [["STUDENT", true], ["TEACHER", true], ["ADMIN", false]] as const)
  test(`${role} verified=${verified} cannot see review controls`, async ({ page }) => {
    const calls = await backend(page, role, verified); await login(page); await expect(page.getByText(`Đã đăng nhập với vai trò ${{ STUDENT: "học viên", TEACHER: "gia sư", ADMIN: "quản trị viên" }[role]}.`)).toBeVisible();
    await expect(page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" })).toHaveCount(0);
    expect(calls.some(c => c.path.includes("/admin/"))).toBe(false);
  });

test("concurrent review conflict refreshes immutable detail without replay", async ({ page }) => {
  await backend(page); await openDetail(page); let reviews = 0;
  await page.route("**/approve", async route => { reviews++;
    await page.route(`**/teacher-applications/${applicationId}`, lookup => lookup.fulfill({ json: { ...detail, status: "APPROVED", reviewedBy: "4", reviewedAt: detail.submittedAt } }));
    await route.fulfill({ status: 409, json: { code: "APPLICATION_ALREADY_REVIEWED", message: "PRIVATE" } });
  });
  await page.getByRole("button", { name: "Phê duyệt hồ sơ" }).click();
  await expect(page.getByRole("status").first()).toContainText("Hồ sơ này đã được xét duyệt");
  await expect(page.getByRole("region", { name: "Chi tiết hồ sơ" }).getByText("Đã phê duyệt", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Phê duyệt hồ sơ" })).toHaveCount(0); expect(reviews).toBe(1);
});

test("uncertain review with failed reconciliation removes mutation controls", async ({ page }) => {
  await backend(page); await openDetail(page); let requests = 0;
  await page.route("**/approve", async route => { requests++;
    await page.route(`**/teacher-applications/${applicationId}`, lookup => lookup.fulfill({ status: 503, json: { code: "AUTHENTICATION_UNAVAILABLE", message: "PRIVATE" } }));
    await route.abort();
  });
  await page.getByRole("button", { name: "Phê duyệt hồ sơ" }).click();
  await expect(page.getByRole("status").first()).toContainText("tạm thời không khả dụng");
  await expect(page.getByRole("button", { name: "Phê duyệt hồ sơ" })).toHaveCount(0); expect(requests).toBe(1);
});

for (const status of [401, 403, 404, 500, 503]) test(`Admin detail ${status} removes stale review controls`, async ({ page }) => {
  await backend(page); await login(page); await page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" }).click();
  await page.route(`**/teacher-applications/${applicationId}`, route => route.fulfill({ status, json: { code: "ERROR", message: "PRIVATE" } }));
  await page.getByRole("button", { name: `Xem hồ sơ ${applicationId}` }).click();
  await expect(page.getByRole("status").first()).not.toBeEmpty(); await expect(page.getByText("PRIVATE")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Phê duyệt hồ sơ" })).toHaveCount(0);
});


test("Student username and phone validation reject unsupported formats", async ({ page }) => {
  const calls = await backend(page); await studentForm(page);
  for (const [label, value] of [["Tên người dùng", "1invalid"], ["Số điện thoại", "0212345678"]]) {
    await page.getByLabel(label, { exact: true }).fill(value);
    await page.getByRole("button", { name: "Tạo tài khoản học viên" }).click();
    await expect(page.getByRole("status")).toContainText("không hợp lệ");
    expect(calls.filter(c => c.path.endsWith("/register/student"))).toHaveLength(0);
    await page.getByLabel("Tên người dùng", { exact: true }).fill("Student_01");
  }
});
