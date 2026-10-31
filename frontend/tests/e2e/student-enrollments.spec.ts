import { expect, test, type Page } from "@playwright/test";
import type { Enrollment } from "../../src/features/enrollment/enrollment";
const row: Enrollment = { id: "9007199254740993", courseId: "42", courseTitle: "Khóa học thực", status: "PENDING", enrolledAt: null, seatReservedUntil: "2026-10-18T02:59:00Z", reservationExpired: true, cancelledAt: null, cancellationReason: null, createdAt: "2026-10-18T02:00:00Z", updatedAt: "2026-10-18T02:00:00Z" };
async function setup(page: Page, options: { role?: string; failure?: number; mutation?: string; empty?: boolean; paginated?: boolean; renewalGate?: Promise<void> } = {}) {
  await page.clock.setFixedTime(new Date("2026-10-18T03:00:00Z"));
  const calls: string[] = [];
  let current: Enrollment = { ...row };
  await page.route("**/api/v1/**", async route => {
    const request = route.request(), path = new URL(request.url()).pathname;
    calls.push(`${request.method()} ${path}`);
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "fixture" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "fixture", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role: options.role ?? "STUDENT", emailVerified: false } });
    if (path.endsWith("/profile")) return route.fulfill({ json: { fullName: "Học viên", email: "fixture@example.invalid" } });
    if (path.includes("/student/enrollments")) {
      expect(request.headers().authorization).toBe("Bearer fixture");
      if (options.failure) return route.fulfill({ status: options.failure, json: { code: "ACCESS_DENIED", message: "Denied" } });
      if (request.method() === "POST") {
        expect(request.postData()).toBeNull();
        if (options.mutation === "network") return route.abort();
        if (options.mutation) return route.fulfill({ status: 409, json: { code: options.mutation, message: "Conflict" } });
        if (options.renewalGate && path.endsWith("/renew")) await options.renewalGate;
        current = path.endsWith("/renew") ? { ...current, reservationExpired: false, seatReservedUntil: "2026-10-18T03:15:00Z" }
          : { ...current, status: "CANCELLED", cancelledAt: "2026-10-18T03:00:00Z" };
        return route.fulfill({ json: current });
      }
      if (path.endsWith("/9007199254740993")) return route.fulfill({ json: current });
      if (path !== "/api/v1/student/enrollments") return route.fulfill({ status: 404, json: { code: "ENROLLMENT_NOT_FOUND", message: "Not found" } });
      const index = Number(new URL(request.url()).searchParams.get("page") ?? "0");
      const items = options.paginated ? index === 0
        ? Array.from({ length: 20 }, (_, i) => ({ ...row, id: String(100 + i), courseTitle: `Khóa trang một ${i + 1}` }))
        : [{ ...row, id: "200", courseTitle: "Khóa trang hai" }] : [current];
      return route.fulfill({ json: { items: options.empty ? [] : items, page: index, size: 20, hasNext: !!options.paginated && index === 0 } });
    }
    return route.fulfill({ status: 404, json: { code: "NOT_FOUND", message: "Not found" } });
  });
  await page.goto("/account");
  await page.getByLabel("Email", { exact: true }).fill("fixture@example.invalid");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.locator(".header-account-menu summary")).toBeVisible();
  if (!options.role) {
    await expect(page).toHaveURL(/\/student\/dashboard$/);
    await expect(page.locator("#student-navigation")).toBeAttached();
    const toggle = page.locator('button[aria-controls="student-navigation"]');
    if (await toggle.isVisible() && await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
    await page.locator("#student-navigation").getByRole("link", { name: "Đăng ký của tôi", exact: true }).click();
    await expect(page).toHaveURL(/\/student\/enrollments$/);
  } else {
    await expect(page.locator("#dashboard-navigation")).toBeAttached();
  }
  return calls;
}
test("expired detail renews explicitly and preserves dashboard on desktop/mobile", async ({ page }) => {
  const calls = await setup(page);
  await expect(page.getByText("Giữ chỗ đã hết hạn.", { exact: true })).toBeVisible();
  expect(calls.filter(call => call.startsWith("POST /api/v1/student"))).toHaveLength(0);
  await page.getByRole("link", { name: "Xem đăng ký", exact: true }).click();
  await expect(page).toHaveURL(/enrollments\?id=9007199254740993$/);
  await page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true }).click();
  await expect(page.getByText("Đã cập nhật thời hạn giữ chỗ.", { exact: true })).toBeVisible();
  await expect(page.getByText("Còn 15 phút 0 giây", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true })).toHaveCount(0);
  await expect(page.locator("#dashboard-navigation")).toBeAttached();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
test("cancellation requires confirmation and never activates", async ({ page }) => {
  const calls = await setup(page);
  await page.getByRole("button", { name: "Hủy đăng ký", exact: true }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "Giữ đăng ký", exact: true }).click();
  expect(calls.filter(call => call.endsWith("/cancel"))).toHaveLength(0);
  await page.getByRole("button", { name: "Hủy đăng ký", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận hủy", exact: true }).click();
  await expect(page.getByText("Đã hủy", { exact: true })).toBeVisible();
  expect(calls.filter(call => call.endsWith("/cancel"))).toHaveLength(1);
  expect(calls.some(call => call.includes("/payments") || call.includes("/activate"))).toBe(false);
});
for (const mutation of ["COURSE_FULL", "network"]) test(`mutation ${mutation} requires explicit reload`, async ({ page }) => {
  const calls = await setup(page, { mutation });
  await page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true }).click();
  await expect(page.getByText(/Vui lòng tải lại đăng ký thành công/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true })).toHaveCount(0);
  expect(calls.filter(call => call.endsWith("/renew"))).toHaveLength(1);
  await page.getByRole("button", { name: "Tải lại đăng ký", exact: true }).click();
  await expect(page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true })).toBeVisible();
});
for (const failure of [401, 403, 404]) test(`private reads deny ${failure} without content disclosure`, async ({ page }) => {
  await setup(page, { failure });
  if (failure === 401) await expect(page.getByText("Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại.")).toBeVisible();
  else await expect(page.getByRole("main").getByRole("alert")).toBeVisible();
  await expect(page.getByText("Khóa học thực", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true })).toHaveCount(0);
});
test("empty enrollments are not sample courses", async ({ page }) => {
  await setup(page, { empty: true });
  await expect(page.getByText("Bạn chưa có đăng ký nào trên trang này.", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Hủy đăng ký", exact: true })).toHaveCount(0);
});

async function navigate(page: Page, path: string) {
  await page.evaluate(target => (window as unknown as { next: { router: { push: (p: string) => void } } }).next.router.push(target), path);
}
test("pagination returns correct next and previous page data", async ({ page }) => {
  await setup(page, { paginated: true });
  await expect(page.getByRole("heading", { name: "Khóa trang một 1", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Trang trước", exact: true })).toBeDisabled();
  const next = page.waitForRequest(request => new URL(request.url()).searchParams.get("page") === "1");
  await page.getByRole("button", { name: "Trang sau", exact: true }).click(); await next;
  await expect(page.getByRole("heading", { name: "Khóa trang hai", exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Khóa trang một 1", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Trang sau", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Trang trước", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Khóa trang một 1", exact: true })).toBeVisible();
});
test("Teacher cannot open Student enrollment management", async ({ page }) => {
  const calls = await setup(page, { role: "TEACHER" });
  await navigate(page, "/student/enrollments");
  await expect(page).toHaveURL(/\/student\/enrollments$/);
  await expect(page.getByRole("main").getByRole("alert")).toHaveText("Trang đăng ký chỉ dành cho học viên.");
  expect(calls.some(call => call.includes("/student/enrollments"))).toBe(false);
  await expect(page.getByRole("region", { name: "Quản lý đăng ký" })).toHaveCount(0);
});
test("another Student detail is rejected without retaining previous content", async ({ page }) => {
  const calls = await setup(page);
  await expect(page.getByRole("heading", { name: row.courseTitle, exact: true })).toBeVisible();
  await navigate(page, "/student/enrollments?id=999");
  await expect(page.getByRole("main").getByRole("alert")).toHaveText("Không tìm thấy đăng ký mà bạn được phép truy cập.");
  expect(calls).toContain("GET /api/v1/student/enrollments/999");
  await expect(page.getByText(row.courseTitle, { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true })).toHaveCount(0);
});
test("delayed renewal sends one mutation despite repeated activation", async ({ page }) => {
  let release!: () => void;
  const renewalGate = new Promise<void>(resolve => { release = resolve; });
  const calls = await setup(page, { renewalGate });
  const button = page.getByRole("button", { name: "Gia hạn giữ chỗ", exact: true });
  try {
    await button.evaluate(element => { (element as HTMLButtonElement).click(); (element as HTMLButtonElement).click(); });
    await expect.poll(() => calls.filter(call => call.endsWith("/renew")).length).toBe(1);
    await expect(button).toBeDisabled();
    await button.evaluate(element => (element as HTMLButtonElement).click());
    expect(calls.filter(call => call.endsWith("/renew"))).toHaveLength(1);
  } finally { release(); }
  await expect(page.getByText("Đã cập nhật thời hạn giữ chỗ.", { exact: true })).toBeVisible();
  expect(calls.filter(call => call.endsWith("/renew"))).toHaveLength(1);
});
test("anonymous direct entry requires login without protected API requests", async ({ page }) => {
  const calls: string[] = [];
  await page.route("**/api/v1/**", route => { calls.push(route.request().url()); return route.abort(); });
  await page.goto("/student/enrollments");
  await expect(page).toHaveURL(/\/login\?next=%2Fstudent%2Fenrollments$/);
  await expect(page.getByLabel("Email", { exact: true })).toBeVisible();
  expect(calls.some(call => call.includes("/student/enrollments"))).toBe(false);
});
test("refresh clears in-memory session and hides Enrollment content", async ({ page }) => {
  const calls = await setup(page);
  await expect(page.getByRole("heading", { name: row.courseTitle, exact: true })).toBeVisible();
  const before = calls.filter(call => call.includes("/student/enrollments")).length;
  await page.reload();
  await expect(page).toHaveURL(/\/login\?next=%2Fstudent%2Fenrollments$/);
  await expect(page.getByLabel("Email", { exact: true })).toBeVisible();
  await expect(page.getByText(row.courseTitle, { exact: true })).toHaveCount(0);
  expect(calls.filter(call => call.includes("/student/enrollments"))).toHaveLength(before);
});
test("cancellation dialog supports keyboard Escape and restores focus", async ({ page }) => {
  const calls = await setup(page);
  const trigger = page.getByRole("button", { name: "Hủy đăng ký", exact: true });
  await trigger.focus(); await page.keyboard.press("Enter");
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByRole("button", { name: "Giữ đăng ký", exact: true })).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(page.getByRole("button", { name: "Xác nhận hủy", exact: true })).toBeFocused();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await expect(trigger).toBeFocused();
  expect(calls.filter(call => call.endsWith("/cancel"))).toHaveLength(0);
});
