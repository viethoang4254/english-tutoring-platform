import { expect, test, type Page } from "@playwright/test";
import { openTeacher, teacherPanel } from "../support/teacher-navigation";

async function login(page: Page, role = "TEACHER", status = "APPROVED") {
  const calls: string[] = [];
  await page.route("**/api/v1/**", route => {
    const path = new URL(route.request().url()).pathname;
    calls.push(path);
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "fixture" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "fixture", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role, emailVerified: true } });
    if (path.endsWith("/onboarding")) return route.fulfill({ json: { applicationId: "1", status, submittedAt: "2026-01-01T00:00:00Z" } });
    if (path.endsWith("/profile")) return route.fulfill({ json: { id: "1", fullName: "Gia sư thử nghiệm", username: null, phone: null, email: "teacher@example.invalid", emailVerified: true } });
    return route.fulfill({ status: 404, json: { code: "COURSE_NOT_FOUND" } });
  });
  await page.goto("/account");
  await page.getByLabel("Email", { exact: true }).fill("fixture@example.invalid");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.locator(".header-account-menu summary")).toBeVisible();
  return calls;
}

test("Teacher routes reuse profile settings and accessible navigation", async ({ page }, info) => {
  await login(page); await openTeacher(page);
  await expect(page).toHaveURL(/\/teacher$/);
  await expect(page.getByRole("heading", { name: "Tổng quan", exact: true })).toBeVisible();
  await teacherPanel(page, "Hồ sơ gia sư");
  await expect(page.getByText("Đã phê duyệt", { exact: true })).toBeVisible();
  await teacherPanel(page, "Cài đặt tài khoản");
  await expect(page.getByRole("button", { name: "Chỉnh sửa thông tin" })).toBeVisible();
  await page.locator("#change-password summary").click();
  await expect(page.getByLabel("Mật khẩu hiện tại", { exact: true })).toBeVisible();
  await teacherPanel(page, "Tổng quan");
  const toggle = page.locator('button[aria-controls="teacher-navigation"]');
  if (await toggle.isVisible()) { await toggle.focus(); await expect(toggle).toBeFocused(); await page.keyboard.press("Enter"); await expect(toggle).toHaveAttribute("aria-expanded", "true"); }
  const courses = page.getByRole("navigation", { name: "Điều hướng gia sư", exact: true }).getByRole("link", { name: "Khóa học của tôi" });
  await courses.focus(); await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/\/teacher\/courses$/);
  await expect(page.getByRole("heading", { name: "Khóa học của tôi" }).last()).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath("teacher-dashboard.png"), fullPage: true });
});

for (const role of ["STUDENT", "ADMIN"]) test(`${role} cannot enter Teacher routes`, async ({ page }) => {
  const calls = await login(page, role);
  await page.evaluate(() => (window as unknown as { next: { router: { push: (p: string) => void } } }).next.router.push("/teacher/courses/42"));
  await expect(page.getByRole("heading", { name: "Không có quyền truy cập" })).toBeVisible();
  expect(calls.some(path => path.includes("/teacher/courses"))).toBe(false);
});

for (const status of ["PENDING", "REJECTED"]) test(`${status} Teacher retains onboarding without management access`, async ({ page }) => {
  const calls = await login(page, "TEACHER", status); await openTeacher(page);
  await expect(page.getByRole("heading", { name: "Chưa thể mở không gian gia sư" })).toBeVisible();
  expect(calls.some(path => path.includes("/teacher/courses"))).toBe(false);
  await page.getByRole("link", { name: "Xem hồ sơ đăng ký và tài khoản" }).click();
  await expect(page).toHaveURL(/\/account$/);
});

test("anonymous deep link preserves destination and makes no protected request", async ({ page }) => {
  const calls: string[] = [];
  await page.route("**/api/v1/**", r => { calls.push(r.request().url()); return r.abort(); });
  await page.goto("/teacher/courses/42");
  await expect(page).toHaveURL(/\/login\?next=%2Fteacher%2Fcourses%2F42$/);
  expect(calls).toHaveLength(0);
});

test("owned detail deep link handles missing Course and full refresh safely", async ({ page }) => {
  await login(page); await openTeacher(page);
  await expect(page.getByRole("heading", { name: "Tổng quan", exact: true })).toBeVisible();
  await page.evaluate(() => (window as unknown as { next: { router: { push: (p: string) => void } } }).next.router.push("/teacher/courses/42"));
  await expect(page).toHaveURL(/\/teacher\/courses\/42$/);
  await expect(page.getByRole("region", { name: "Khóa học của tôi", exact: true }).getByRole("alert")).toBeVisible();
  await expect(page.getByRole("button", { name: "Chỉnh sửa bản nháp", exact: true })).toHaveCount(0);
  await page.reload();
  await expect(page).toHaveURL(/\/login\?next=%2Fteacher%2Fcourses%2F42$/);
});

test("direct owned Course URL loads the requested string ID", async ({ page }) => {
  await login(page); await openTeacher(page);
  await expect(page.getByRole("heading", { name: "Tổng quan", exact: true })).toBeVisible();
  const id = "9007199254740993";
  await page.route(`**/api/v1/teacher/courses/${id}`, r => {
    expect(r.request().headers().authorization).toBe("Bearer fixture");
    return r.fulfill({ json: { id, name: "Direct owned course", status: "DRAFT", category: { id: "1", name: "Category", active: true }, description: "Text", tuition: { amount: "0.00", currency: "VND" }, minStudents: 1, maxStudents: 2, sessionCount: 1, plannedStartDate: "2030-01-01", timezone: "Asia/Ho_Chi_Minh" } });
  });
  await page.evaluate(id => (window as unknown as { next: { router: { push: (p: string) => void } } }).next.router.push(`/teacher/courses/${id}`), id);
  await expect(page.getByRole("heading", { name: "Direct owned course", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Quản lý lịch & buổi học" })).toBeVisible();
});

for (const status of [401, 403]) test(`current account ${status} denies stale Teacher session`, async ({ page }) => {
  const calls = await login(page);
  await page.route("**/api/v1/users/me", r => r.fulfill({ status, json: { code: "ACCESS_DENIED" } }));
  await openTeacher(page);
  if (status === 401) await expect(page).toHaveURL(/\/login\?next=%2Fteacher$/);
  else await expect(page.getByRole("heading", { name: "Chưa thể mở không gian gia sư" })).toBeVisible();
  expect(calls.some(path => path.includes("/teacher/courses"))).toBe(false);
});
